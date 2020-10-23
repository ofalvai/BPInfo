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

package com.ofalvai.bpinfo.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ofalvai.bpinfo.util.Analytics
import com.ofalvai.bpinfo.util.LocaleManager
import org.koin.android.ext.android.inject
import timber.log.Timber

class AlertMessagingService : FirebaseMessagingService() {

    companion object {
        const val PREF_KEY_TOKEN = "fcm_token"
        private const val DATA_KEY_ID = "id"
        private const val DATA_KEY_TITLE = "title"
    }

    private val  sharedPreferences: SharedPreferences by inject()

    private val analytics: Analytics by inject()

    override fun attachBaseContext(base: Context) {
        // Updating locale
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("Message from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            if (data.containsKey(DATA_KEY_TITLE) && data.containsKey(DATA_KEY_ID)) {
                val id = data[DATA_KEY_ID]
                val title = data[DATA_KEY_TITLE]
                val text = DescriptionMaker.makeDescription(data, baseContext)
                if (title != null && id != null) {
                    Timber.d("Creating notification")
                    Timber.d("ID: $id")
                    Timber.d("Title: $title")
                    Timber.d("Text: $text")

                    NotificationMaker.make(this, id, title, text)
                } else {
                    Timber.e("Message data is null")
                }
            } else {
                Timber.e("Message data is invalid")
            }
        } else {
            Timber.e("Message data is empty")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val oldToken: String? = getOldToken()
        Timber.i("New Firebase token: $token")
        analytics.logDeviceTokenUpdate(token)

        if (oldToken != null) {
            Timber.i("Previous token was found, scheduling upload")
            scheduleTokenUpload(oldToken, token)
        }

        Timber.i("Persisting new token in SharedPreferences")
        persistNewToken(token)
    }

    private fun getOldToken(): String? {
        return sharedPreferences.getString(PREF_KEY_TOKEN, null)
    }

    private fun scheduleTokenUpload(oldToken: String, newToken: String) {
        val inputData = workDataOf(
            TokenUploadWorker.KEY_NEW_TOKEN to newToken,
            TokenUploadWorker.KEY_OLD_TOKEN to oldToken
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<TokenUploadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            TokenUploadWorker.TAG,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun persistNewToken(newToken: String) {
        sharedPreferences.edit()
            .putString(PREF_KEY_TOKEN, newToken)
            .apply()
    }
}
