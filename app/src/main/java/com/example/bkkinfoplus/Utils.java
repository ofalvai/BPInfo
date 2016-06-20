package com.example.bkkinfoplus;

import java.util.Comparator;
import java.util.regex.Pattern;

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
}
