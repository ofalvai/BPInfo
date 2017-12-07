package com.ofalvai.bpinfo.util

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.CustomEvent
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.RouteType

object FabricUtils {

    private fun alertHasNoRoutes(alert: Alert): String {
        return if (alert.affectedRoutes.isEmpty()) "true" else "false"
    }

    fun logAlertContentView(alert: Alert?) {
        alert?.let {
            Answers.getInstance().logContentView(ContentViewEvent()
                    .putContentName(alert.header)
                    .putContentId(alert.id)
                    .putCustomAttribute("Alert URL", alert.url)
                    .putCustomAttribute("No routes", alertHasNoRoutes(alert))
            )
        }
    }

    fun logAlertUrlClick(alert: Alert?) {
        alert?.let {
            Answers.getInstance().logCustom(CustomEvent("Alert URL click")
                    .putCustomAttribute("Alert URL", alert.url)
                    .putCustomAttribute("Alert ID", alert.id)
                    .putCustomAttribute("No routes", alertHasNoRoutes(alert))
            )
        }
    }

    fun logLanguageChange(newValue: String) {
        Answers.getInstance().logCustom(CustomEvent("Changed languge")
                .putCustomAttribute("New language", newValue)
        )
    }

    fun logDebugMode(newState: String) {
        Answers.getInstance().logCustom(CustomEvent("Debug mode changed")
                .putCustomAttribute("New state", newState)
        )
    }

    fun logManualRefresh() {
        Answers.getInstance().logCustom(CustomEvent("Manual refresh"))
    }

    fun logFilterDialogOpened() {
        Answers.getInstance().logCustom(CustomEvent("Filter dialog opened"))
    }

    fun logFilterApplied(routeTypes: Set<RouteType>) {
        Answers.getInstance().logCustom(CustomEvent("Vehicle filter applied")
                .putCustomAttribute("Filter bus", if (routeTypes.contains(RouteType.BUS)) "true" else "false")
                .putCustomAttribute("Filter ferry", if (routeTypes.contains(RouteType.FERRY)) "true" else "false")
                .putCustomAttribute("Filter rail", if (routeTypes.contains(RouteType.RAIL)) "true" else "false")
                .putCustomAttribute("Filter tram", if (routeTypes.contains(RouteType.TRAM)) "true" else "false")
                .putCustomAttribute("Filter trolleybus", if (routeTypes.contains(RouteType.TROLLEYBUS)) "true" else "false")
                .putCustomAttribute("Filter subway", if (routeTypes.contains(RouteType.SUBWAY)) "true" else "false")
                .putCustomAttribute("Filter other", if (routeTypes.contains(RouteType._OTHER_)) "true" else "false")
        )
    }

    fun logNoticeDialogView() {
        Answers.getInstance().logCustom(CustomEvent("Notice dialog view"))
    }

    fun logDataSourceChange(selectedDataSourceLabel: String) {
        Answers.getInstance().logCustom(CustomEvent("Data source changed")
                .putCustomAttribute("New data source", selectedDataSourceLabel))
    }
}
