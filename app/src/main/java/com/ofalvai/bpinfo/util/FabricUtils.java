package com.ofalvai.bpinfo.util;


import android.support.annotation.Nullable;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.RouteType;

import java.util.Set;

public class FabricUtils {

    private static String alertHasNoRoutes(Alert alert) {
        return alert.getAffectedRoutes().isEmpty() ? "true" : "false";
    }

    public static void logAlertContentView(@Nullable Alert alert) {
        if (alert != null) {
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName(alert.getHeader())
                    .putContentId(alert.getId())
                    .putCustomAttribute("Alert URL", alert.getUrl())
                    .putCustomAttribute("No routes", alertHasNoRoutes(alert))
            );
        }
    }

    public static void logAlertUrlClick(@Nullable Alert alert) {
        if (alert != null) {
            Answers.getInstance().logCustom(new CustomEvent("Alert URL click")
                    .putCustomAttribute("Alert URL", alert.getUrl())
                    .putCustomAttribute("Alert ID", alert.getId())
                    .putCustomAttribute("No routes", alertHasNoRoutes(alert))
            );
        }
    }

    public static void logLanguageChange(String newValue) {
        Answers.getInstance().logCustom(new CustomEvent("Changed languge")
            .putCustomAttribute("New language", newValue)
        );
    }

    public static void logDebugMode(String newState) {
        Answers.getInstance().logCustom(new CustomEvent("Debug mode changed")
                .putCustomAttribute("New state", newState)
        );
    }

    public static void logManualRefresh() {
        Answers.getInstance().logCustom(new CustomEvent("Manual refresh"));
    }

    public static void logFilterDialogOpened() {
        Answers.getInstance().logCustom(new CustomEvent("Filter dialog opened"));
    }

    public static void logFilterApplied(Set<RouteType> routeTypes) {
        Answers.getInstance().logCustom(new CustomEvent("Vehicle filter applied")
                .putCustomAttribute("Filter bus", routeTypes.contains(RouteType.BUS) ? "true" : "false")
                .putCustomAttribute("Filter ferry", routeTypes.contains(RouteType.FERRY) ? "true" : "false")
                .putCustomAttribute("Filter rail", routeTypes.contains(RouteType.RAIL) ? "true" : "false")
                .putCustomAttribute("Filter tram", routeTypes.contains(RouteType.TRAM) ? "true" : "false")
                .putCustomAttribute("Filter trolleybus", routeTypes.contains(RouteType.TROLLEYBUS) ? "true" : "false")
                .putCustomAttribute("Filter subway", routeTypes.contains(RouteType.SUBWAY) ? "true" : "false")
                .putCustomAttribute("Filter other", routeTypes.contains(RouteType._OTHER_) ? "true" : "false")
        );
    }

    public static void logNoticeDialogView() {
        Answers.getInstance().logCustom(new CustomEvent("Notice dialog view"));
    }

    public static void logDataSourceChange(String selectedDataSourceLabel) {
        Answers.getInstance().logCustom(new CustomEvent("Data source changed")
            .putCustomAttribute("New data source", selectedDataSourceLabel));
    }
}
