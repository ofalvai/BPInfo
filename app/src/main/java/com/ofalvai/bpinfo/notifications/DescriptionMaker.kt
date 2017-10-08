package com.ofalvai.bpinfo.notifications

import android.content.Context
import com.ofalvai.bpinfo.R


class DescriptionMaker {

    companion object {

        const val DATA_KEY_ROUTE_BUS = "route_bus"
        const val DATA_KEY_ROUTE_FERRY = "route_ferry"
        const val DATA_KEY_ROUTE_RAIL = "route_rail"
        const val DATA_KEY_ROUTE_TRAM = "route_tram"
        const val DATA_KEY_ROUTE_TROLLEYBUS = "route_trolleybus"
        const val DATA_KEY_ROUTE_SUBWAY = "route_subway"
        const val DATA_KEY_ROUTE_OTHER = "route_other"

        const val DATA_KEY_ROUTE_SEPARATOR = "|"

        /**
         * Makes the localized description of affected routes, grouped by route types.
         * One route type per line, structure of line (route list) depends on locale.
         * @param routeData Map of route type keys and route short names separated by "|"
         * @param context Needed for localized string resources
         */
        @JvmStatic
        fun makeDescription(routeData: Map<String, String>, context: Context): String {
            val subwayList = makeRouteList(routeData[DATA_KEY_ROUTE_SUBWAY], context, DATA_KEY_ROUTE_SUBWAY)
            val busList = makeRouteList(routeData[DATA_KEY_ROUTE_BUS], context, DATA_KEY_ROUTE_BUS)
            val tramList = makeRouteList(routeData[DATA_KEY_ROUTE_TRAM], context, DATA_KEY_ROUTE_TRAM)
            val trolleyList = makeRouteList(routeData[DATA_KEY_ROUTE_TROLLEYBUS], context, DATA_KEY_ROUTE_TROLLEYBUS)
            val railList = makeRouteList(routeData[DATA_KEY_ROUTE_RAIL], context, DATA_KEY_ROUTE_RAIL)
            val ferryList = makeRouteList(routeData[DATA_KEY_ROUTE_FERRY], context, DATA_KEY_ROUTE_FERRY)
            val otherList = makeRouteList(routeData[DATA_KEY_ROUTE_OTHER], context, DATA_KEY_ROUTE_OTHER)

            return arrayListOf(subwayList, busList, tramList, trolleyList, railList, ferryList, otherList)
                    .filter { it.isNotEmpty() }
                    .joinToString(separator = "\n")
        }

        private fun makeRouteList(routeData: String?, context: Context, routeType: String): String {
            val langCode = context.resources.configuration.locale.language
            if (langCode == "hu") {
                return makeRouteLineHu(routeData, context, routeType)
            } else {
                return makeRouteLineEn(routeData, context, routeType)
            }
        }

        private fun getLocalizedRouteType(context: Context, routeType: String): String = when (routeType) {
            DATA_KEY_ROUTE_BUS -> context.getString(R.string.route_bus)
            DATA_KEY_ROUTE_FERRY -> context.getString(R.string.route_ferry)
            DATA_KEY_ROUTE_RAIL -> context.getString(R.string.route_rail)
            DATA_KEY_ROUTE_TRAM -> context.getString(R.string.route_tram)
            DATA_KEY_ROUTE_TROLLEYBUS -> context.getString(R.string.route_trolleybus)
            DATA_KEY_ROUTE_SUBWAY -> context.getString(R.string.route_subway)
            DATA_KEY_ROUTE_OTHER -> context.getString(R.string.route_other)
            else -> context.getString(R.string.route_other)
        }

        private fun makeRouteLineHu(routeData: String?, context: Context, routeType: String): String {
            if (routeData != null && routeData.isNotEmpty()) {
                val name = getLocalizedRouteType(context, routeType)

                return routeData
                        .split(DATA_KEY_ROUTE_SEPARATOR)
                        .map { it.trim() }
                        .map(this::numberPostfixHu)
                        .joinToString(separator = ", ")
                        .plus(" $name") // TODO: other?
            } else {
                return ""
            }
        }

        private fun makeRouteLineEn(routeData: String?, context: Context, routeType: String): String {
            val sb = StringBuilder()
            if (routeData != null && routeData.isNotEmpty()) {
                val name = getLocalizedRouteType(context, routeType)
                sb.append("$name ")
                val routeList = routeData
                        .split(DATA_KEY_ROUTE_SEPARATOR)
                        .map { it.trim() }
                        .joinToString(separator = ", ")
                sb.append(routeList)
            }
            return sb.toString()
        }

        private fun numberPostfixHu(name: String): String {
            when (name.last()) {
                'A', 'E' -> return name
                '1', '2', '4', '7', '9' -> return "$name-es"
                '3', '8' -> return "$name-as"
                '5' -> return "$name-ös"
                '6' -> return "$name-os"
                '0' -> when (name.takeLast(2)) {
                    "10", "40", "50", "70", "90" -> return "$name-es"
                    "20", "30", "60", "80", "00" -> return "$name-as"
                    else -> return name
                }
                else -> return name
            }
        }
    }
}