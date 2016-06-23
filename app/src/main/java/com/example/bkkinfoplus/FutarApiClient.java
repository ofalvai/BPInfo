package com.example.bkkinfoplus;

import android.net.Uri;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.bkkinfoplus.model.Alert;
import com.example.bkkinfoplus.model.Route;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by oli on 2016. 06. 14..
 */
public class FutarApiClient implements Response.Listener<JSONObject>, Response.ErrorListener {
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

    public interface FutarApiCallback {
        void onAlertResponse(List<Alert> alerts);

        void onError(Exception ex);
    }

    public FutarApiClient(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;

        mRoutes = new HashMap<>();
        mAlerts = new ArrayList<>();
    }

    private Uri buildUri() {
        String startTimestamp = String.valueOf(new GregorianCalendar().getTimeInMillis() / 1000L);

        return Uri.parse(BASE_URL).buildUpon()
                .appendQueryParameter("key", QUERY_KEY)
                .appendQueryParameter("version", QUERY_VERSION)
                .appendQueryParameter("appVersion", QUERY_APPVERSION)
                .appendQueryParameter("includeReferences", QUERY_INCLUDEREFERENCES)
                .appendQueryParameter("start", startTimestamp)
                //.appendQueryParameter("end", startTimestamp)
                .build();
    }

    public void fetchAlertList(FutarApiCallback callback) {
        // TODO: esetleg egy fetchAll(), és mAlerts-től függően fetchAll() vagy visszatérni az mAlerts-el
        setApiCallback(callback);

        Uri uri = buildUri();

        JsonObjectRequest request = new JsonObjectRequest(uri.toString(), null, this, this);

        mRequestQueue.add(request);
    }

    private void setApiCallback(FutarApiCallback callback) {
        mApiCallback = callback;
    }

    @Override
    public void onResponse(JSONObject response) {
        try {
            mAlerts = parseAlerts(response);
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

    public List<Alert> parseAlerts(JSONObject response) throws JSONException {
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

        Date now = new Date();

        for (int i = 0; i < alerts.length(); i++) {
            JSONObject alertNode = alerts.getJSONObject(i);
            Alert alert;
            try {
                alert = parseAlert(alertNode);
            } catch (JSONException ex) {
                alert = new Alert(null, 0, 0, 0, null, null, null, null, null);
            }

            // Sometimes the API returns alerts from the future,
            // despite settings the "start" parameter to current time.
            if (alert.getStart() <= (now.getTime() / 1000L)) {
                alertList.add(alert);
            }
        }

        return alertList;
    }

    public Alert parseAlert(JSONObject alertNode) throws JSONException {
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
        String url = urlNode.getString("someTranslation");

        JSONObject headerNode = alertNode.getJSONObject("header");
        JSONObject translationsNode = headerNode.getJSONObject("translations");
        String header = translationsNode.getString("hu");
        header = Utils.capitalizeString(header);

        JSONObject descriptionNode = alertNode.getJSONObject("description");
        JSONObject translationsNode2 = descriptionNode.getJSONObject("translations");
        String description = translationsNode2.getString("hu");

        return new Alert(id, start, end, timestamp, stopIds, routeIds, url, header, description);
    }

    public HashMap<String, Route> parseRoutes(JSONObject response) throws JSONException {
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

                if (!Utils.isRouteReplacement(route.getId())) {
                    // Replacement routes are inconsistent and unnecessary to display
                    routeMap.put(route.getId(), route);
                }
            } catch (JSONException ex) {
                Log.e(TAG, "Failed to parse route at index " + i + ":");
                if (routeNode != null) {
                    Log.e(TAG, routeNode.toString());
                }
            }
        }

        return routeMap;
    }

    private Route parseRoute(JSONObject routeNode) throws JSONException {
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

        String type = routeNode.getString("type");

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

    public Route getRoute(String id) {
        return mRoutes.get(id);
    }

    public List<Route> getAffectedRoutesForAlert(Alert alert) {
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
