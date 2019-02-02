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
import com.ofalvai.bpinfo.Config
import com.ofalvai.bpinfo.api.AlertApiClient
import com.ofalvai.bpinfo.model.Alert
import org.json.JSONException
import org.threeten.bp.Duration
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

    private var lastUpdate: LocalDateTime? = null
    private val refreshThreshold = Duration.ofSeconds(Config.Behavior.REFRESH_THRESHOLD_SEC.toLong())

    init {
        fetchAlerts()
    }

    fun fetchAlerts() {
        if (shouldUpdate().not()) return

        // TODO: network detection
        // TODO: refactor this listener
        alertApiClient.fetchAlertList(object : AlertApiClient.AlertListListener {
            override fun onAlertListResponse(todayAlerts: List<Alert>, futureAlerts: List<Alert>) {
                lastUpdate = LocalDateTime.now()

                this@AlertsRepository.todayAlerts.value = todayAlerts
                this@AlertsRepository.futureAlerts.value = futureAlerts
            }

            override fun onError(ex: Exception) {
                Timber.e(ex.toString())
                when (ex) {
                    is VolleyError -> this@AlertsRepository.error.value = Error.NetworkError(ex)
                    is JSONException -> {
                        this@AlertsRepository.error.value = Error.DataError
                        Crashlytics.logException(ex)
                    }
                    else -> {
                        this@AlertsRepository.error.value = Error.GeneralError
                        Crashlytics.logException(ex)
                    }
                }
            }
        })
    }

    private fun shouldUpdate(): Boolean {
        // TODO: rewrite to UNIX timestamps
        // Something like https://github.com/googlesamples/android-architecture-components/blob/master/GithubBrowserSample/app/src/main/java/com/android/example/github/util/RateLimiter.kt
        return if (lastUpdate == null) {
            true
        } else {
            LocalDateTime.now().isAfter(lastUpdate?.plus(refreshThreshold))
        }
    }
}