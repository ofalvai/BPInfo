package com.ofalvai.bpinfo.notifications

import com.android.volley.VolleyError
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.ofalvai.bpinfo.BpInfoApplication
import com.ofalvai.bpinfo.api.subscription.SubscriptionClient
import timber.log.Timber
import javax.inject.Inject

class TokenUploadJobService : JobService() {

    companion object {
        const val TAG = "TokenUploadJobService"
        const val KEY_NEW_TOKEN = "new_token"
        const val KEY_OLD_TOKEN = "old_token"
    }

    @Inject
    lateinit var subscriptionClient: SubscriptionClient

    override fun onCreate() {
        super.onCreate()
        BpInfoApplication.injector.inject(this)
    }

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
                        jobFinished(job, false)
                    }

                    override fun onTokenReplaceError(error: VolleyError) {
                        jobFinished(job, true)
                    }
                })
            return true // there is still work remaining
        } else {
            Timber.w("Not uploading invalid tokens; old: %s, new: %s", oldToken, newToken)
            return false // the job is done
        }
    }

    override fun onStopJob(job: JobParameters?): Boolean {
        // TODO: cancel request
        return true // should be retried
    }
}