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

package com.ofalvai.bpinfo;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.ofalvai.bpinfo.injection.ApiModule;
import com.ofalvai.bpinfo.injection.AppComponent;
import com.ofalvai.bpinfo.injection.AppModule;
import com.ofalvai.bpinfo.injection.DaggerAppComponent;
import com.ofalvai.bpinfo.notifications.AlertMessagingServiceKt;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.Locale;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;


public class BpInfoApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static AppComponent injector;

    @Inject SharedPreferences mSharedPreferences;

    private RefWatcher mRefWatcher;

    private void initDagger() {
        injector = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .apiModule(new ApiModule()) //depends on selected build flavor (prod/mock)
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        mRefWatcher = LeakCanary.install(this);

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        initStrictMode();

        initDagger();

        injector.inject(this); // Oh the irony...

        setLanguage();

        JodaTimeAndroid.init(this);

        Fabric.with(this, new Crashlytics(), new Answers());

        initTimber();

        createNotificationChannels();
    }

    /**
     * This handles orientation and other configuration changes. Without this, a screen rotation
     * would reset the language previously set in Application.onCreate() above.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setLanguage();
    }

    /**
     * Listening for data source preference change to rebuild the Dagger object graph in order to
     * provide a new AlertApiClient. Recreating the Activity (and Presenters with injected fields)
     * is done in another listener in SettingsActivity.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String dataSourceKey = getString(R.string.pref_key_data_source);
        if (key.equals(dataSourceKey)) {
            initDagger();
        }
    }

    public static RefWatcher getRefWatcher(Context context) {
        BpInfoApplication application = (BpInfoApplication) context.getApplicationContext();
        return application.mRefWatcher;
    }

    /**
     * Sets the application language (and locale) to the value saved in preferences.
     * If nothing is set, or set to "auto", it won't update the configuration.
     */
    private void setLanguage() {
        String languagePreference = mSharedPreferences.getString(
                getString(R.string.pref_key_language),
                getString(R.string.pref_key_language_auto)
        );

        if (languagePreference.equals(getString(R.string.pref_key_language_auto))) {
            // Language is "auto". This is either because the preference is missing,
            // or because it has been set to "auto"
            return;
        }

        Locale newLocale = new Locale(languagePreference);
        Locale.setDefault(newLocale);
        Configuration config = new Configuration();
        config.locale = newLocale;

        getResources().updateConfiguration(config, null);
    }

    private void initStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyFlashScreen()
                    .penaltyLog()
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    //.detectLeakedSqlLiteObjects()
                    //.detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    .build());
        }
    }

    private void initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        CharSequence name = getString(R.string.notif_channel_alerts_title);
        String description = getString(R.string.notif_channel_alerts_desc);
        NotificationChannel channel = new NotificationChannel(
                AlertMessagingServiceKt.NOTIF_CHANNEL_ID_ALERTS,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }
}
