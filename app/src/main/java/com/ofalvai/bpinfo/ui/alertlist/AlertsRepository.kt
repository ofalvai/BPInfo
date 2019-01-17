/*
 * Copyright 2019 Olivér Falvai
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

package com.ofalvai.bpinfo.ui.alertlist

import androidx.lifecycle.MutableLiveData
import com.android.volley.VolleyError
import com.crashlytics.android.Crashlytics
import com.ofalvai.bpinfo.api.AlertApiClient
import com.ofalvai.bpinfo.api.AlertListErrorMessage
import com.ofalvai.bpinfo.api.AlertListMessage
import com.ofalvai.bpinfo.api.AlertRequestParams
import com.ofalvai.bpinfo.model.Alert
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.json.JSONException
import org.threeten.bp.LocalDateTime
import timber.log.Timber

class AlertsRepository(
    private val alertApiClient: AlertApiClient
) {

    sealed class Error {
        class NetworkError(val volleyError: VolleyError) : Error()
        object DataError : Error()
        object GeneralError : Error()
    }

    val todayAlerts = MutableLiveData<List<Alert>>()
    val futureAlerts = MutableLiveData<List<Alert>>()

    val error = MutableLiveData<Error>()

    private val alertRequestParams: AlertRequestParams
        get() = AlertRequestParams(AlertListType.ALERTS_TODAY, getCurrentLanguageCode())
    // TODO: remove AlertListType from params

    private var lastUpdate: LocalDateTime = LocalDateTime.now()

    init {
        EventBus.getDefault().register(this)
        fetchAlerts()
    }

    private fun fetchAlerts() {
        // TODO: network detection
        alertApiClient.fetchAlertList(alertRequestParams)
    }

    @Subscribe
    fun onAlertListEvent(message: AlertListMessage) {
        lastUpdate = LocalDateTime.now()

        todayAlerts.value = message.todayAlerts
        futureAlerts.value = message.futureAlerts
    }

    @Subscribe
    fun onAlertListErrorEvent(message: AlertListErrorMessage) {
        val ex = message.exception
        Timber.e(ex.toString())
        when (ex) {
            is VolleyError -> error.value = Error.NetworkError(ex)
            is JSONException -> {
                error.value = Error.DataError
                Crashlytics.logException(ex)
            }
            else -> {
                error.value = Error.GeneralError
                Crashlytics.logException(ex)
            }
        }
    }

    // TODO: implement without context
    // TODO: return enum, not string
    private fun getCurrentLanguageCode(): String {
        return "hu"
    }
}