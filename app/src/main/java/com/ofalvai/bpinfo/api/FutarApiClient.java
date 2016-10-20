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

package com.ofalvai.bpinfo.api;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.ArrayMap;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.crashlytics.android.Crashlytics;
import com.ofalvai.bpinfo.BuildConfig;
import com.ofalvai.bpinfo.Config;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.Route;
import com.ofalvai.bpinfo.model.RouteType;
import com.ofalvai.bpinfo.ui.alertlist.AlertListType;
import com.ofalvai.bpinfo.util.Utils;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ofalvai.bpinfo.util.LogUtils.LOGI;
import static com.ofalvai.bpinfo.util.LogUtils.LOGW;

public class FutarApiClient {
    private static final String TAG = "FutarApiClient";

    private static final String QUERY_API_KEY = BuildConfig.APPLICATION_ID;

    private static final String QUERY_API_VERSION = "3";

    private static final String QUERY_APPVERSION = BuildConfig.VERSION_NAME;

    private static final String QUERY_INCLUDEREFERENCES = "alerts,routes";

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
    private static final String LANG_PARAM = "&lang=";

    private final RequestQueue mRequestQueue;

    @Nullable
    private String mLanguageCode;

    public interface FutarApiListener {
        void onAlertResponse(@NonNull List<Alert> alerts);

        void onError(@NonNull Exception ex);
    }

    public FutarApiClient(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;
    }

    @NonNull
    private Uri buildUri(@NonNull AlertListType alertListType) {
        String startTimestamp = "";

        if (alertListType == AlertListType.ALERTS_TODAY) {
            startTimestamp = String.valueOf(new DateTime().getMillis() / 1000L);
        } else if (alertListType == AlertListType.ALERTS_FUTURE) {
            DateTime now = new DateTime().withTimeAtStartOfDay();
            DateTime tomorrow = now.plusDays(1).withTimeAtStartOfDay();
            startTimestamp = String.valueOf(tomorrow.getMillis() / 1000L);
        } else {
            LOGW(TAG, "No alert list type provided, startTimestamp will be empty");
        }

        return Uri.parse(Config.FUTAR_API_BASE_URL).buildUpon()
                .appendEncodedPath(AlertSearchContract.API_ENDPOINT)
                .appendQueryParameter("key", QUERY_API_KEY)
                .appendQueryParameter("version", QUERY_API_VERSION)
                .appendQueryParameter("appVersion", QUERY_APPVERSION)
                .appendQueryParameter("includeReferences", QUERY_INCLUDEREFERENCES)
                .appendQueryParameter("start", startTimestamp)
                .build();
    }

    public void fetchAlertList(@NonNull final FutarApiListener listener,
                               @NonNull String languageCode,
                               @NonNull final AlertListType alertListType) {
        mLanguageCode = languageCode;

        Uri uri = buildUri(alertListType);

        LOGI(TAG, "API request: " + uri.toString());

        JsonObjectRequest request = new JsonObjectRequest(
                uri.toString(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onResponseCallback(listener, response, alertListType);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onErrorCallback(listener, error);
                    }
                }
        );

        mRequestQueue.add(request);
    }

    /**
     * Parses alerts and routes, then sends the parsed objects to the listener.
     */
    private void onResponseCallback(@NonNull FutarApiListener listener, JSONObject response,
                                    @NonNull AlertListType alertListType) {
        List<Alert> alerts = new ArrayList<>();
        Map<String, Route> routes;
        try {
            routes = parseRoutes(response);
            alerts = parseAlerts(response, alertListType);

            for (Alert alert : alerts) {
                List<Route> affectedRoutes = getAffectedRoutesForAlert(alert, routes);
                alert.setAffectedRoutes(affectedRoutes);
            }
        } catch (Exception ex) {
            listener.onError(ex);
        }

        listener.onAlertResponse(alerts);
    }

    private void onErrorCallback(@NonNull FutarApiListener listener, VolleyError error) {
        listener.onError(error);
    }

    @NonNull
    private List<Alert> parseAlerts(@NonNull JSONObject response, @NonNull AlertListType alertListType)
            throws JSONException {
        List<Alert> alertList = new ArrayList<>();

        JSONObject dataNode = response.getJSONObject(AlertSearchContract.DATA);
        JSONObject entryNode = dataNode.getJSONObject(AlertSearchContract.DATA_ENTRY);
        JSONArray alertIdsNode = entryNode.getJSONArray(AlertSearchContract.DATA_ENTRY_ALERT_IDS);

        if (alertIdsNode.length() == 0) {
            return alertList;
        }

        JSONObject referencesNode = dataNode.getJSONObject(AlertSearchContract.DATA_REFERENCES);
        JSONObject alertsNode = referencesNode.getJSONObject(AlertSearchContract.DATA_REFERENCES_ALERTS);

        JSONArray alerts = Utils.jsonObjectToArray(alertsNode);

        for (int i = 0; i < alerts.length(); i++) {
            JSONObject alertNode = alerts.getJSONObject(i);
            Alert alert;
            try {
                alert = parseAlert(alertNode);
            } catch (JSONException ex) {
                alert = new Alert(null, 0, 0, 0, null, null, null, null, null);

                Crashlytics.log(Log.WARN, TAG, "Alert parse: failed to parse");

            }

            // Time ranges in the API response are messed up. We need to filter out alerts that are
            // before/after the time range we want.
            DateTime alertStartTime = new DateTime(alert.getStart() * 1000L);
            if (alertListType == AlertListType.ALERTS_TODAY && alertStartTime.isBeforeNow()) {
                alertList.add(alert);
            } else if (alertListType == AlertListType.ALERTS_FUTURE && alertStartTime.isAfterNow()) {
                alertList.add(alert);
            }
        }

        return alertList;
    }

    @NonNull
    private Alert parseAlert(@NonNull JSONObject alertNode)
            throws JSONException {

        String id = alertNode.getString(AlertContract.ALERT_ID);
        long start = alertNode.getLong(AlertContract.ALERT_START);
        long end;
        try {
            // There are alerts with unknown ends, represented by null
            end = alertNode.getLong(AlertContract.ALERT_END);
        } catch (JSONException ex) {
            end = 0;
        }

        long timestamp = alertNode.getLong(AlertContract.ALERT_TIMESTAMP);

        JSONArray stopIdsNode = alertNode.getJSONArray(AlertContract.ALERT_STOP_IDS);
        List<String> stopIds = Utils.jsonArrayToStringList(stopIdsNode);

        JSONArray routeIdsNode = alertNode.getJSONArray(AlertContract.ALERT_ROUTE_IDS);
        List<String> routeIds = Utils.jsonArrayToStringList(routeIdsNode);

        JSONObject urlNode = alertNode.getJSONObject(AlertContract.ALERT_URL);

        String url = urlNode.getString(AlertSearchContract.LANG_SOME) + LANG_PARAM + mLanguageCode;

        String header;
        JSONObject headerNode = alertNode.getJSONObject(AlertContract.ALERT_HEADER);
        JSONObject translationsNode = headerNode.getJSONObject(AlertContract.ALERT_HEADER_TRANSLATIONS);
        try {
            // Trying to get the specific language's translation
            // It might be null or completely missing from the response
            header = translationsNode.getString(mLanguageCode);

            if (header == null || header.equals("null")) {
                throw new JSONException("header field is null");
            }
        } catch (JSONException ex) {
            // Falling back to the "someTranslation" field
            header = headerNode.getString(AlertSearchContract.LANG_SOME);
            Crashlytics.log(Log.WARN, TAG, "Alert parse: header translation missing");
        }
        header = Utils.capitalizeString(header);

        String description;
        JSONObject descriptionNode = alertNode.getJSONObject(AlertContract.ALERT_DESC);
        JSONObject translationsNode2 = descriptionNode.getJSONObject(AlertContract.ALERT_DESC_TRANSLATIONS);
        try {
            // Trying to get the specific language's translation
            // It might be null or completely missing from the response
            description = translationsNode2.getString(mLanguageCode);

            if (description == null || description.equals("null")) {
                throw new JSONException("description field is null");
            }
        } catch (JSONException ex) {
            // Falling back to the "someTranslation" field
            description = descriptionNode.getString(AlertSearchContract.LANG_SOME);

            Crashlytics.log(Log.WARN, TAG, "Alert parse: description translation missing");

        }

        return new Alert(id, start, end, timestamp, stopIds, routeIds, url, header, description);
    }

    @NonNull
    private Map<String, Route> parseRoutes(@NonNull JSONObject response) throws JSONException {
        Map<String, Route> routeMap = new ArrayMap<>();

        JSONObject dataNode = response.getJSONObject(AlertSearchContract.DATA);
        JSONObject referencesNode = dataNode.getJSONObject(AlertSearchContract.DATA_REFERENCES);
        JSONObject routesNode = referencesNode.getJSONObject(AlertSearchContract.DATA_REFERENCES_ROUTES);
        JSONArray routesArray = Utils.jsonObjectToArray(routesNode);

        for (int i = 0; i < routesArray.length(); i++) {
            JSONObject routeNode = routesArray.getJSONObject(i);
            Route route;
            try {
                route = parseRoute(routeNode);

                // Replacement routes are inconsistent and unnecessary to display
                if (!Utils.isRouteReplacement(route.getId())) {
                    routeMap.put(route.getId(), route);
                }
            } catch (JSONException ex) {
                Crashlytics.log(Log.ERROR, TAG, "Route parse: failed at index " + i + ":\n" + routeNode.toString());
            }
        }

        return routeMap;
    }

    @NonNull
    private Route parseRoute(@NonNull JSONObject routeNode) throws JSONException {
        String id = routeNode.getString(RouteContract.ROUTE_ID);
        String shortName = routeNode.getString(RouteContract.ROUTE_SHORT_NAME);

        String longName;
        try {
            longName = routeNode.getString(RouteContract.ROUTE_LONG_NAME);
        } catch (JSONException ex) {
            longName = null;
        }

        // Sometimes the description field is missing form the object
        String description;
        try {
            description = routeNode.getString(RouteContract.ROUTE_DESC);
        } catch (JSONException ex) {
            description = null;
        }

        RouteType type = parseRouteType(routeNode.getString(RouteContract.ROUTE_TYPE));

        String url;
        try {
            url = routeNode.getString(RouteContract.ROUTE_URL);
        } catch (JSONException ex) {
            url = null;
        }

        String color = routeNode.getString(RouteContract.ROUTE_COLOR);
        String textColor = routeNode.getString(RouteContract.ROUTE_TEXT_COLOR);

        return new Route(id, shortName, longName, description, type, url, color, textColor);
    }

    @NonNull
    private RouteType parseRouteType(@NonNull String type) {
        try {
            return RouteType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            Crashlytics.log(Log.WARN, TAG, "Route parse: failed to parse route type to enum: " + type);
        }
        return RouteType._OTHER_;
    }

    /**
     * Alerts returned by the API has affected routes' IDs only,
     * but this method returns a list of affected routes from the parsed routes
     */
    @NonNull
    private List<Route> getAffectedRoutesForAlert(@NonNull Alert alert, @NonNull Map<String, Route> routes) {
        List<Route> affectedRoutes = new ArrayList<>();

        for (String routeId : alert.getRouteIds()) {
            Route route = routes.get(routeId);
            if (route == null) {
                // Replacement routes are filtered out at the parse stage,
                // getting a route by the returned routeId might be null.
                continue;
            }
            affectedRoutes.add(route);
        }

        return affectedRoutes;
    }
}
