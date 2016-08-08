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

package com.example.bkkinfoplus;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.bkkinfoplus.model.Alert;
import com.example.bkkinfoplus.model.Route;
import com.example.bkkinfoplus.model.RouteType;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by oli on 2016. 06. 14..
 */
public class FutarApiClient implements Response.Listener<JSONObject>, Response.ErrorListener {
    public static final String LANG_HU = "hu";
    public static final String LANG_EN = "en";
    private static final String LANG_SOME = "someTranslation";

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

    private static final String TAG = "FutarApiClient";

    private static final String BASE_URL = "http://futar.bkk.hu/bkk-utvonaltervezo-api/ws/otp/api/where/alert-search.json";

    private static final String QUERY_KEY = "apaiary-test";

    private static final String QUERY_VERSION = "3";

    private static final String QUERY_APPVERSION = "apiary-1.0";

    private static final String QUERY_INCLUDEREFERENCES = "true";

    private RequestQueue mRequestQueue;

    private List<Alert> mAlerts;

    private HashMap<String, Route> mRoutes;

    private FutarApiCallback mApiCallback;

    private String mLanguageCode;

    public interface FutarApiCallback {
        void onAlertResponse(@Nullable List<Alert> alerts);

        void onError(@NonNull Exception ex);
    }

    public FutarApiClient(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;

        mRoutes = new HashMap<>();
        mAlerts = new ArrayList<>();
    }

    @NonNull
    private Uri buildUri() {
        String startTimestamp = String.valueOf(new DateTime().getMillis() / 1000L);

        return Uri.parse(BASE_URL).buildUpon()
                .appendQueryParameter("key", QUERY_KEY)
                .appendQueryParameter("version", QUERY_VERSION)
                .appendQueryParameter("appVersion", QUERY_APPVERSION)
                .appendQueryParameter("includeReferences", QUERY_INCLUDEREFERENCES)
                .appendQueryParameter("start", startTimestamp)
                //.appendQueryParameter("end", startTimestamp)
                .build();
    }

    public void fetchAlertList(@NonNull FutarApiCallback callback, @NonNull String languageCode) {
        // TODO: esetleg egy fetchAll(), és mAlerts-től függően fetchAll() vagy visszatérni az mAlerts-el
        setApiCallback(callback);

        mLanguageCode = languageCode;

        Uri uri = buildUri();

        Log.d(TAG, "API request: " + uri.toString());

        JsonObjectRequest request = new JsonObjectRequest(uri.toString(), null, this, this);

        mRequestQueue.add(request);
    }

    private void setApiCallback(@Nullable FutarApiCallback callback) {
        mApiCallback = callback;
    }

    @Override
    public void onResponse(JSONObject response) {
        try {
            mAlerts = parseAlerts(response, mLanguageCode);
            mRoutes = parseRoutes(response);
        } catch (Exception ex) {
            if (mApiCallback != null) {
                mApiCallback.onError(ex);
            }
        }

        if (mApiCallback != null) {
            mApiCallback.onAlertResponse(mAlerts);
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        if (mApiCallback != null) {
            mApiCallback.onError(error);
        }
    }

    @NonNull
    public List<Alert> parseAlerts(@NonNull JSONObject response, @NonNull String languageCode)
            throws JSONException {
        List<Alert> alertList = new ArrayList<>();

        JSONObject dataNode = response.getJSONObject("data");
        JSONObject entryNode = dataNode.getJSONObject("entry");
        JSONArray alertIdsNode = entryNode.getJSONArray("alertIds");

        if (alertIdsNode.length() == 0) {
            return alertList;
        }

        JSONObject referencesNode = dataNode.getJSONObject("references");
        JSONObject alertsNode = referencesNode.getJSONObject("alerts");

        JSONArray alerts = Utils.jsonObjectToArray(alertsNode);

        for (int i = 0; i < alerts.length(); i++) {
            JSONObject alertNode = alerts.getJSONObject(i);
            Alert alert;
            try {
                alert = parseAlert(alertNode, languageCode);
            } catch (JSONException ex) {
                alert = new Alert(null, 0, 0, 0, null, null, null, null, null);
            }

            // Sometimes the API returns alerts from the future,
            // despite setting the "start" parameter to current time.
            DateTime alertStartTime = new DateTime(alert.getStart() * 1000L);
            if (alertStartTime.isBeforeNow()) {
                alertList.add(alert);
            }
        }

        return alertList;
    }

    @NonNull
    public Alert parseAlert(@NonNull JSONObject alertNode, @NonNull String languageCode)
            throws JSONException {
        String id = alertNode.getString("id");
        long start = alertNode.getLong("start");
        long end;
        try {
            // There are alerts with unknown ends, represented by null
            end = alertNode.getLong("end");
        } catch (JSONException ex) {
            end = 0;
        }

        long timestamp = alertNode.getLong("timestamp");

        JSONArray stopIdsNode = alertNode.getJSONArray("stopIds");
        List<String> stopIds = Utils.jsonArrayToStringList(stopIdsNode);

        JSONArray routeIdsNode = alertNode.getJSONArray("routeIds");
        List<String> routeIds = Utils.jsonArrayToStringList(routeIdsNode);

        JSONObject urlNode = alertNode.getJSONObject("url");

        String url = urlNode.getString("someTranslation") + LANG_PARAM + languageCode;

        String header;
        JSONObject headerNode = alertNode.getJSONObject("header");
        JSONObject translationsNode = headerNode.getJSONObject("translations");
        try {
            // Trying to get the specific language's translation
            // It might be null or completely missing from the response
            header = translationsNode.getString(languageCode);

            if (header == null || header.equals("null")) {
                throw new JSONException("header field is null");
            }
        } catch (JSONException ex) {
            // Falling back to the "someTranslation" field
            header = headerNode.getString(LANG_SOME);
        }
        header = Utils.capitalizeString(header);

        String description;
        JSONObject descriptionNode = alertNode.getJSONObject("description");
        JSONObject translationsNode2 = descriptionNode.getJSONObject("translations");
        try {
            // Trying to get the specific language's translation
            // It might be null or completely missing from the response
            description = translationsNode2.getString(languageCode);

            if (description == null || description.equals("null")) {
                throw new JSONException("description field is null");
            }
        } catch (JSONException ex) {
            // Falling back to the "someTranslation" field
            description = descriptionNode.getString(LANG_SOME);
        }

        return new Alert(id, start, end, timestamp, stopIds, routeIds, url, header, description);
    }

    @NonNull
    public HashMap<String, Route> parseRoutes(@NonNull JSONObject response) throws JSONException {
        HashMap<String, Route> routeMap = new HashMap<>();

        JSONObject dataNode = response.getJSONObject("data");
        JSONObject referencesNode = dataNode.getJSONObject("references");
        JSONObject routesNode = referencesNode.getJSONObject("routes");
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
                Log.e(TAG, "Failed to parse route at index " + i + ":");
                Log.e(TAG, routeNode.toString());
            }
        }

        return routeMap;
    }

    @NonNull
    private Route parseRoute(@NonNull JSONObject routeNode) throws JSONException {
        String id = routeNode.getString("id");
        String shortName = routeNode.getString("shortName");

        String longName;
        try {
            longName = routeNode.getString("longName");
        } catch (JSONException ex) {
            longName = null;
        }

        // Sometimes the description field is missing form the object
        String description;
        try {
            description = routeNode.getString("description");
        } catch (JSONException ex) {
            description = null;
        }

        RouteType type = parseRouteType(routeNode.getString("type"));

        String url;
        try {
            url = routeNode.getString("url");
        } catch (JSONException ex) {
            url = null;
        }

        String color = routeNode.getString("color");
        String textColor = routeNode.getString("textColor");

        return new Route(id, shortName, longName, description, type, url, color, textColor);
    }

    @NonNull
    private RouteType parseRouteType(@NonNull String type) {
        try {
            return RouteType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Failed to parse route type to enum: " + type);
        }
        return RouteType._OTHER_;
    }

    @Nullable
    public Route getRoute(@NonNull String id) {
        return mRoutes.get(id);
    }

    @NonNull
    public List<Route> getAffectedRoutesForAlert(@NonNull Alert alert) {
        List<Route> affectedRoutes = new ArrayList<>();

        for (String routeId : alert.getRouteIds()) {
            Route route = getRoute(routeId);
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
