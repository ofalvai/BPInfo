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
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.android.volley.VolleyError
import com.google.common.util.concurrent.ListenableFuture
import com.ofalvai.bpinfo.api.subscription.SubscriptionClient
import com.ofalvai.bpinfo.util.Analytics
import timber.log.Timber

class TokenUploadWorker(
    appContext: Context,
    private val params: WorkerParameters,
    private val subscriptionClient: SubscriptionClient,
    private val analytics: Analytics
) : ListenableWorker(appContext, params) {

    companion object {
        const val TAG = "TokenUploadWorker"
        const val KEY_NEW_TOKEN = "new_token"
        const val KEY_OLD_TOKEN = "old_token"
    }

    class Factory(
        private val subscriptionClient: SubscriptionClient,
        private val analytics: Analytics
    ) : WorkerFactory() {

        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return if (workerClassName == TokenUploadWorker::class.java.name) {
                TokenUploadWorker(appContext, workerParameters, subscriptionClient, analytics)
            } else {
                null
            }
        }
    }

    override fun startWork(): ListenableFuture<Result> {
        val newToken: String? = params.inputData.getString(KEY_NEW_TOKEN)
        val oldToken: String? = params.inputData.getString(KEY_OLD_TOKEN)

        return CallbackToFutureAdapter.getFuture { completer ->
            if (newToken != null && oldToken != null) {
                subscriptionClient.replaceToken(
                    oldToken,
                    newToken,
                    object : SubscriptionClient.TokenReplaceCallback {
                        override fun onTokenReplaceSuccess() {
                            Timber.d("New token successfully uploaded")
                            completer.set(Result.success())
                        }

                        override fun onTokenReplaceError(error: VolleyError) {
                            completer.set(Result.failure())
                            Timber.d(error, "New token upload unsuccessful")
                            analytics.logException(error)
                        }
                    })
            } else {
                Timber.w("Not uploading invalid tokens; old: %s, new: %s", oldToken, newToken)
                completer.set(Result.failure())
            }
        }
    }
}
