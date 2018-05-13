/*
 * Copyright 2016 Olivér Falvai
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

package com.ofalvai.bpinfo.ui.alertlist

import android.content.Context
import android.content.SharedPreferences
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.crashlytics.android.Crashlytics
import com.ofalvai.bpinfo.BpInfoApplication
import com.ofalvai.bpinfo.Config
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.api.AlertApiClient
import com.ofalvai.bpinfo.api.AlertListErrorMessage
import com.ofalvai.bpinfo.api.AlertListMessage
import com.ofalvai.bpinfo.api.AlertRequestParams
import com.ofalvai.bpinfo.api.bkkfutar.AlertSearchContract
import com.ofalvai.bpinfo.api.notice.NoticeClient
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.base.BasePresenter
import com.ofalvai.bpinfo.util.alertStartComparator
import com.ofalvai.bpinfo.util.hasNetworkConnection
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.json.JSONException
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class AlertListPresenter(private val alertListType: AlertListType)
    : BasePresenter<AlertListContract.View>(), NoticeClient.NoticeListener,
        AlertListContract.Presenter {

    @Inject
    lateinit var alertApiClient: AlertApiClient

    @Inject
    lateinit var noticeClient: NoticeClient

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var context: Context

    /**
     * List of alerts returned by the client, before filtering by RouteTypes
     */
    private var unfilteredAlerts: MutableList<Alert>? = null

    private var lastUpdate: LocalDateTime = LocalDateTime.now()

    private var activeFilter: MutableSet<RouteType> = mutableSetOf()

    private val alertRequestParams: AlertRequestParams
        get() = AlertRequestParams(alertListType, getCurrentLanguageCode())

    init {
        BpInfoApplication.injector.inject(this)
    }

    override fun attachView(view: AlertListContract.View) {
        super.attachView(view)
        EventBus.getDefault().register(this)
    }

    override fun detachView() {
        super.detachView()
        EventBus.getDefault().unregister(this)
    }

    /**
     * Initiates a network refresh if possible, and returns the alert list to the listener, or
     * calls the appropriate callback.
     */
    override fun fetchAlertList() {
        when {
            context.hasNetworkConnection() -> alertApiClient.fetchAlertList(alertRequestParams)
            unfilteredAlerts == null -> {
                // Nothing was displayed previously, showing a full error view
                view?.displayNetworkError(NoConnectionError())
            }
            else -> {
                // A list was loaded previously, we don't clear that, only display a warning.
                view?.displayNoNetworkWarning()
            }
        }
    }

    /**
     * If possible, returns the local, filtered state of the alert list to the listener,
     * otherwise calls fetchAlertList() to get data from the API.
     */
    override fun getAlertList() {
        if (unfilteredAlerts != null) {
            val processedAlerts = filterAndSort(activeFilter, unfilteredAlerts!!, alertListType)
            view?.displayAlerts(processedAlerts)
        } else {
            fetchAlertList()
        }
    }

    override fun fetchAlert(alertId: String) {
        alertApiClient.fetchAlert(
                alertId,
                object : AlertApiClient.AlertDetailListener {
                    override fun onAlertResponse(alert: Alert) {
                        view?.updateAlertDetail(alert)
                    }

                    override fun onError(ex: Exception) {
                        view?.displayAlertDetailError()
                        Timber.e(ex)
                        Crashlytics.logException(ex)
                    }
                },
                alertRequestParams
        )
    }

    override fun setLastUpdate() {
        lastUpdate = LocalDateTime.now()
    }

    /**
     * Initiates a list update if enough time has passed since the last update
     */
    override fun updateIfNeeded(): Boolean {
        val refreshThreshold = Duration.ofSeconds(Config.Behavior.REFRESH_THRESHOLD_SEC.toLong())
        @Suppress("LiftReturnOrAssignment")
        if (lastUpdate.plus(refreshThreshold).isBefore(LocalDateTime.now())) {
            fetchAlertList()
            fetchNotice()
            return true
        } else {
            return false
        }
    }

    /**
     * Sets the RouteType filter to be applied to the returned alert list.
     * If an empty Set or null is passed, the list is not filtered.
     */
    override fun setFilter(routeTypes: MutableSet<RouteType>?) {
        if (routeTypes == null) {
            activeFilter.clear()
        } else {
            activeFilter = routeTypes
        }
    }

    override fun getFilter(): MutableSet<RouteType>? {
        return activeFilter
    }

    /**
     * Transforms the list of returned alerts in the following order:
     * 1. Sort the list by the alerts' start time
     * 2. Filter the list by the currently active filter
     */
    @Subscribe
    fun onAlertListEvent(message: AlertListMessage) {
        if (alertListType == AlertListType.ALERTS_TODAY) {
            unfilteredAlerts = message.todayAlerts.toMutableList()
        } else if (alertListType == AlertListType.ALERTS_FUTURE) {
            unfilteredAlerts = message.futureAlerts.toMutableList()
        }

        val processedAlerts = filterAndSort(activeFilter, unfilteredAlerts!!, alertListType)
        view?.displayAlerts(processedAlerts)
    }

    @Subscribe
    fun onAlertListErrorEvent(message: AlertListErrorMessage) {
        unfilteredAlerts?.clear()

        val ex = message.exception
        Timber.e(ex.toString())
        when (ex) {
            is VolleyError -> view?.displayNetworkError(ex)
            is JSONException -> {
                view?.displayDataError()
                Crashlytics.logException(ex)
            }
            else -> {
                view?.displayGeneralError()
                Crashlytics.logException(ex)
            }
        }
    }

    /**
     * Returns a new filtered list of Alerts matching the provided set of RouteTypes, and sorted
     * according to the alert list type (descending/ascending)
     */
    // TODO: possible rewrite with Kotlin collection methods
    private fun filterAndSort(types: Set<RouteType>?,
                              alerts: List<Alert>,
                              type: AlertListType): List<Alert> {
        val sorted = ArrayList(alerts)

        // Sort: descending by alert start time
        Collections.sort(sorted, alertStartComparator)
        if (type == AlertListType.ALERTS_TODAY) {
            sorted.reverse()
        }

        if (types == null || types.isEmpty()) {
            return sorted
        }

        val filtered = ArrayList<Alert>()

        for (alert in sorted) {
            for ((_, _, _, _, routeType) in alert.affectedRoutes) {
                if (types.contains(routeType)) {
                    filtered.add(alert)
                    break
                }
            }
        }

        return filtered
    }

    override fun fetchNotice() {
        // We only need to display one dialog per activity
        if (alertListType == AlertListType.ALERTS_TODAY) {
            noticeClient.fetchNotice(this, getCurrentLanguageCode())
        }
    }

    override fun onNoticeResponse(noticeBody: String) {
        view?.displayNotice(noticeBody)
    }

    override fun onNoNotice() {
        view?.removeNotice()
    }

    /**
     * Gets the current language's language code.
     * If a language has been set in the preferences, it reads the value from SharedPreferences.
     * If it has been set to "auto" or unset, it decides based on the current locale, using "en" for
     * any other language than Hungarian ("hu")
     * @return The app's current language's code.
     */
    private fun getCurrentLanguageCode(): String {
        var languageCode = sharedPreferences.getString(
                context.getString(R.string.pref_key_language),
                context.getString(R.string.pref_key_language_auto)
        )

        if (languageCode == context.getString(R.string.pref_key_language_auto)) {
            languageCode = if (Locale.getDefault().language == AlertSearchContract.LANG_HU) {
                AlertSearchContract.LANG_HU
            } else {
                AlertSearchContract.LANG_EN
            }
        }

        return languageCode
    }
}
