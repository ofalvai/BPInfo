/*
 * Copyright 2016 Olivér Falvai
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.ofalvai.bpinfo.util;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.annotation.StringRes;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.ofalvai.bpinfo.Config;
import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.Route;
import com.ofalvai.bpinfo.model.RouteType;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class Utils {

    @NonNull
    @CheckResult
    public static String capitalizeString(@NonNull String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static class AlertStartTimestampComparator implements Comparator<Alert> {
        @Override
        public int compare(Alert lhs, Alert rhs) {
            return Long.valueOf(lhs.getStart()).compareTo(rhs.getStart());
        }
    }

    /**
     * Detects if a route seems to be a replacement route from its ID format.
     * It's needed because the API returns replacement routes mixed together with the affected routes.
     */
    public static boolean isRouteReplacement(@NonNull String routeId) {
        /**
         * Possible values and meanings:
         * BKK_VP: VillamosPótló
         * BKK_OP: Operatív Pótló (Metró, Villamos)
         * BKK_TP: TroliPótló
         * BKK_HP: HévPótló
         * BKK_MP: MetróPótló
         * to be continued...
         */
        String replacementIdPattern = "BKK_(VP|OP|TP|HP|MP)[0-9A-Z]+";

        return routeId.matches(replacementIdPattern);
    }

    public static String routeTypeToString(@NonNull Context context, @NonNull RouteType routeType) {
        int resourceId;
        switch (routeType) {
            case BUS:
                resourceId = R.string.route_bus;
                break;
            case FERRY:
                resourceId = R.string.route_ferry;
                break;
            case RAIL:
                resourceId = R.string.route_rail;
                break;
            case TRAM:
                resourceId = R.string.route_tram;
                break;
            case TROLLEYBUS:
                resourceId = R.string.route_trolleybus;
                break;
            case SUBWAY:
                resourceId = R.string.route_subway;
                break;
            case _OTHER_:
                resourceId = R.string.route_other;
                break;
            default:
                resourceId = R.string.route_other;
        }
        return context.getString(resourceId);
    }

    @NonNull
    public static List<String> jsonArrayToStringList(@NonNull JSONArray array) throws JSONException {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }

        return list;
    }

    @NonNull
    public static JSONArray jsonObjectToArray(@NonNull JSONObject object) throws JSONException {
        Iterator<String> keys = object.keys();
        JSONArray result = new JSONArray();

        while (keys.hasNext()) {
            String key = keys.next();
            result.put(object.getJSONObject(key));
        }

        return result;
    }

    /**
     * Returns the appropriate error message depending on the concrete error type
     * @param error VolleyError object
     * @return  ID of the String resource of the appropriate error message
     */
    @StringRes
    public static int volleyErrorTypeHandler(@NonNull VolleyError error) {
        int stringId;

        if (error instanceof NoConnectionError) {
            stringId = R.string.error_no_connection;
        } else if (error instanceof NetworkError || error instanceof TimeoutError ) {
            stringId = R.string.error_network;
        } else if (error instanceof ServerError) {
            stringId = R.string.error_response;
        } else {
            stringId = R.string.error_communication;
        }

        return stringId;
    }

    /**
     * Certain affected routes returned by the alerts API are visually identical if we display only
     * their shortName and color. It's enough to display only one of these routes, so every affected
     * route can be compared to the already displayed routes with this method.
     * @param routes List of routes to test against
     * @return true if routeToTest is visually identical to any of the other routes, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isRouteVisuallyDuplicate(@NonNull Route routeToTest, @NonNull List<Route> routes) {
        for (Route route : routes) {
            if (route.getShortName() == null) {
                continue;
            } else if (route.getShortName().equals(routeToTest.getShortName()) &&
                    route.getType().equals(routeToTest.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether an alert counts as a recent one based on the start timestamp.
     */
    public static boolean isAlertRecent(@NonNull Alert alert) {
        DateTime alertTime = new DateTime(alert.getStart() * 1000L);
        DateTime now = new DateTime();

        return alertTime.plusHours(Config.ALERT_RECENT_THRESHOLD_HOURS).getMillis() >= now.getMillis();
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public static boolean hasNetworkConnection(@NonNull Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
