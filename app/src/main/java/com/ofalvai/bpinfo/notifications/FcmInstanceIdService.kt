package com.ofalvai.bpinfo.notifications

import android.content.SharedPreferences
import android.os.Bundle
import com.firebase.jobdispatcher.Constraint
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.firebase.jobdispatcher.Lifetime
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.ofalvai.bpinfo.BpInfoApplication
import timber.log.Timber
import javax.inject.Inject

class FcmInstanceIdService : FirebaseInstanceIdService() {

    companion object {
        const val PREF_KEY_TOKEN = "fcm_token"
    }

    @Inject lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        BpInfoApplication.injector.inject(this)
    }

    override fun onTokenRefresh() {
        super.onTokenRefresh()

        val newToken: String? = FirebaseInstanceId.getInstance().token
        val oldToken: String? = getOldToken()
        Timber.i("New Firebase token: $newToken")

        if (newToken == null) {
            // It should never happen
            return
        }

        if (oldToken != null) {
            Timber.i("Previous token was found, scheduling upload")
            scheduleTokenUpload(oldToken, newToken)
        }

        Timber.i("Persisting new token in SharedPreferences")
        persistNewToken(newToken)
    }

    private fun getOldToken(): String? {
        return sharedPreferences.getString(PREF_KEY_TOKEN, null)
    }

    private fun scheduleTokenUpload(oldToken: String, newToken: String) {
        val extras = Bundle().apply {
            putString(TokenUploadJobService.KEY_NEW_TOKEN, newToken)
            putString(TokenUploadJobService.KEY_OLD_TOKEN, oldToken)
        }

        val jobDispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        val job = jobDispatcher.newJobBuilder()
            .setService(TokenUploadJobService::class.java)
            .setTag(TokenUploadJobService.TAG)
            .addConstraint(Constraint.ON_ANY_NETWORK)
            .setLifetime(Lifetime.FOREVER)
            .setReplaceCurrent(false)
            .setExtras(extras)
            .build()

        jobDispatcher.mustSchedule(job)
        // TODO: ScheduleFailedException
    }

    private fun persistNewToken(newToken: String) {
        sharedPreferences.edit()
            .putString(PREF_KEY_TOKEN, newToken)
            .apply()
    }
}
