package com.ofalvai.bpinfo.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.RouteType

object Analytics {

    private fun alertHasNoRoutes(alert: Alert): String {
        return if (alert.affectedRoutes.isEmpty()) "true" else "false"
    }

    fun logAlertContentView(context: Context, alert: Alert?) {
        alert?.let {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, alert.id)
                putString(FirebaseAnalytics.Param.ITEM_NAME, alert.header)
                putString(FirebaseAnalytics.Param.SOURCE, alert.url)
                putString("no_routes", alertHasNoRoutes(alert))
            }
            FirebaseAnalytics.getInstance(context)
                .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        }
    }

    fun logAlertUrlClick(context: Context, alert: Alert?) {
        alert?.let {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SOURCE, alert.url)
                putString(FirebaseAnalytics.Param.ITEM_ID, alert.id)
                putString("no_routes", alertHasNoRoutes(alert))
            }
            FirebaseAnalytics.getInstance(context).logEvent("alert_url_click", bundle)
        }
    }

    fun logLanguageChange(context: Context, newValue: String) {
        val bundle = Bundle()
        bundle.putString("settings_new_language", newValue)
        FirebaseAnalytics.getInstance(context).logEvent("settings_changed_language", bundle);
    }

    fun logDebugMode(context: Context, newState: String) {
        val bundle = Bundle()
        bundle.putString("settings_new_debug_state", newState)
        FirebaseAnalytics.getInstance(context).logEvent("settings_changed_debug_mode", bundle)
    }

    fun logManualRefresh(context: Context) {
        FirebaseAnalytics.getInstance(context).logEvent("alert_list_manual_refresh", null)
    }

    fun logFilterDialogOpened(context: Context) {
        FirebaseAnalytics.getInstance(context).logEvent("alert_filter_open", null)
    }

    fun logFilterApplied(context: Context, routeTypes: Set<RouteType>) {
        val bundle = Bundle()
        bundle.putString("alert_filters", routeTypes.toString())
        FirebaseAnalytics.getInstance(context).logEvent("alert_filter_apply", bundle)
    }

    fun logNoticeDialogView(context: Context) {
        FirebaseAnalytics.getInstance(context).logEvent("notice_dialog_view", null)
    }

    fun logDataSourceChange(context: Context, selectedDataSourceLabel: String) {
        val bundle = Bundle()
        bundle.putString("settings_data_source", selectedDataSourceLabel)
        FirebaseAnalytics.getInstance(context).logEvent("settings_data_source_changed", bundle)
    }
}
