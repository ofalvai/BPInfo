/*
 * Copyright 2018 Olivér Falvai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ofalvai.bpinfo.api.bkkinfo

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import androidx.annotation.ColorInt
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.api.AlertApiClient
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.alertlist.AlertListType
import com.ofalvai.bpinfo.util.LocaleManager
import com.ofalvai.bpinfo.util.apiTimestampToDateTime
import com.ofalvai.bpinfo.util.toArray
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.*

class BkkInfoClient(
    private val requestQueue: RequestQueue,
    private val sharedPreferences: SharedPreferences,
    private val context: Context
) : AlertApiClient {

    companion object {

        /**
         * The API is so slow we have to increase the default Volley timeout.
         * Response time increases the most when there's an alert with many affected routes.
         */
        private const val TIMEOUT_MS = 5000

        private const val API_BASE_URL = "https://bkk.hu/apps/bkkinfo/"

        private const val API_ENDPOINT_HU = "json.php"

        private const val API_ENDPOINT_EN = "json_en.php"

        private const val PARAM_ALERT_LIST = "?lista"

        private const val PARAM_ALERT_DETAIL = "id"

        private const val DETAIL_WEBVIEW_BASE_URL = "http://m.bkkinfo.hu/alert.php"

        private const val DETAIL_WEBVIEW_PARAM_ID = "id"

        /**
         * Returns a retry policy with increased timeout
         */
        @JvmStatic
        private val retryPolicy = DefaultRetryPolicy(
            TIMEOUT_MS,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
    }

    private var alertDetailTrace: Trace? = null

    private val languageCode: String =
        LocaleManager.getCurrentLanguageCode(sharedPreferences)

    override fun fetchAlertList(callback: AlertApiClient.AlertListCallback) {
        val url = buildAlertListUrl()

        Timber.i("API request: %s", url.toString())

        val request = JsonObjectRequest(
            url.toString(),
            null,
            { response -> onAlertListResponse(response, callback) },
            { error ->
                callback.onError(error)
            }
        )
        request.retryPolicy = retryPolicy

        requestQueue.add(request)
    }

    override fun fetchAlert(id: String, alertListType: AlertListType,
                            callback: AlertApiClient.AlertDetailCallback) {
        val url = buildAlertDetailUrl(id)

        Timber.i("API request: %s", url.toString())

        val request = JsonObjectRequest(
            url.toString(), null,
            { response -> onAlertDetailResponse(callback, response) },
            { error -> callback.onError(error) }
        )
        request.retryPolicy = retryPolicy

        requestQueue.add(request)
        createAndStartTrace("network_alert_detail_bkk")
    }

    private fun buildAlertListUrl() = Uri.parse(API_BASE_URL)
            .buildUpon()
            .appendEncodedPath(if (languageCode == "hu") API_ENDPOINT_HU else API_ENDPOINT_EN)
            .appendEncodedPath(PARAM_ALERT_LIST)
            .build()

    private fun buildAlertDetailUrl(alertId: String) =
        Uri.parse(API_BASE_URL)
            .buildUpon()
            .appendEncodedPath(if (languageCode == "hu") API_ENDPOINT_HU else API_ENDPOINT_EN)
            .appendQueryParameter(PARAM_ALERT_DETAIL, alertId)
            .build()

    private fun onAlertListResponse(response: JSONObject, callback: AlertApiClient.AlertListCallback) {
        try {
            val alertsToday = parseTodayAlerts(response).toMutableList()
            val alertsFuture = parseFutureAlerts(response)
            fixFutureAlertsInTodayList(alertsToday, alertsFuture)

            callback.onAlertListResponse(alertsToday, alertsFuture)
        } catch (ex: Exception) {
            callback.onError(ex)
        }
    }

    private fun onAlertDetailResponse(
            callback: AlertApiClient.AlertDetailCallback,
            response: JSONObject
    ) {
        alertDetailTrace?.stop()
        try {
            val alert = parseAlertDetail(response)
            callback.onAlertResponse(alert)
        } catch (ex: Exception) {
            callback.onError(ex)
        }
    }

    @Throws(JSONException::class)
    private fun parseTodayAlerts(response: JSONObject): List<Alert> {
        val alerts = ArrayList<Alert>()

        val isDebugMode = sharedPreferences.getBoolean(
            context.getString(R.string.pref_key_debug_mode), false
        )

        val activeAlertsList = response.getJSONArray("active")
        for (i in 0 until activeAlertsList.length()) {
            try {
                val alertNode = activeAlertsList.getJSONObject(i)
                val alert = parseAlert(alertNode)

                // Some alerts are still listed a few minutes after they ended, we need to hide them,
                // but still show them if debug mode is enabled
                val alertEndTime: ZonedDateTime = apiTimestampToDateTime(alert.end)
                if (alertEndTime.isAfter(ZonedDateTime.now()) || alert.end == 0L || isDebugMode) {
                    alerts.add(alert)
                }
            } catch (ex: JSONException) {
                FirebaseCrashlytics.getInstance().log("Alert parse: failed to parse:\n$ex")
            }
        }

        return alerts
    }

    @Throws(JSONException::class)
    private fun parseFutureAlerts(response: JSONObject): MutableList<Alert> {
        val alerts = ArrayList<Alert>()

        // Future alerts are in two groups: near-future and far-future
        val soonAlertList = response.getJSONArray("soon")
        for (i in 0 until soonAlertList.length()) {
            try {
                val alertNode = soonAlertList.getJSONObject(i)
                alerts.add(parseAlert(alertNode))
            } catch (ex: JSONException) {
                FirebaseCrashlytics.getInstance().log("Alert parse: failed to parse:\n$ex")
            }
        }

        val futureAlertList = response.getJSONArray("future")
        for (i in 0 until futureAlertList.length()) {
            try {
                val alertNode = futureAlertList.getJSONObject(i)
                alerts.add(parseAlert(alertNode))
            } catch (ex: JSONException) {
                FirebaseCrashlytics.getInstance().log("Alert parse: failed to parse:\n$ex")
            }
        }

        return alerts
    }

    /**
     * Parses alert details found in the alert list API response
     * This structure is different than the alert detail API response
     */
    @Throws(JSONException::class)
    private fun parseAlert(alertNode: JSONObject): Alert {
        val id = alertNode.getString("id")

        var start: Long = 0
        if (!alertNode.isNull("kezd")) {
            val beginNode = alertNode.getJSONObject("kezd")
            start = beginNode.getLong("epoch")
        }

        var end: Long = 0
        if (!alertNode.isNull("vege")) {
            val endNode = alertNode.getJSONObject("vege")
            end = endNode.getLong("epoch")
        }

        val timestamp: Long
        val modifiedNode = alertNode.getJSONObject("modositva")
        timestamp = modifiedNode.getLong("epoch")

        val url = getUrl(id)

        val header = alertNode.getString("elnevezes").capitalize(Locale.getDefault())

        val routesArray = alertNode.getJSONArray("jaratokByFajta")
        val affectedRoutes = parseAffectedRoutes(routesArray)

        return Alert(id, start, end, timestamp, url, header, null, affectedRoutes, true)
    }

    /**
     * Parses alert details found in the alert detail API response
     * This structure is different than the alert list API response
     */
    @Throws(JSONException::class)
    private fun parseAlertDetail(response: JSONObject): Alert {
        val id = response.getString("id")

        var start: Long = 0
        if (!response.isNull("kezdEpoch")) {
            start = response.getLong("kezdEpoch")
        }

        var end: Long = 0
        if (!response.isNull("vegeEpoch")) {
            end = response.getLong("vegeEpoch")
        }

        var timestamp: Long = 0
        if (!response.isNull("modEpoch")) {
            timestamp = response.getLong("modEpoch")
        }

        val url = getUrl(id)

        val header: String
        // The API returns a header of 3 parts separated by "|" characters. We need the last part.
        val rawHeader = response.getString("targy")
        header = rawHeader.split("|")[2].trim().capitalize(Locale.getDefault())

        val description: String
        val descriptionBuilder = StringBuilder()
        descriptionBuilder.append(response.getString("feed"))

        val routesArray = response.getJSONObject("jaratok").toArray()
        for (i in 0 until routesArray.length()) {
            val routeNode = routesArray.getJSONObject(i)
            val optionsNode = routeNode.getJSONObject("opciok")

            if (!optionsNode.isNull("szabad_szoveg")) {
                val routeTextArray = optionsNode.getJSONArray("szabad_szoveg")
                for (j in 0 until routeTextArray.length()) {
                    descriptionBuilder.append("<br />")
                    descriptionBuilder.append(routeTextArray.getString(j))
                }
            }
        }
        description = descriptionBuilder.toString()

        val affectedRoutes: List<Route>
        val routeDetailsNode = response.getJSONObject("jarat_adatok")
        val affectedRouteIds = response.getJSONObject("jaratok").keys()
        // Some routes in routeDetailsNode are not affected by the alert, but alternative
        // recommended routes. The real affected routes' IDs are in "jaratok"
        affectedRoutes = parseDetailedAffectedRoutes(routeDetailsNode, affectedRouteIds)

        return Alert(id, start, end, timestamp, url, header, description, affectedRoutes, false)
    }

    private fun getUrl(alertId: String): String {
        return "$DETAIL_WEBVIEW_BASE_URL?$DETAIL_WEBVIEW_PARAM_ID=$alertId"
    }

    /**
     * Parses affected routes found in the alert list API response
     * This structure is different than the alert detail API response
     */
    @Throws(JSONException::class)
    private fun parseAffectedRoutes(routesArray: JSONArray): List<Route> {
        // The API lists multiple affected routes grouped by their vehicle type (bus, tram, etc.)
        val routes = ArrayList<Route>()
        for (i in 0 until routesArray.length()) {
            val routeNode = routesArray.getJSONObject(i)
            val typeString = routeNode.getString("type")
            val type = parseRouteType(typeString)

            val concreteRoutes = routeNode.getJSONArray("jaratok")
            for (j in 0 until concreteRoutes.length()) {
                val shortName = concreteRoutes.getString(j).trim()
                val colors = parseRouteColors(type, shortName)

                // There's no ID returned by the API, using shortName instead
                val route = Route(
                    shortName,
                    shortName, null, null,
                    type,
                    colors[0],
                    colors[1],
                    false
                )
                routes.add(route)
            }
        }
        return routes
    }

    /**
     * Parses affected routes found in the alert detail API response
     * This structure is different than the alert list API response
     * @param routesNode Details of routes. Some of them are not affected by the alert, only
     * recommended alternative routes
     * @param affectedRouteIds IDs of only the affected routes
     */
    @Throws(JSONException::class)
    private fun parseDetailedAffectedRoutes(
        routesNode: JSONObject,
        affectedRouteIds: Iterator<String>
    ): List<Route> {
        val routes = ArrayList<Route>()

        while (affectedRouteIds.hasNext()) {
            val routeId = affectedRouteIds.next()
            val routeNode = routesNode.getJSONObject(routeId)

            val id = routeNode.getString("forte")
            val shortName = routeNode.getString("szam")
            val description = routeNode.getString("utvonal")
            val routeType = parseRouteType(routeNode.getString("tipus"))
            val color = Color.parseColor("#" + routeNode.getString("szin"))
            val textColor = Color.parseColor("#" + routeNode.getString("betu"))

            val route = Route(
                id,
                shortName,
                null,
                description,
                routeType,
                color,
                textColor,
                false
            )
            routes.add(route)
        }

        routes.sort()

        return routes
    }

    private fun parseRouteType(routeTypeString: String): RouteType {
        return when (routeTypeString) {
            "busz" -> RouteType.BUS
            "ejszakai" ->
                // Night buses are parsed as buses. Their colors are corrected in parseRouteColors()
                RouteType.BUS
            "hajo" -> RouteType.FERRY
            "villamos" -> RouteType.TRAM
            "trolibusz" -> RouteType.TROLLEYBUS
            "metro" -> RouteType.SUBWAY
            "libego" -> RouteType.CHAIRLIFT
            "hev" -> RouteType.RAIL
            "siklo" -> RouteType.FUNICULAR
            else -> RouteType.OTHER
        }
    }

    /**
     * Returns the background and foreground colors of the route, because the alert list API
     * doesn't return them in the response.
     * Note that the alert detail response contains color values, so the alert detail parsing
     * doesn't need to call this.
     * @param type Parsed type of the route. Most of the time this is enough to match the colors
     * @param shortName Parsed short name (line number) of the route. This is needed because some
     * route types have different colors for each route (eg. subway, ferry).
     * @return  Array of color-ints: background, foreground
     */
    @ColorInt
    private fun parseRouteColors(type: RouteType, shortName: String): IntArray {
        // Color values based on this list of routes:
        // http://online.winmenetrend.hu/budapest/latest/lines

        val defaultBackground = "EEEEEE"
        val defaultText = "BBBBBB"

        val background: String
        val text: String
        when (type) {
            RouteType.BUS -> when {
                shortName.matches("^9[0-9][0-9][A-Z]?$".toRegex()) -> {
                    // Night bus numbers start from 900, and might contain one extra letter after
                    // the 3 digits.
                    background = "1E1E1E"
                    text = "FFFFFF"
                }
                shortName == "I" -> {
                    // Nostalgia bus
                    background = "FFA417"
                    text = "FFFFFF"
                }
                else -> {
                    // Regular bus
                    background = "009FE3"
                    text = "FFFFFF"
                }
            }
            RouteType.FERRY -> if (shortName == "D12") {
                background = "9A1915"
                text = "FFFFFF"
            } else {
                background = "E50475"
                text = "FFFFFF"
            }
            RouteType.RAIL -> when (shortName) {
                "H5" -> {
                    background = "821066"
                    text = "FFFFFF"
                }
                "H6" -> {
                    background = "884200"
                    text = "FFFFFF"
                }
                "H7" -> {
                    background = "EE7203"
                    text = "FFFFFF"
                }
                "H8" -> {
                    background = "FF6677"
                    text = "FFFFFF"
                }
                "H9" -> {
                    background = "FF6677"
                    text = "FFFFFF"
                }
                else -> {
                    background = defaultBackground
                    text = defaultText
                }
            }
            RouteType.TRAM -> {
                background = "FFD800"
                text = "000000"
            }
            RouteType.TROLLEYBUS -> {
                background = "FF1609"
                text = "FFFFFF"
            }
            RouteType.SUBWAY -> when (shortName) {
                "M1" -> {
                    background = "FFD800"
                    text = "000000"
                }
                "M2" -> {
                    background = "FF1609"
                    text = "FFFFFF"
                }
                "M3" -> {
                    background = "005CA5"
                    text = "FFFFFF"
                }
                "M4" -> {
                    background = "19A949"
                    text = "FFFFFF"
                }
                else -> {
                    background = defaultBackground
                    text = defaultText
                }
            }
            RouteType.CHAIRLIFT -> {
                background = "009155"
                text = "000000"
            }
            RouteType.FUNICULAR -> {
                background = "884200"
                text = "000000"
            }
            RouteType.OTHER -> {
                background = defaultBackground
                text = defaultText
            }
        }

        var backgroundColor: Int
        var textColor: Int
        try {
            backgroundColor = Color.parseColor("#$background")
            textColor = Color.parseColor("#$text")
        } catch (ex: IllegalArgumentException) {
            backgroundColor = Color.parseColor("#$defaultBackground")
            textColor = Color.parseColor("#$defaultText")
        }

        return intArrayOf(backgroundColor, textColor)
    }

    private fun createAndStartTrace(name: String) {
        alertDetailTrace = FirebasePerformance.getInstance().newTrace(name)
        alertDetailTrace?.start()
    }

    /**
     * Alerts scheduled for the current day (and not yet started) appear in the current alerts list.
     * We need to find them and move to the future alerts list
     */
    private fun fixFutureAlertsInTodayList(
        alertsToday: MutableList<Alert>,
        alertsFuture: MutableList<Alert>
    ) {
        // Avoiding ConcurrentModificationException when removing from alertsToday
        val todayIterator = alertsToday.listIterator()
        while (todayIterator.hasNext()) {
            val alert = todayIterator.next()
            val startTime: ZonedDateTime = apiTimestampToDateTime(alert.start)
            if (startTime.isAfter(ZonedDateTime.now())) {
                alertsFuture.add(alert)
                todayIterator.remove()
            }
        }
    }
}
