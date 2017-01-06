/*
 * Copyright 2016. 12. 25. Olivér Falvai
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

package com.ofalvai.bpinfo.api.bkkinfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.crashlytics.android.Crashlytics;
import com.ofalvai.bpinfo.BpInfoApplication;
import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.api.AlertApiClient;
import com.ofalvai.bpinfo.api.AlertListErrorMessage;
import com.ofalvai.bpinfo.api.AlertListMessage;
import com.ofalvai.bpinfo.api.AlertRequestParams;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.Route;
import com.ofalvai.bpinfo.model.RouteType;
import com.ofalvai.bpinfo.util.Utils;

import org.greenrobot.eventbus.EventBus;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.ofalvai.bpinfo.util.LogUtils.LOGI;

public class BkkInfoClient implements AlertApiClient {
    private static final String TAG = "BkkInfoClient";

    private static final String API_BASE_URL = "http://bkk.hu/apps/bkkinfo/";

    private static final String API_ENDPOINT_HU = "json.php";

    private static final String API_ENDPOINT_EN = "json_en.php";

    private static final String PARAM_ALERT_LIST = "?lista";

    private static final String PARAM_ALERT_DETAIL = "id";

    private static final String DETAIL_WEBVIEW_BASE_URL = "http://m.bkkinfo.hu/alert.php";

    private static final String DETAIL_WEBVIEW_PARAM_ID = "id";

    @Inject
    SharedPreferences mSharedPreferences;

    @Inject
    Context mContext;

    private RequestQueue mRequestQueue;

    /**
     * This client performs only one alert list API call, because the API is structured in a way
     * that both current and future data is returned at the same time, sometimes even mixed together.
     * If multiple alert list requests are called, only the first will perform the request, and
     * later notify all of its EventBus subscribers.
     */
    private boolean mRequestInProgress = false;

    private List<Alert> mAlertsToday = new ArrayList<>();

    private List<Alert> mAlertsFuture = new ArrayList<>();

    public BkkInfoClient(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;
        BpInfoApplication.injector.inject(this);
    }

    @Override
    public void fetchAlertList(final @NonNull AlertRequestParams params) {
        // If a request is in progress, we don't proceed. The response callback will notify every subscriber
        if (mRequestInProgress) return;

        final Uri url = buildAlertListUrl(params);

        LOGI(TAG, "API request: " + url.toString());

        JsonObjectRequest request = new JsonObjectRequest(
                url.toString(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onAlertListResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        EventBus.getDefault().post(new AlertListErrorMessage(error));
                    }
                }
        );

        mRequestInProgress = true;
        mRequestQueue.add(request);
    }

    @Override
    public void fetchAlert(@NonNull String id, final @NonNull AlertDetailListener listener,
                           @NonNull AlertRequestParams params) {
        Uri url = buildAlertDetailUrl(params, id);

        LOGI(TAG, "API request: " + url.toString());

        JsonObjectRequest request = new JsonObjectRequest(
                url.toString(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onAlertDetailResponse(listener, response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        listener.onError(error);
                    }
                }
        );

        mRequestQueue.add(request);
    }

    private Uri buildAlertListUrl(AlertRequestParams params) {
        String endpoint = params.mLanguageCode.equals("hu") ? API_ENDPOINT_HU :
                API_ENDPOINT_EN;

        return Uri.parse(API_BASE_URL).buildUpon()
                .appendEncodedPath(endpoint)
                .appendEncodedPath(PARAM_ALERT_LIST)
                .build();
    }

    private Uri buildAlertDetailUrl(AlertRequestParams params, String alertId) {
        String endpoint = params.mLanguageCode.equals("hu") ? API_ENDPOINT_HU :
                API_ENDPOINT_EN;

        return Uri.parse(API_BASE_URL).buildUpon()
                .appendEncodedPath(endpoint)
                .appendQueryParameter(PARAM_ALERT_DETAIL, alertId)
                .build();
    }

    private void onAlertListResponse(JSONObject response) {
        try {
            mAlertsToday = parseTodayAlerts(response);
            mAlertsFuture = parseFutureAlerts(response);

            EventBus.getDefault().post(new AlertListMessage(mAlertsToday, mAlertsFuture));
        } catch (Exception ex) {
            EventBus.getDefault().post(new AlertListErrorMessage(ex));
        } finally {
            mRequestInProgress = false;
        }
    }

    private void onAlertDetailResponse(AlertDetailListener listener, JSONObject response) {
        try {
            Alert alert = parseAlertDetail(response);
            listener.onAlertResponse(alert);
        } catch (Exception ex) {
            listener.onError(ex);
        }
    }

    @NonNull
    private List<Alert> parseTodayAlerts(JSONObject response) throws JSONException {
        List<Alert> alerts = new ArrayList<>();

        JSONArray activeAlertsList = response.getJSONArray("active");
        for (int i = 0; i < activeAlertsList.length(); i++) {
            try {
                JSONObject alertNode = activeAlertsList.getJSONObject(i);
                Alert alert = parseAlert(alertNode);

                // Some alerts are still listed a few minutes after they ended, we need to hide them,
                // but still show them if debug mode is enabled
                boolean isDebugMode = mSharedPreferences.getBoolean(
                        mContext.getString(R.string.pref_key_debug_mode), false);
                DateTime alertEndTime = new DateTime(alert.getEnd() * 1000L);
                if (alertEndTime.isAfterNow() || alert.getEnd() == 0 || isDebugMode) {
                    alerts.add(parseAlert(alertNode));
                }
            } catch (JSONException ex) {
                Crashlytics.log(Log.WARN, TAG, "Alert parse: failed to parse:\n" + ex.toString());
            }
        }

        return alerts;
    }

    @NonNull
    private List<Alert> parseFutureAlerts(JSONObject response) throws JSONException {
        List<Alert> alerts = new ArrayList<>();

        // Future alerts are in two groups: near-future and far-future
        JSONArray soonAlertList = response.getJSONArray("soon");
        for (int i = 0; i < soonAlertList.length(); i++) {
            try {
                JSONObject alertNode = soonAlertList.getJSONObject(i);
                alerts.add(parseAlert(alertNode));
            } catch (JSONException ex) {
                Crashlytics.log(Log.WARN, TAG, "Alert parse: failed to parse:\n" + ex.toString());
            }
        }

        JSONArray futureAlertList = response.getJSONArray("future");
        for (int i = 0; i < futureAlertList.length(); i++) {
            try {
                JSONObject alertNode = futureAlertList.getJSONObject(i);
                alerts.add(parseAlert(alertNode));
            } catch (JSONException ex) {
                Crashlytics.log(Log.WARN, TAG, "Alert parse: failed to parse:\n" + ex.toString());
            }
        }

        return alerts;
    }

    /**
     * Parses alert details found in the alert list API response
     * This structure is different than the alert detail API response
     */
    @NonNull
    private Alert parseAlert(JSONObject alertNode) throws JSONException {
        String id = alertNode.getString("id");

        long start = 0;
        if (!alertNode.isNull("kezd")) {
            JSONObject beginNode = alertNode.getJSONObject("kezd");
            start = beginNode.getLong("epoch");
        }

        long end = 0;
        if (!alertNode.isNull("vege")) {
            JSONObject endNode = alertNode.getJSONObject("vege");
            end = endNode.getLong("epoch");
        }

        long timestamp;
        JSONObject modifiedNode = alertNode.getJSONObject("modositva");
        timestamp = modifiedNode.getLong("epoch");

        String url = getUrl(id);

        String header = Utils.capitalizeString(alertNode.getString("elnevezes"));

        String description = null;
        JSONObject optionNode = alertNode.getJSONObject("opcio");
        if (!optionNode.isNull("szabad_szoveg")) {
            description = optionNode.getString("szabad_szoveg");
        }

        List<Route> affectedRoutes;
        JSONArray routesArray = alertNode.getJSONArray("jaratokByFajta");
        affectedRoutes = parseAffectedRoutes(routesArray);

        return new Alert(id, start, end, timestamp, url, header, description, affectedRoutes);
    }

    /**
     * Parses alert details found in the alert detail API response
     * This structure is different than the alert list API response
     */
    private Alert parseAlertDetail(JSONObject response) throws JSONException {
        String id = response.getString("id");

        long start = 0;
        if (!response.isNull("kezdEpoch")) {
            start = response.getLong("kezdEpoch");
        }

        long end = 0;
        if (!response.isNull("vegeEpoch")) {
            end = response.getLong("vegeEpoch");
        }

        long timestamp = 0;
        if (!response.isNull("modEpoch")) {
            timestamp = response.getLong("modEpoch");
        }

        String url = getUrl(id);

        String header;
        // The API returns a header of 3 parts separated by "|" characters. We need the last part.
        String rawHeader = response.getString("targy");
        String[] rawHeaderParts = rawHeader.split("\\|");
        header = Utils.capitalizeString(rawHeaderParts[2].trim());

        String description;
        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append(response.getString("feed"));

        JSONArray routesNode = Utils.jsonObjectToArray(response.getJSONObject("jaratok"));
        for (int i = 0; i < routesNode.length(); i++) {
            JSONObject routeNode = routesNode.getJSONObject(i);
            JSONObject optionsNode = routeNode.getJSONObject("opciok");

            if (!optionsNode.isNull("szabad_szoveg")) {
                JSONArray routeTextArray = optionsNode.getJSONArray("szabad_szoveg");
                for (int j = 0; j < routeTextArray.length(); j++) {
                    descriptionBuilder.append(routeTextArray.getString(j));
                }
            }
        }
        description = descriptionBuilder.toString();

        List<Route> affectedRoutes;
        JSONArray routesArray = Utils.jsonObjectToArray(response.getJSONObject("jarat_adatok"));
        affectedRoutes = parseDetailedAffectedRoutes(routesArray);

        return new Alert(id, start, end, timestamp, url, header, description, affectedRoutes);
    }

    private String getUrl(String alertId) {
        return DETAIL_WEBVIEW_BASE_URL + "?" + DETAIL_WEBVIEW_PARAM_ID + "=" + alertId;
    }

    /**
     * Parses affected routes found in the alert list API response
     * This structure is different than the alert detail API response
     */
    @NonNull
    private List<Route> parseAffectedRoutes(JSONArray routesArray) throws JSONException {
        // The API lists multiple affected routes grouped by their vehicle type (bus, tram, etc.)
        List<Route> routes = new ArrayList<>();
        for (int i = 0; i < routesArray.length(); i++) {
            JSONObject routeNode = routesArray.getJSONObject(i);
            String typeString = routeNode.getString("type");
            RouteType type = parseRouteType(typeString);

            JSONArray concreteRoutes = routeNode.getJSONArray("jaratok");
            for (int j = 0; j < concreteRoutes.length(); j++) {
                String shortName = concreteRoutes.getString(j);
                int[] colors = parseRouteColors(type, shortName);

                // There's no ID returned by the API, using shortName instead
                Route route = new Route(shortName, shortName, null, null, type, colors[0], colors[1]);
                routes.add(route);
            }
        }
        return routes;
    }

    /**
     * Parses affected routes found in the alert detail API response
     * This structure is different than the alert list API response
     */
    @NonNull
    private List<Route> parseDetailedAffectedRoutes(JSONArray routesArray) throws JSONException {
        List<Route> routes = new ArrayList<>();
        for (int i = 0; i < routesArray.length(); i++) {
            JSONObject routeNode = routesArray.getJSONObject(i);

            String id = routeNode.getString("forte");
            String shortName = routeNode.getString("szam");
            String description = routeNode.getString("utvonal");
            RouteType routeType = parseRouteType(routeNode.getString("tipus"));
            int color = Color.parseColor("#" + routeNode.getString("szin"));
            int textColor = Color.parseColor("#" + routeNode.getString("betu"));

            Route route = new Route(id, shortName, null, description, routeType, color, textColor);
            routes.add(route);
        }
        return routes;
    }

    private RouteType parseRouteType(String routeTypeString) {
        switch (routeTypeString) {
            case "busz":
                return RouteType.BUS;
            case "hajo":
                return RouteType.FERRY;
            case "villamos":
                return RouteType.TRAM;
            case "trolibusz":
                return RouteType.TROLLEYBUS;
            case "metro":
                return RouteType.SUBWAY;
            case "libego":
                return RouteType.CHAIRLIFT;
            default:
                return RouteType._OTHER_;
        }
        //TODO: missing: RAIL (HÉV)
    }

    /**
     * Returns the background and foreground colors of the route, because the alert list API
     * doesn't return them in the response.
     * Note that the alert detail response contains color values, so the alert detail parsing
     * doesn't need to call this.
     * @param type Parsed type of the route. Most of the time this is enough to match the colors
     * @param shortName Parsed short name (line number) of the route. This is needed because some
     *                  route types have different colors for each route (eg. subway, ferry).
     * @return  Array of color-ints: background, foreground
     */
    @ColorInt
    private int[] parseRouteColors(RouteType type, String shortName) {
        // Color values based on this list of routes:
        // http://online.winmenetrend.hu/budapest/latest/lines

        String defaultBackground = "EEEEEE";
        String defaultText = "BBBBBB";

        String background;
        String text;
        switch (type) {
            case BUS:
                if (shortName.startsWith("9")) {
                    // Night bus
                    background = "1E1E1E";
                    text = "FFFFFF";
                } else {
                    // Regular bus
                    background = "009FE3";
                    text = "FFFFFF";
                }
                break;
            case FERRY:
                switch (shortName) {
                    case "D2":
                        background = "FF1609";
                        text = "FFFFFF";
                        break;
                    case "D11":
                        background = "D60080";
                        text = "FFFFFF";
                        break;
                    case "D14":
                        background = "D60080";
                        text = "FFFFFF";
                        break;
                    default:
                        background = defaultBackground;
                        text = defaultText;
                }
                break;
            case RAIL:
                switch (shortName) {
                    case "H5":
                        background = "821066";
                        text = "FFFFFF";
                        break;
                    case "H6":
                        background = "824B00";
                        text = "FFFFFF";
                        break;
                    case "H7":
                        background = "EE7203";
                        text = "FFFFFF";
                        break;
                    case "H8":
                        background = "ED677E";
                        text = "FFFFFF";
                        break;
                    case "H9":
                        background = "ED677E";
                        text = "FFFFFF";
                        break;
                    default:
                        background = defaultBackground;
                        text = defaultText;
                }
                break;
            case TRAM:
                background = "FFD800";
                text = "000000";
                break;
            case TROLLEYBUS:
                background = "FF1609";
                text = "FFFFFF";
                break;
            case SUBWAY:
                switch (shortName) {
                    case "M1":
                        background = "FFD800";
                        text = "000000";
                        break;
                    case "M2":
                        background = "E41F18";
                        text = "FFFFFF";
                        break;
                    case "M3":
                        background = "005CA5";
                        text = "FFFFFF";
                        break;
                    case "M4":
                        background = "4CA22F";
                        text = "FFFFFF";
                        break;
                    default:
                        background = defaultBackground;
                        text = defaultText;
                }
                break;
            case CHAIRLIFT:
                background = "009155";
                text = "000000";
                break;
            case _OTHER_:
                background = defaultBackground;
                text = defaultText;
                break;
            default:
                background = defaultBackground;
                text = defaultText;
                break;
        }

        int backgroundColor;
        int textColor;
        try {
            backgroundColor = Color.parseColor("#" + background);
            textColor = Color.parseColor("#" + text);
        } catch (IllegalArgumentException ex) {
            backgroundColor = Color.parseColor("#" + defaultBackground);
            textColor = Color.parseColor("#" + defaultText);
        }

        return new int[] { backgroundColor, textColor};
    }
}
