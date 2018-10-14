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
import android.os.StrictMode
import android.preference.PreferenceManager
import com.jakewharton.threetenabp.AndroidThreeTen
import com.ofalvai.bpinfo.injection.ApiModule
import com.ofalvai.bpinfo.injection.AppComponent
import com.ofalvai.bpinfo.injection.AppModule
import com.ofalvai.bpinfo.injection.DaggerAppComponent
import com.ofalvai.bpinfo.util.LocaleManager
import com.squareup.leakcanary.LeakCanary
import timber.log.Timber
import javax.inject.Inject

class BpInfoApplication : Application(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {

        @JvmStatic
        lateinit var injector: AppComponent
    }

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)

        initStrictMode()

        initDagger()
        injector.inject(this) // Oh the irony...

        AndroidThreeTen.init(this)

        initTimber()

        createNotificationChannels()

        overrideSettingsIfNeeded()
    }

    override fun attachBaseContext(base: Context) {
        // Updating locale
        super.attachBaseContext(LocaleManager.setLocale(base))
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
            initDagger()
        }
    }

    private fun initDagger() {
        injector = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .apiModule(ApiModule()) //depends on selected build flavor (prod/mock)
            .build()
    }

    private fun initStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyFlashScreen()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    //.detectLeakedSqlLiteObjects()
                    //.detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    .build()
            )
        }
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

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val id = getString(R.string.notif_channel_alerts_id)
        val name = getString(R.string.notif_channel_alerts_title)
        val description = getString(R.string.notif_channel_alerts_desc)
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = description
        notificationManager.createNotificationChannel(channel)
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
