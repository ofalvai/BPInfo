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

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.RouteType

class Analytics(private val context: Context) {

    companion object {
        const val DATA_SOURCE_BKKINFO = "bkkinfo"
        const val DATA_SOURCE_FUTAR = "futar"
    }

    fun setDataSource(dataSource: String) {
        FirebaseAnalytics.getInstance(context)
            .setUserProperty("data_source", dataSource)
    }

    /**
     * Checks if notifications are enabled or disabled for the app, and sets the result as a
     * user property.
     */
    fun setSystemNotificationState() {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        FirebaseAnalytics.getInstance(context)
            .setUserProperty("notifications_enabled", enabled.toString())
    }

    fun setRestrictions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

        val activityManager = context
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val isBackgroundRestricted: String = activityManager.isBackgroundRestricted.toString()
        FirebaseAnalytics.getInstance(context)
            .setUserProperty("background_restricted", isBackgroundRestricted)

        val usageStatsManager = context
            .getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        FirebaseAnalytics.getInstance(context)
            .setUserProperty("app_standby_bucket", usageStatsManager.appStandbyBucket.toString())
    }

    fun logAlertContentView(alert: Alert?) {
        alert?.let {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, alert.id)
                putString(FirebaseAnalytics.Param.ITEM_NAME, alert.header)
            }
            FirebaseAnalytics.getInstance(context)
                .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        }
    }

    fun logAlertUrlClick(alert: Alert?) {
        alert?.let {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, alert.id)
            }
            FirebaseAnalytics.getInstance(context).logEvent("alert_url_click", bundle)
        }
    }

    fun logLanguageChange(newValue: String) {
        val bundle = Bundle()
        bundle.putString("settings_new_language", newValue)
        FirebaseAnalytics.getInstance(context).logEvent("settings_changed_language", bundle)
    }

    fun logDebugMode(newState: String) {
        val bundle = Bundle()
        bundle.putString("settings_new_debug_state", newState)
        FirebaseAnalytics.getInstance(context).logEvent("settings_changed_debug_mode", bundle)
    }

    fun logManualRefresh() {
        FirebaseAnalytics.getInstance(context).logEvent("alert_list_manual_refresh", null)
    }

    fun logFilterDialogOpened() {
        FirebaseAnalytics.getInstance(context).logEvent("alert_filter_open", null)
    }

    fun logFilterApplied(routeTypes: Set<RouteType>) {
        val bundle = Bundle()
        bundle.putString("alert_filters", routeTypes.toString())
        FirebaseAnalytics.getInstance(context).logEvent("alert_filter_apply", bundle)
    }

    fun logNoticeDialogView() {
        FirebaseAnalytics.getInstance(context).logEvent("notice_dialog_view", null)
    }

    fun logDataSourceChange() {
        val bundle = Bundle()
        FirebaseAnalytics.getInstance(context).logEvent("settings_data_source_changed", bundle)
    }

    fun logNotificationSubscribe(routeId: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, routeId)
        FirebaseAnalytics.getInstance(context).logEvent("notif_subscribe_route", bundle)
    }

    fun logNotificationUnsubscribe(routeId: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, routeId)
        FirebaseAnalytics.getInstance(context).logEvent("notif_unsubscribe_route", bundle)
    }

    fun logNotificationOpen(alertId: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, alertId)
        FirebaseAnalytics.getInstance(context).logEvent("notif_open", bundle)
    }

    fun logNotificationChannelsOpened() {
        FirebaseAnalytics.getInstance(context).logEvent("notif_channels_opened", null)
    }

    fun logNotificationFromSettingsOpened() {
        FirebaseAnalytics.getInstance(context).logEvent("notif_from_settings_opened", null)
    }

    fun logDeviceTokenUpdate(newToken: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, newToken)
        FirebaseAnalytics.getInstance(context).logEvent("notif_token_update", bundle)
    }
}
