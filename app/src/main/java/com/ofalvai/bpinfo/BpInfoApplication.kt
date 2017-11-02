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

package com.ofalvai.bpinfo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.StrictMode
import android.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.ofalvai.bpinfo.injection.ApiModule
import com.ofalvai.bpinfo.injection.AppComponent
import com.ofalvai.bpinfo.injection.AppModule
import com.ofalvai.bpinfo.injection.DaggerAppComponent
import com.ofalvai.bpinfo.notifications.NOTIF_CHANNEL_ID_ALERTS
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import io.fabric.sdk.android.Fabric
import net.danlew.android.joda.JodaTimeAndroid
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class BpInfoApplication : Application(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {

        @JvmStatic
        lateinit var injector: AppComponent

        @JvmStatic
        fun getRefWatcher(context: Context): RefWatcher {
            val application = context.applicationContext as BpInfoApplication
            return application.mRefWatcher
        }
    }

    @Inject
    lateinit var mSharedPreferences: SharedPreferences

    private lateinit var mRefWatcher: RefWatcher

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        mRefWatcher = LeakCanary.install(this)

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)

        initStrictMode()

        initDagger()

        injector.inject(this) // Oh the irony...

        setLanguage()

        JodaTimeAndroid.init(this)

        Fabric.with(this, Crashlytics(), Answers())

        initTimber()

        createNotificationChannels()
    }

    /**
     * This handles orientation and other configuration changes. Without this, a screen rotation
     * would reset the language previously set in Application.onCreate() above.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        setLanguage()
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

    /**
     * Sets the application language (and locale) to the value saved in preferences.
     * If nothing is set, or set to "auto", it won't update the configuration.
     */
    private fun setLanguage() {
        val languagePreference = mSharedPreferences.getString(
                getString(R.string.pref_key_language),
                getString(R.string.pref_key_language_auto)
        )

        if (languagePreference == getString(R.string.pref_key_language_auto)) {
            // Language is "auto". This is either because the preference is missing,
            // or because it has been set to "auto"
            return
        }

        val newLocale = Locale(languagePreference)
        Locale.setDefault(newLocale)
        val config = Configuration()
        config.locale = newLocale

        resources.updateConfiguration(config, null)
    }

    private fun initDagger() {
        injector = DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .apiModule(ApiModule()) //depends on selected build flavor (prod/mock)
                .build()
    }

    private fun initStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyFlashScreen()
                    .penaltyLog()
                    .build())

            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    //.detectLeakedSqlLiteObjects()
                    //.detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    .build())
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

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val name = getString(R.string.notif_channel_alerts_title)
        val description = getString(R.string.notif_channel_alerts_desc)
        val channel = NotificationChannel(
                NOTIF_CHANNEL_ID_ALERTS,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = description
        notificationManager.createNotificationChannel(channel)
    }
}
