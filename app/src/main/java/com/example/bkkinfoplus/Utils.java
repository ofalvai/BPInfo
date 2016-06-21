package com.example.bkkinfoplus;

import com.example.bkkinfoplus.model.Alert;

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
    public static String capitalizeString(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static class AlertStartTimestampComparator implements Comparator<Alert> {
        @Override
        public int compare(Alert lhs, Alert rhs) {
            return Long.valueOf(lhs.getStart()).compareTo(rhs.getStart());
        }
    }

    public static boolean isRouteReplacement(String routeId) {
        String replacementIdPattern = "BKK_(VP|TP)[0-9]+";

        return routeId.matches(replacementIdPattern);
    }

    public static List<String> jsonArrayToStringList(JSONArray array) throws JSONException {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }

        return list;
    }

    public static JSONArray jsonObjectToArray(JSONObject object) throws JSONException {
        Iterator<String> keys = object.keys();
        JSONArray result = new JSONArray();

        while (keys.hasNext()) {
            String key = keys.next();
            result.put(object.getJSONObject(key));
        }

        return result;
    }
}
