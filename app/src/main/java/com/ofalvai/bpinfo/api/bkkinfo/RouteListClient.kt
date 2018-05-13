package com.ofalvai.bpinfo.api.bkkinfo

import android.graphics.Color
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteType
import org.json.JSONException
import org.json.JSONObject

class RouteListClient(private val requestQueue: RequestQueue) {

    interface RouteListListener {

        fun onRouteListResponse(routeList: List<Route>)

        fun onRouteListError(ex: Exception)
    }

    companion object {

        const val URL = "https://bpinfo-backend-api.herokuapp.com/api/v1/routes"

        const val ROUTE_ID_UNKNOWN = "UNKNOWN"

        const val DEFAULT_COLOR_BG = "EEEEEE"

        const val DEFAULT_COLOR_TEXT = "BBBBBB"

        const val KEY_ID = "id"

        const val KEY_DETAILS = "adatok"

        const val KEY_DESC = "leiras"

        const val KEY_TYPE = "tipus"

        const val KEY_COLOR_BG = "szin"

        const val KEY_COLOR_TEXT = "betu"

        const val KEY_KEY = "kulcs"

        /**
         * Some routes are still kept in the list, but with a warning description.
         * We don't need these routes.
         */
        const val VALUE_DISCONTINUED = "megszűnt"
    }

    fun fetchRouteList(listener: RouteListListener) {
        val request = JsonObjectRequest(
                URL,
                null,
                {
                    try {
                        val routeList = parseRouteList(it)
                        listener.onRouteListResponse(routeList)
                    } catch (ex: JSONException) {
                        listener.onRouteListError(ex)
                    }
                },
                {
                    listener.onRouteListError(it)
                }
        )

        requestQueue.add(request)
    }

    private fun parseRouteList(routeListJson: JSONObject?): List<Route> {
        val routeList = mutableListOf<Route>()

        if (routeListJson == null) return routeList

        routeListJson.keys().forEach {
            val routeJson = routeListJson.getJSONObject(it)
            val route = parseRoute(it, routeJson)
            if (!route.discontinued) {
                routeList.add(route)
            }
        }

        return routeList
    }

    private fun parseRoute(key: String, routeJson: JSONObject?): Route {
        val details = routeJson?.getJSONObject(KEY_DETAILS)

        val backgroundColor = details?.getString(KEY_COLOR_BG) ?: DEFAULT_COLOR_BG
        val textColor = details?.getString(KEY_COLOR_TEXT) ?: DEFAULT_COLOR_TEXT
        val type = parseRouteType(details?.getString(KEY_TYPE), key.trim())
        val discontinued = details?.getString(KEY_KEY).equals(VALUE_DISCONTINUED)

        return Route(
                id = details?.getString(KEY_ID) ?: ROUTE_ID_UNKNOWN,
                shortName = key.trim(),
                longName = null,
                description = details?.getString(KEY_DESC)?.replace("&nbsp;", ""),
                type = type,
                color = Color.parseColor("#" + backgroundColor),
                textColor = Color.parseColor("#" + textColor),
                discontinued = discontinued
        )
    }

    private fun parseRouteType(typeString: String?, shortName: String?): RouteType {
        if (typeString == null) {
            return RouteType._OTHER_
        } else {
            return when (typeString) {
                "B" -> RouteType.BUS
                "E" -> RouteType.BUS // Night bus
                "M" -> RouteType.SUBWAY
                "T" -> RouteType.TROLLEYBUS
                "V" -> RouteType.TRAM
                "D" -> RouteType.FERRY
                "H" -> RouteType.RAIL
                "P" -> RouteType._OTHER_ // Sétajárat
                "L" -> RouteType._OTHER_ // RouteType.CHAIRLIFT
                "S" -> RouteType._OTHER_ // Sikló
                "N" -> RouteType._OTHER_ // Nosztalgia: http://www.bkk.hu/nosztalgia/menetrendek
//                    "SH" -> RouteType.FERRY // Sétahajó
//                    "DH" -> RouteType.TRAM // Duna villamos
//                    "TH" -> RouteType.TRAM // Termál villamos
//                    "BH" -> RouteType.BUS // Nosztalgiabusz
//                    "BP100" -> RouteType.TRAM // 4-6 nosztalgia
//                    "NT" -> RouteType.TROLLEYBUS // Nosztalgia troli
//                    "N74" -> RouteType.TROLLEYBUS
//                    "N76" -> RouteType.TROLLEYBUS
//                    "N17" -> RouteType.TRAM
                else -> RouteType._OTHER_
            }
        }
    }
}
