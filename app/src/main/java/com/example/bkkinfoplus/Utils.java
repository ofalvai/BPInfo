package com.example.bkkinfoplus;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.example.bkkinfoplus.model.Alert;
import com.example.bkkinfoplus.model.Route;
import com.example.bkkinfoplus.model.RouteType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Created by oli on 2016. 06. 15..
 */
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
     * Detects if a route seems to be a replacement route by its ID format.
     * It's needed because the API returns replacement routes mixed together with the affected routes.
     * @param routeId
     * @return
     */
    public static boolean isRouteReplacement(@NonNull String routeId) {
        /**
         * Possible values and meanings:
         * BKK_VP: VillamosPótló
         * BKK_OP: Operatív villamosPótló
         * BKK_TP: TroliPótló
         * BKK_HP: HévPótló
         * to be continued...
         */
        String replacementIdPattern = "BKK_(VP|OP|TP|HP)[0-9]+";

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
     * @param routeToTest
     * @param routes List of routes to test against
     * @return true if routeToTest is visually identical to any of the other routes, false otherwise
     */
    public static boolean isRouteVisuallyDuplicate(@NonNull Route routeToTest, @NonNull List<Route> routes) {
        for (Route route : routes) {
            if (route.getShortName().equals(routeToTest.getShortName()) &&
                    route.getType().equals(routeToTest.getType())) {
                return true;
            }
        }
        return false;
    }
}
