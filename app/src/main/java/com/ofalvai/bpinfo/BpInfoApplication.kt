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

package com.ofalvai.bpinfo

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.preference.PreferenceManager
import androidx.core.content.getSystemService
import androidx.multidex.MultiDex
import com.jakewharton.threetenabp.AndroidThreeTen
import com.ofalvai.bpinfo.api.subscription.SubscriptionClient
import com.ofalvai.bpinfo.injection.allModules
import com.ofalvai.bpinfo.notifications.TokenUploadWorker
import com.ofalvai.bpinfo.util.Analytics
import com.ofalvai.bpinfo.util.LocaleManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.startKoin
import org.koin.android.logger.AndroidLogger
import org.koin.log.EmptyLogger
import org.koin.standalone.StandAloneContext
import timber.log.Timber

class BpInfoApplication :
    Application(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    androidx.work.Configuration.Provider {

    private val sharedPreferences: SharedPreferences by inject()
    private val analytics: Analytics by inject()
    private val subscriptionClient: SubscriptionClient by inject()

    override fun onCreate() {
        super.onCreate()

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)

        initKoin()

        AndroidThreeTen.init(this)

        initTimber()

        createNotificationChannels()

        overrideSettingsIfNeeded()

        analytics.setSystemNotificationState()
        analytics.setRestrictions()
    }

    override fun attachBaseContext(base: Context) {
        val newBaseContext = LocaleManager.setLocale(base)
        super.attachBaseContext(newBaseContext)

        if (BuildConfig.DEBUG) {
            MultiDex.install(this)
        }
    }

    /**
     * Without this, a screen rotation would reset the language previously set
     * in Application.onCreate() above.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        LocaleManager.setLocale(this)
    }

    /**
     * Listening for data source preference change to rebuild the Dagger object graph in order to
     * provide a new AlertApiClient. Recreating the Activity (and Presenters with injected fields)
     * is done in another listener in SettingsActivity.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val dataSourceKey = getString(R.string.pref_key_data_source)
        if (key == dataSourceKey) {
            StandAloneContext.stopKoin()
            initKoin()
        }
    }

    override fun getWorkManagerConfiguration() = androidx.work.Configuration.Builder()
        .setWorkerFactory(TokenUploadWorker.Factory(subscriptionClient, analytics))
        .build()

    private fun initKoin() {
        val logger = if (BuildConfig.DEBUG) AndroidLogger() else EmptyLogger()
        startKoin(this, allModules, logger = logger)
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = getSystemService<NotificationManager>()

        val id = getString(R.string.notif_channel_alerts_id)
        val name = getString(R.string.notif_channel_alerts_title)
        val description = getString(R.string.notif_channel_alerts_desc)
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = description
        notificationManager?.createNotificationChannel(channel)
    }

    @SuppressLint("ApplySharedPref")
    private fun overrideSettingsIfNeeded() {
        val lastOverrideVersionCode = sharedPreferences.getInt(
            getString(R.string.pref_key_data_source_override_version_code),
            0
        )

        if (BuildConfig.VERSION_CODE > lastOverrideVersionCode) {
            // Override current user preference with the current default value,
            // then store the version code of this version
            sharedPreferences.edit()
                .putString(
                    getString(R.string.pref_key_data_source),
                    getString(R.string.pref_key_data_source_default)
                )
                .putInt(
                    getString(R.string.pref_key_data_source_override_version_code),
                    BuildConfig.VERSION_CODE
                )
                .commit()
        }
    }
}
