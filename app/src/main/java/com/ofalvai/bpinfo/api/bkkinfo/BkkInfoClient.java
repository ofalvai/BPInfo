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

import android.net.Uri;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.ofalvai.bpinfo.api.AlertApiClient;
import com.ofalvai.bpinfo.api.AlertRequestParams;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.Route;
import com.ofalvai.bpinfo.model.RouteType;
import com.ofalvai.bpinfo.ui.alertlist.AlertListType;
import com.ofalvai.bpinfo.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.ofalvai.bpinfo.util.LogUtils.LOGI;

public class BkkInfoClient implements AlertApiClient {
    private static final String TAG = "BkkInfoClient";

    private static final String BKKINFO_API_BASE_URL = "http://bkk.hu/apps/bkkinfo/";

    private static final String BKKINFO_API_ENDPOINT_HU = "json.php";

    private static final String BKKINFO_API_ENDPOINT_EN = "json_en.php";

    private static final String PARAM_ALERT_LIST = "?lista";

    private static final String PARAM_ALERT_DETAIL = "id";

    private static final String BKKINFO_DETAIL_WEBVIEW_URL = "http://bkk.hu/apps/bkkinfo/iframe.php"; //TODO: not responsive

    private RequestQueue mRequestQueue;

    private List<Alert> mAlertsToday = new ArrayList<>();

    private List<Alert> mAlertsFuture = new ArrayList<>();

    public BkkInfoClient(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;
    }

    @Override
    public void fetchAlertList(final @NonNull AlertListListener listener, final @NonNull AlertRequestParams params) {
        final Uri url = buildUrl(params);

        LOGI(TAG, "API request: " + url.toString());

        JsonObjectRequest request = new JsonObjectRequest(
                url.toString(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            List<Alert> alerts = parseAlerts(response);
                            // TODO: temporary if-else until refactoring both ApiClients
                            if (params.mAlertListType.equals(AlertListType.ALERTS_TODAY)) {
                                mAlertsToday = alerts;
                            } else {
                                mAlertsFuture = alerts;
                            }
                            listener.onAlertListResponse(alerts);
                        } catch (Exception ex) {
                            listener.onError(ex);
                        }
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

    @Override
    public void fetchAlert(@NonNull String id, @NonNull AlertListener listener,
                           @NonNull AlertRequestParams params) {
        if (mAlertsToday == null) {
            throw new RuntimeException("fetchAlert() was called before fetchAlertList()");
            // TODO: this might be a problem when recreating only the AlertDetailFragment
        } else {
            for (Alert alert : mAlertsToday) {
                if (alert.getId().equals(id)) {
                    listener.onAlertResponse(alert);
                    return;
                }
            }
            listener.onError(new Exception("Alert not found"));
        }
    }

    private Uri buildUrl(AlertRequestParams params) {
        String language = params.mLanguageCode.equals("hu") ? BKKINFO_API_ENDPOINT_HU :
                BKKINFO_API_ENDPOINT_EN;

        return Uri.parse(BKKINFO_API_BASE_URL).buildUpon()
                .appendEncodedPath(language)
                .appendEncodedPath(PARAM_ALERT_LIST)
                .build();
    }

    @NonNull
    private List<Alert> parseAlerts(JSONObject response) throws JSONException {
        List<Alert> alerts = new ArrayList<>();

        JSONArray activeAlertsList = response.getJSONArray("active");
        for (int i = 0; i < activeAlertsList.length(); i++) {
            JSONObject alertNode = activeAlertsList.getJSONObject(i);
            alerts.add(parseAlert(alertNode));
        }

        return alerts;
    }

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

        Alert alert =  new Alert(id, start, end, timestamp, null, null, url, header, description);
        alert.setAffectedRoutes(affectedRoutes);
        return alert;
    }

    private String getUrl(String alertId) {
        return BKKINFO_DETAIL_WEBVIEW_URL + "?id=" + alertId;
    }

    @NonNull
    private List<Route> parseAffectedRoutes(JSONArray routesArray) throws JSONException {
        // The API lists multiple affected routes grouped by their type
        List<Route> routes = new ArrayList<>();
        for (int i = 0; i < routesArray.length(); i++) {
            JSONObject routeNode = routesArray.getJSONObject(i);
            String typeString = routeNode.getString("type");
            RouteType type = parseRouteType(typeString);

            JSONArray concreteRoutes = routeNode.getJSONArray("jaratok");
            for (int j = 0; j < concreteRoutes.length(); j++) {
                String shortName = concreteRoutes.getString(j);
                String[] colors = parseRouteColors(type);

                Route route = new Route(null, shortName, null, null, type, null, colors[0], colors[1]);
                routes.add(route);
            }
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
                return RouteType._OTHER_; //TODO
            default:
                return RouteType._OTHER_;
        }
        //TODO: missing: RAIL (HÉV)
    }

    /**
     * Returns the background and foreground colors of the route, because this API doesn't return
     * them in the response.
     * @param type
     * @return  Array of colors: background, foreground
     */
    private String[] parseRouteColors(RouteType type) {
        String defaultBackground = "EEEEEE";
        String defaultText = "BBBBBB";

        String background;
        String text;
        switch (type) {
            case BUS:
                background = "009FE3";
                text = "FFFFFF";
                break;
            case FERRY:
                background = "D60080";
                text = "FFFFFF";
                break;
            case RAIL:
                // TODO: color depends on the route name
                background = defaultBackground;
                text = defaultText;
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
                // TODO: color depends on the route name
                background = defaultBackground;
                text = defaultText;
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

        return new String[] { background, text };
    }
}
