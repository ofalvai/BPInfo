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

package com.ofalvai.bpinfo.api.bkkfutar

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.crashlytics.android.Crashlytics
import com.ofalvai.bpinfo.BuildConfig
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.api.AlertApiClient
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.alertlist.AlertListType
import com.ofalvai.bpinfo.util.*
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.*

class FutarApiClient(
    private val requestQueue: RequestQueue,
    private val sharedPreferences: SharedPreferences,
    private val context: Context
) : AlertApiClient {

    companion object {
        private const val TAG = "FutarApiClient"

        private const val QUERY_API_KEY = BuildConfig.APPLICATION_ID

        private const val QUERY_API_VERSION = "3"

        private const val QUERY_APPVERSION = BuildConfig.VERSION_NAME

        private const val QUERY_INCLUDEREFERENCES = "alerts,routes"

        // The website doesn't have a language switch, but the URL has a hidden query parameter:
        // /alert.php?id=1234&lang=en
        // This creates a session cookie with the language code, and the website uses this cookie
        // for language selection.
        // The only problem: it seems that the cookie has higher priority that the URL
        // parameter in the language selection, and even though this is a session cookie,
        // Chrome doesn't delete it immediately after closing the custom tab, but when the last
        // Chrome process dies.
        // So it can be really hard to change the website's language AFTER it's been visited with
        // the app set to a different language.
        // But it's better than having no language selection at all.
        private const val LANG_PARAM = "&lang="
    }

    /**
     * There's only one call in the API to get both the list and the details about alerts,
     * so we have to store them after the first call.
     */
    private var alertsToday: List<Alert> = arrayListOf()

    private var alertsFuture: List<Alert> = arrayListOf()

    /**
     * Map of all parsed routes. This is used to set every alert's affected routes by ID.
     */
    private var routes: Map<String, Route> = mutableMapOf()

    private var languageCode: String? = null

    override fun fetchAlertList(listener: AlertApiClient.AlertListListener) {
        languageCode = LocaleManager.getCurrentLanguageCode(sharedPreferences)

        val uri = buildUri()

        Timber.i("API request: %s", uri.toString())

        val request = JsonObjectRequest(
            uri.toString(), null,
            Response.Listener { response ->
                try {
                    routes = parseRoutes(response)
                    alertsToday = parseAlerts(response, AlertListType.ALERTS_TODAY)
                    alertsFuture = parseAlerts(response, AlertListType.ALERTS_FUTURE)
                    listener.onAlertListResponse(alertsToday, alertsFuture)
                } catch (ex: Exception) {
                    listener.onError(ex)
                }
            },
            Response.ErrorListener { error ->
                listener.onError(error)
            }
        )

        requestQueue.add(request)
    }

    override fun fetchAlert(id: String, alertListType: AlertListType,
                            listener: AlertApiClient.AlertDetailListener) {
        if (alertListType == AlertListType.ALERTS_TODAY) {
            alertsToday.find { it.id == id }?.let {
                listener.onAlertResponse(it)
                return
            }

            listener.onError(Exception("Alert not found"))
        } else {
            alertsFuture.find { it.id == id }?.let {
                listener.onAlertResponse(it)
                return
            }

            listener.onError(Exception("Alert not found"))
        }
    }

    private fun buildUri(): Uri {
        val builder = Uri.parse(AlertSearchContract.BASE_URL).buildUpon()
            .appendEncodedPath(AlertSearchContract.API_ENDPOINT)
            .appendQueryParameter("key", QUERY_API_KEY)
            .appendQueryParameter("version", QUERY_API_VERSION)
            .appendQueryParameter("appVersion", QUERY_APPVERSION)
            .appendQueryParameter("includeReferences", QUERY_INCLUDEREFERENCES)

        // In debug mode, all alerts (even past ones) are retrieved
        val isDebugMode = sharedPreferences.getBoolean(
            context.getString(R.string.pref_key_debug_mode), false
        )

        if (!isDebugMode) {
            val startTimestamp: String = Instant.now().epochSecond.toString()
            builder.appendQueryParameter("start", startTimestamp)
        }

        return builder.build()
    }

    @Throws(JSONException::class)
    private fun parseAlerts(response: JSONObject, alertListType: AlertListType): List<Alert> {
        val alertList = ArrayList<Alert>()

        val dataNode = response.getJSONObject(AlertSearchContract.DATA)
        val entryNode = dataNode.getJSONObject(AlertSearchContract.DATA_ENTRY)
        val alertIdsNode = entryNode.getJSONArray(AlertSearchContract.DATA_ENTRY_ALERT_IDS)

        if (alertIdsNode.length() == 0) {
            return alertList
        }

        val referencesNode = dataNode.getJSONObject(AlertSearchContract.DATA_REFERENCES)
        val alertsNode = referencesNode.getJSONObject(AlertSearchContract.DATA_REFERENCES_ALERTS)

        val alerts = alertsNode.toArray()

        for (i in 0 until alerts.length()) {
            val alertNode = alerts.getJSONObject(i)
            val alert: Alert
            try {
                alert = parseAlert(alertNode)

                if (alert.id == "BKK_alert-4") {
                    // This alert is a special message warning users to update the official app
                    continue
                }

                // Time ranges in the API response are messed up. We need to filter out alerts that are
                // before/after the time range we want.
                val alertStartTime: ZonedDateTime = apiTimestampToDateTime(alert.start)
                if (alertListType == AlertListType.ALERTS_TODAY && alertStartTime.isBefore(
                        ZonedDateTime.now()
                    )) {
                    alertList.add(alert)
                } else if (alertListType == AlertListType.ALERTS_FUTURE && alertStartTime.isAfter(
                        ZonedDateTime.now()
                    )) {
                    alertList.add(alert)
                }
            } catch (ex: JSONException) {
                Crashlytics.log(Log.WARN, TAG, "Alert parse: failed to parse:\n" + ex.toString())
            }
        }

        return alertList
    }

    @Throws(JSONException::class)
    private fun parseAlert(alertNode: JSONObject): Alert {

        val id = alertNode.getString(AlertContract.ALERT_ID)
        val start = alertNode.getLong(AlertContract.ALERT_START)
        var end: Long = 0

        if (!alertNode.isNull(AlertContract.ALERT_END)) {
            // There are alerts with unknown ends, represented by null
            end = alertNode.getLong(AlertContract.ALERT_END)
        }

        val timestamp = alertNode.getLong(AlertContract.ALERT_TIMESTAMP)

        val urlNode = alertNode.getJSONObject(AlertContract.ALERT_URL)

        var url: String? = null
        if (!urlNode.isNull(AlertSearchContract.LANG_SOME)) {
            url = urlNode.getString(AlertSearchContract.LANG_SOME) + LANG_PARAM + languageCode
        }

        var header: String?
        val headerNode = alertNode.getJSONObject(AlertContract.ALERT_HEADER)
        val translationsNode = headerNode.getJSONObject(AlertContract.ALERT_HEADER_TRANSLATIONS)
        try {
            // Trying to get the specific language's translation
            // It might be null or completely missing from the response
            header = translationsNode.getString(languageCode)

            if (header == null || header == "null") {
                throw JSONException("header field is null")
            }
        } catch (ex: JSONException) {
            // Falling back to the "someTranslation" field
            header = headerNode.getString(AlertSearchContract.LANG_SOME)
            Crashlytics.log(Log.WARN, TAG, "Alert parse: header translation missing")
        }

        header = header?.capitalize() ?: ""

        val description: String
        val descriptionNode = alertNode.getJSONObject(AlertContract.ALERT_DESC)
        val translationsNode2 = descriptionNode.getJSONObject(AlertContract.ALERT_DESC_TRANSLATIONS)
        if (!translationsNode2.isNull(languageCode)) {
            // Trying to get the specific language's translation
            // It might be null or completely missing from the response
            description = translationsNode2.getString(languageCode)
        } else {
            // Falling back to the "someTranslation" field
            description = descriptionNode.getString(AlertSearchContract.LANG_SOME)

            Crashlytics.log(Log.WARN, TAG, "Alert parse: description translation missing")
        }

        val routeIdsNode = alertNode.getJSONArray(AlertContract.ALERT_ROUTE_IDS)
        val routeIds = routeIdsNode.toStringList()
        val affectedRoutes = getRoutesByIds(routeIds)

        return Alert(id, start, end, timestamp, url, header, description, affectedRoutes, false)
    }

    @Throws(JSONException::class)
    private fun parseRoutes(response: JSONObject): Map<String, Route> {
        val routeMap = mutableMapOf<String, Route>()

        val dataNode = response.getJSONObject(AlertSearchContract.DATA)
        val referencesNode = dataNode.getJSONObject(AlertSearchContract.DATA_REFERENCES)
        val routesNode = referencesNode.getJSONObject(AlertSearchContract.DATA_REFERENCES_ROUTES)
        val routesArray = routesNode.toArray()

        for (i in 0 until routesArray.length()) {
            val routeNode = routesArray.getJSONObject(i)
            val route: Route
            try {
                route = parseRoute(routeNode)

                // Replacement routes are inconsistent and unnecessary to display
                if (!route.isReplacement()) {
                    routeMap[route.id] = route
                }
            } catch (ex: JSONException) {
                Crashlytics.log(
                    Log.ERROR,
                    TAG,
                    "Route parse: failed at index " + i + ":\n" + routeNode.toString()
                )
            }
        }

        return routeMap
    }

    @Throws(JSONException::class)
    private fun parseRoute(routeNode: JSONObject): Route {
        val id = routeNode.getString(RouteContract.ROUTE_ID)
        val shortName = routeNode.getString(RouteContract.ROUTE_SHORT_NAME)

        val longName: String? = try {
            routeNode.getString(RouteContract.ROUTE_LONG_NAME)
        } catch (ex: JSONException) {
            null
        }

        // Sometimes the description field is missing form the object
        val description: String? = try {
            routeNode.getString(RouteContract.ROUTE_DESC)
        } catch (ex: JSONException) {
            null
        }

        val type = parseRouteType(routeNode.getString(RouteContract.ROUTE_TYPE))

        val color = Color.parseColor("#" + routeNode.getString(RouteContract.ROUTE_COLOR))
        val textColor = Color.parseColor("#" + routeNode.getString(RouteContract.ROUTE_TEXT_COLOR))

        return Route(id, shortName, longName, description, type, color, textColor, false)
    }

    private fun parseRouteType(type: String): RouteType {
        try {
            return RouteType.valueOf(type)
        } catch (ex: IllegalArgumentException) {
            Crashlytics.log(Log.WARN, TAG, "Route parse: failed to parse route type to enum: $type")
        }

        return RouteType._OTHER_
    }

    /**
     * Alerts returned by the API has affected routes' IDs only,
     * but this method returns a list of affected routes from the parsed routes
     */
    private fun getRoutesByIds(routeIds: List<String>): List<Route> {
        val affectedRoutes = ArrayList<Route>()

        if (routes.isEmpty()) {
            return affectedRoutes
        }

        // Replacement routes are filtered out at the parse stage,
        // getting a route by the returned routeId might be null, which is ok.
        routeIds.mapNotNullTo(affectedRoutes) {
            routes[it]
        }

        return affectedRoutes
    }
}
