package com.ofalvai.bpinfo.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.support.annotation.RequiresPermission
import android.support.annotation.StringRes
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v7.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.TextView
import com.android.volley.*
import com.ofalvai.bpinfo.Config
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteType
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

val alertStartComparator = compareBy<Alert> { it.start }.thenBy { it.description }

/**
 * Detects if a route seems to be a replacement route from its ID format.
 * It's needed because the API returns replacement routes mixed together with the affected routes.
 */
fun Route.isReplacement(): Boolean {
    /*
      Possible values and meanings:
      BKK_VP: VillamosPótló
      BKK_OP: Operatív Pótló (Metró, Villamos)
      BKK_TP: TroliPótló
      BKK_HP: HévPótló
      BKK_MP: MetróPótló
      to be continued...
     */
    val replacementIdPattern = "BKK_(VP|OP|TP|HP|MP)[0-9A-Z]+"
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
        RouteType._OTHER_ -> R.string.route_other
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
    is TimeoutError -> R.string.error_network
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
    val alertTime = DateTime(start * 1000L)
    val now = DateTime()

    return alertTime.plusHours(Config.ALERT_RECENT_THRESHOLD_HOURS).millis >= now.millis
}

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.hasNetworkConnection(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val activeNetwork = cm.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting
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
fun Alert.formatDate(context: Context): String {
    val startDateTime = DateTime(start * 1000L)
    val endDateTime = DateTime(end * 1000L)
    val startDate = startDateTime.toLocalDate()
    val endDate = endDateTime.toLocalDate()

    val today = DateTime()
    val todayDate = DateTime().toLocalDate()
    val yesterday = today.minusDays(1)
    val yesterdayDate = yesterday.toLocalDate()
    val tomorrow = today.plusDays(1)
    val tomorrowDate = tomorrow.toLocalDate()

    // Alert start, date part
    val startDateString = if (startDate == todayDate) {
        // Start day is today, replacing month and day with today string
        context.getString(R.string.date_today) + " "
    } else if (startDate.year().get() < today.year().get()) {
        // The start year is less than the current year, displaying the year too
        Config.FORMATTER_DATE_YEAR.print(startDateTime)
    } else if (startDate == yesterdayDate) {
        context.getString(R.string.date_yesterday) + " "
    } else if (startDate == tomorrowDate) {
        context.getString(R.string.date_tomorrow) + " "
    } else {
        Config.FORMATTER_DATE.print(startDateTime)
    }

    // Alert start, time part
    val startTimeString = if (startDateTime.hourOfDay().get() == 0 && startDateTime.minuteOfHour().get() == 0) {
        // The API marks "first departure" as 00:00
        context.getString(R.string.date_first_departure)
    } else {
        Config.FORMATTER_TIME.print(startDateTime)
    }

    // Alert end, date part
    val endDateString = if (end == 0L) {
        // The API marks "until further notice" as 0 (in UNIX epoch), no need to display date
        // (the replacement string is displayed as the time part, not the date)
        " "
    } else if (endDate.year().get() > today.year().get()) {
        // The end year is greater than the current year, displaying the year too
        Config.FORMATTER_DATE_YEAR.print(endDateTime)
    } else if (endDate == todayDate) {
        // End day is today, replacing month and day with today string
        context.getString(R.string.date_today) + " "
    } else if (endDate == yesterdayDate) {
        context.getString(R.string.date_yesterday) + " "
    } else if (endDate == tomorrowDate) {
        context.getString(R.string.date_tomorrow) + " "
    } else {
        Config.FORMATTER_DATE.print(endDateTime)
    }

    // Alert end, time part
    val endTimeString = if (end == 0L) {
        // The API marks "until further notice" as 0 (in UNIX epoch)
        context.getString(R.string.date_until_revoke)
    } else if (endDateTime.hourOfDay().get() == 23 && endDateTime.minuteOfHour().get() == 59) {
        // The API marks "last departure" as 23:59
        context.getString(R.string.date_last_departure)
    } else {
        Config.FORMATTER_TIME.print(endDateTime)
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
fun addRouteIcon(context: Context, root: ViewGroup, route: Route) {
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
        activity.startActivity(intent)
    } else {
        val color = ContextCompat.getColor(activity, R.color.colorPrimary)
        val customTabsIntent = CustomTabsIntent.Builder()
                .setToolbarColor(color)
                .build()
        customTabsIntent.intent.`package` = packageName
        customTabsIntent.launchUrl(activity, url)
    }
}