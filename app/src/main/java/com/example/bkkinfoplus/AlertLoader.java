package com.example.bkkinfoplus;

import android.content.Context;
import android.net.Uri;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by oli on 2016. 06. 14..
 */
public class AlertLoader {

    private RequestQueue mRequestQueue;

    private static final String BASE_URL = "http://futar.bkk.hu/bkk-utvonaltervezo-api/ws/otp/api/where/alert-search.json";

    private static final String QUERY_KEY = "apaiary-test";

    private static final String QUERY_VERSION = "3";

    private static final String QUERY_APPVERSION = "apiary-1.0";

    private static final String QUERY_INCLUDEREFERENCES = "true";

    private HashMap<String, Route> mRoutes;

    @Inject
    public AlertLoader(RequestQueue requestQueue) {
        // This is for dependency injection
        mRequestQueue = requestQueue;

        mRoutes = new HashMap<>();
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

    public void fetchAlertList(Context context, Response.Listener<JSONObject> responseListener,
                               Response.ErrorListener errorListener) {
        Uri uri = buildUri();

        JsonObjectRequest request = new JsonObjectRequest(uri.toString(), null,
                responseListener, errorListener);

        mRequestQueue.add(request);
    }

    public List<Alert> parseAlertList(JSONObject response) throws JSONException {
        List<Alert> alertList = new ArrayList<>();

        JSONObject dataNode = response.getJSONObject("data");
        JSONObject entryNode = dataNode.getJSONObject("entry");
        JSONArray alertIdsNode = entryNode.getJSONArray("alertIds");

        if (alertIdsNode.length() == 0) {
            return alertList;
        }

        JSONObject referencesNode = dataNode.getJSONObject("references");
        JSONObject alertsNode = referencesNode.getJSONObject("alerts");

        JSONArray alerts = jsonObjectToArray(alertsNode);

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
        List<String> stopIds = jsonArrayToStringList(stopIdsNode);

        JSONArray routeIdsNode = alertNode.getJSONArray("routeIds");
        List<String> routeIds = jsonArrayToStringList(routeIdsNode);

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



    public void updateRoutes(JSONObject response) throws JSONException {
        JSONObject dataNode = response.getJSONObject("data");
        JSONObject referencesNode = dataNode.getJSONObject("references");
        JSONObject routesNode = referencesNode.getJSONObject("routes");
        JSONArray routesArray = jsonObjectToArray(routesNode);

        for (int i = 0; i < routesArray.length(); i++) {
            JSONObject routeNode = routesArray.getJSONObject(i);
            Route route;
            try {
                route = parseRoute(routeNode);

                if (!Utils.isRouteReplacement(route.getId())) {
                    // Replacement routes are inconsistent and unnecessary to display
                    mRoutes.put(route.getId(), route);
                }
            } catch (JSONException ex) {
                // TODO: valami logolás
            }
        }
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

    private static List<String> jsonArrayToStringList(JSONArray array) throws JSONException {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }

        return list;
    }

    private static JSONArray jsonObjectToArray(JSONObject object) throws JSONException {
        Iterator<String> keys = object.keys();
        JSONArray result = new JSONArray();

        while (keys.hasNext()) {
            String key = keys.next();
            result.put(object.getJSONObject(key));
        }

        return result;
    }

}
