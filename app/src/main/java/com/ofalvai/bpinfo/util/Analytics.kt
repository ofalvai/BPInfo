/*
 * Copyright 2018 Olivér Falvai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ofalvai.bpinfo.util

import android.content.Context
import android.os.Bundle
import android.support.v4.app.NotificationManagerCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.RouteType

object Analytics {

    const val DATA_SOURCE_BKKINFO = "bkkinfo"
    const val DATA_SOURCE_FUTAR = "futar"

    private fun alertHasNoRoutes(alert: Alert): String {
        return if (alert.affectedRoutes.isEmpty()) "true" else "false"
    }

    fun setDataSource(context: Context, dataSource: String) {
        FirebaseAnalytics.getInstance(context)
            .setUserProperty("data_source", dataSource)
    }

    /**
     * Checks if notifications are enabled or disabled for the app, and sets the result as a
     * user property.
     */
    fun setSystemNotificationState(context: Context) {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        FirebaseAnalytics.getInstance(context)
            .setUserProperty("notifications_enabled", enabled.toString())
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

    fun logNotificationSubscribe(context: Context, routeId: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, routeId)
        FirebaseAnalytics.getInstance(context).logEvent("notif_subscribe_route", bundle)
    }

    fun logNotificationUnsubscribe(context: Context, routeId: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, routeId)
        FirebaseAnalytics.getInstance(context).logEvent("notif_unsubscribe_route", bundle)
    }

    fun logNotificationOpen(context: Context, alertId: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, alertId)
        FirebaseAnalytics.getInstance(context).logEvent("notif_open", bundle)
    }

    fun logNotificationChannelsOpened(context: Context) {
        FirebaseAnalytics.getInstance(context).logEvent("notif_channels_opened", null)
    }

    fun logNotificationFromSettingsOpened(context: Context) {
        FirebaseAnalytics.getInstance(context).logEvent("notif_from_settings_opened", null)
    }

    fun logDeviceTokenUpdate(context: Context, newToken: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, newToken)
        FirebaseAnalytics.getInstance(context).logEvent("notif_token_update", bundle)
    }
}
