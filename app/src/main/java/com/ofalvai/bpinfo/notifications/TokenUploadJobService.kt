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

import com.android.volley.VolleyError
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.ofalvai.bpinfo.api.subscription.SubscriptionClient
import com.ofalvai.bpinfo.util.Analytics
import org.koin.android.ext.android.inject
import timber.log.Timber

class TokenUploadJobService : JobService() {

    companion object {
        const val TAG = "TokenUploadJobService"
        const val KEY_NEW_TOKEN = "new_token"
        const val KEY_OLD_TOKEN = "old_token"
    }

    private val subscriptionClient: SubscriptionClient by inject()
    private val analytics: Analytics by inject()

    override fun onStartJob(job: JobParameters?): Boolean {
        val newToken: String? = job?.extras?.getString(KEY_NEW_TOKEN)
        val oldToken: String? = job?.extras?.getString(KEY_OLD_TOKEN)

        @Suppress("LiftReturnOrAssignment")
        if (newToken != null && oldToken != null) {
            subscriptionClient.replaceToken(
                oldToken,
                newToken,
                object : SubscriptionClient.TokenReplaceCallback {
                    override fun onTokenReplaceSuccess() {
                        Timber.d("New token successfully uploaded")
                        jobFinished(job, false)
                    }

                    override fun onTokenReplaceError(error: VolleyError) {
                        Timber.d("New token upload unsuccessful", error)
                        analytics.logException(error)
                        jobFinished(job, true)
                    }
                })
            return true // there is still work remaining
        } else {
            Timber.w("Not uploading invalid tokens; old: %s, new: %s", oldToken, newToken)
            return false // the job is done
        }
    }

    override fun onStopJob(job: JobParameters?) = true // should be retried later
}