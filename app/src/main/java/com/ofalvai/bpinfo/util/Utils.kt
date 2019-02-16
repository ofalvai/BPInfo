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

package com.ofalvai.bpinfo.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.android.volley.*
import com.ofalvai.bpinfo.Config
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Detects if a route seems to be a replacement route from its ID format.
 * It's needed because the API returns replacement routes mixed together with the affected routes.
 */
fun Route.isReplacement(): Boolean {
    /*
      Possible values and meanings:
      BKK_VP: VillamosPótló
      BKK_V: VillamosPótló
      BKK_OP: Operatív Pótló (Metró, Villamos)
      BKK_TP: TroliPótló
      BKK_HP: HévPótló
      BKK_MP: MetróPótló
      to be continued...
     */
    val replacementIdPattern = "BKK_(VP?|OP|TP|HP|MP)[0-9A-Z]+"
    return id.matches(replacementIdPattern.toRegex())
}

fun RouteType.getName(context: Context): String {
    val resourceId: Int = when (this) {
        RouteType.BUS -> R.string.route_bus
        RouteType.FERRY -> R.string.route_ferry
        RouteType.RAIL -> R.string.route_rail
        RouteType.TRAM -> R.string.route_tram
        RouteType.TROLLEYBUS -> R.string.route_trolleybus
        RouteType.SUBWAY -> R.string.route_subway
        RouteType.OTHER -> R.string.route_other
        else -> R.string.route_other
    }
    return context.getString(resourceId)
}

@Throws(JSONException::class)
fun JSONArray.toStringList(): List<String> {
    return (0 until length()).map { getString(it) }
}

@Throws(JSONException::class)
fun JSONObject.toArray(): JSONArray {
    val keys = keys()
    val result = JSONArray()

    while (keys.hasNext()) {
        val key = keys.next()
        result.put(getJSONObject(key))
    }

    return result
}

/**
 * Returns the appropriate error message depending on the concrete error type
 * @return  ID of the String resource of the appropriate error message
 */
@StringRes
fun VolleyError.toStringRes() = when (this) {
    is NoConnectionError -> R.string.error_no_connection
    is NetworkError -> R.string.error_network
    is TimeoutError -> R.string.error_no_connection
    is ServerError -> R.string.error_response
    else -> R.string.error_communication
}

/**
 * Certain affected routes returned by the alerts API are visually identical if we display only
 * their shortName and color. It's enough to display only one of these routes, so every affected
 * route can be compared to the already displayed routes with this method.
 * @param routes List of routes to test against
 * @return true if routeToTest is visually identical to any of the other routes, false otherwise
 */
fun isRouteVisuallyDuplicate(routeToTest: Route, routes: List<Route>): Boolean {
    for ((_, shortName, _, _, type) in routes) {
        if (shortName == routeToTest.shortName && type == routeToTest.type) {
            return true
        }
    }
    return false
}

/**
 * Returns whether an alert counts as a recent one based on the start timestamp.
 */
fun Alert.isRecent(): Boolean {
    val startPlusThreshold: ZonedDateTime = apiTimestampToDateTime(start)
        .plusHours(Config.Behavior.ALERT_RECENT_THRESHOLD_HOURS.toLong())

    return startPlusThreshold.isAfter(ZonedDateTime.now())
}

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.hasNetworkConnection(): Boolean {
    val cm = getSystemService<ConnectivityManager>()

    val activeNetwork = cm?.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnected
}

fun Route.getContentDescription(context: Context): String {
    val routeType = type.getName(context)
    return "$routeType $shortName"
}

/**
 * Transforms the start and end timestamps into a human-friendly readable string,
 * with special replacements for special dates, times, and the API's strange notations.
 * @return  A string in the format of {startdate} {starttime} {separator} {enddate} {endtime}
 */
@Suppress("CascadeIf")
fun Alert.formatDate(context: Context): String {
    val startDateTime: ZonedDateTime = apiTimestampToDateTime(start)
    val endDateTime = apiTimestampToDateTime(end)
    val startDate = startDateTime.toLocalDate()
    val endDate = endDateTime.toLocalDate()

    val today = ZonedDateTime.now()
    val todayDate = today.toLocalDate()
    val yesterday = today.minusDays(1)
    val yesterdayDate = yesterday.toLocalDate()
    val tomorrow = today.plusDays(1)
    val tomorrowDate = tomorrow.toLocalDate()

    // Alert start, date part
    val startDateString = if (startDate == todayDate) {
        // Start day is today, replacing month and day with today string
        context.getString(R.string.date_today) + " "
    } else if (startDate.year < today.year) {
        // The start year is less than the current year, displaying the year too
        Config.FORMATTER_DATE_YEAR.format(startDateTime)
    } else if (startDate == yesterdayDate) {
        context.getString(R.string.date_yesterday) + " "
    } else if (startDate == tomorrowDate) {
        context.getString(R.string.date_tomorrow) + " "
    } else {
        Config.FORMATTER_DATE.format(startDateTime)
    }

    // Alert start, time part
    val startTimeString = if (startDateTime.hour == 0 && startDateTime.minute == 0) {
        // The API marks "first departure" as 00:00
        context.getString(R.string.date_first_departure)
    } else {
        Config.FORMATTER_TIME.format(startDateTime)
    }

    // Alert end, date part
    val endDateString = if (end == 0L) {
        // The API marks "until further notice" as 0 (in UNIX epoch), no need to display date
        // (the replacement string is displayed as the time part, not the date)
        " "
    } else if (endDate.year > today.year) {
        // The end year is greater than the current year, displaying the year too
        Config.FORMATTER_DATE_YEAR.format(endDateTime)
    } else if (endDate == todayDate) {
        // End day is today, replacing month and day with today string
        context.getString(R.string.date_today) + " "
    } else if (endDate == yesterdayDate) {
        context.getString(R.string.date_yesterday) + " "
    } else if (endDate == tomorrowDate) {
        context.getString(R.string.date_tomorrow) + " "
    } else {
        Config.FORMATTER_DATE.format(endDateTime)
    }

    // Alert end, time part
    val endTimeString = if (end == 0L) {
        // The API marks "until further notice" as 0 (in UNIX epoch)
        context.getString(R.string.date_until_revoke)
    } else if (endDateTime.hour == 23 && endDateTime.minute == 59) {
        // The API marks "last departure" as 23:59
        context.getString(R.string.date_last_departure)
    } else {
        Config.FORMATTER_TIME.format(endDateTime)
    }

    return startDateString + startTimeString + Config.DATE_SEPARATOR + endDateString + endTimeString
}

/**
 * Adds a rectangular icon for the affected route.
 *
 * First it creates a TextView, then sets the style properties of the view.
 * The custom colored rounded background is achieved by a Drawable and a ColorFilter on top of.
 * @param context Context
 * @param root ViewGroup to add the icon to
 * @param route
 */
@SuppressLint("RestrictedApi")
fun addRouteIcon(context: Context, root: ViewGroup, route: Route): TextView {
    val iconContextTheme = ContextThemeWrapper(context, R.style.RouteIcon)
    val iconView = TextView(iconContextTheme)

    iconView.text = route.shortName
    iconView.setTextColor(route.textColor)
    iconView.contentDescription = route.getContentDescription(context)
    root.addView(iconView)

    // Layout attributes defined in R.style.RouteIcon were ignored before attaching the view to
    // a parent, so we need to manually set them
    val params = iconView.layoutParams as ViewGroup.MarginLayoutParams
    params.width = ViewGroup.LayoutParams.WRAP_CONTENT
    val margin = context.resources.getDimension(R.dimen.route_icon_margin).toInt()
    params.rightMargin = margin
    params.topMargin = margin
    // A requestLayout() call is not necessary here because the setBackground() method below
    // will call that anyway.
    //iconView.requestLayout();

    // Setting a custom colored rounded background drawable as background
    val iconBackground = ContextCompat.getDrawable(context, R.drawable.rounded_corner_5dp)
    if (iconBackground != null) {
        val colorFilter = LightingColorFilter(Color.rgb(1, 1, 1), route.color)
        iconBackground.mutate().colorFilter = colorFilter
        iconView.background = iconBackground
    }

    return iconView
}

/**
 * Opens a Chrome custom tab styled to the application's theme
 * @param activity  Used for context and launching fallback intent
 * @param url   URL to open
 */
fun openCustomTab(activity: Activity, url: Uri) {
    val packageName = CustomTabsHelper.getPackageNameToUse(activity)

    if (packageName == null) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = url

        if (intent.resolveActivity(activity.packageManager!!) != null) {
            activity.startActivity(intent)
        }
    } else {
        val color = ContextCompat.getColor(activity, R.color.primary)
        val customTabsIntent = CustomTabsIntent.Builder()
                .setToolbarColor(color)
                .build()
        customTabsIntent.intent.`package` = packageName
        customTabsIntent.launchUrl(activity, url)
    }
}

fun TextView.underline() {
    this.paintFlags = this.paintFlags or Paint.UNDERLINE_TEXT_FLAG
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun apiTimestampToDateTime(seconds: Long): ZonedDateTime {
    return ZonedDateTime.ofInstant(
        Instant.ofEpochSecond(seconds),
        ZoneId.of("Europe/Budapest") // We can safely assume the timezone of the API
    )
}

fun <T> Request<T>.addTo(queue: RequestQueue) {
    queue.add(this)
}

fun <T> AppCompatActivity.observe(liveData: LiveData<T>, observer: (T) -> Unit) {
    liveData.observe(this, Observer { observer.invoke(it) })
}

fun <T> Fragment.observe(liveData: LiveData<T>, observer: (T) -> Unit) {
    liveData.observe(viewLifecycleOwner, Observer { observer.invoke(it) })
}