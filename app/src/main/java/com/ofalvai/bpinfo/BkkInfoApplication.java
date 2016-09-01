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
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.instabug.library.Feature;
import com.instabug.library.IBGInvocationEvent;
import com.instabug.library.Instabug;
import com.ofalvai.bpinfo.injection.ApiModule;
import com.ofalvai.bpinfo.injection.AppComponent;
import com.ofalvai.bpinfo.injection.AppModule;
import com.ofalvai.bpinfo.injection.DaggerAppComponent;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.Locale;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;


public class BkkInfoApplication extends Application {

    public static AppComponent injector;

    @Inject SharedPreferences mSharedPreferences;

    private void initDagger() {
        injector = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .apiModule(new ApiModule())
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initDagger();

        injector.inject(this); // Oh the irony...

        setLanguage();

        JodaTimeAndroid.init(this);

        initInstaBug();

        Fabric.with(this, new Crashlytics(), new Answers());
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

    private void initInstaBug() {
        new Instabug.Builder(this, Config.INSTABUG_TOKEN)
                .setInvocationEvent(IBGInvocationEvent.IBGInvocationEventNone)
                .setEmailFieldRequired(false)
                .setInAppMessagingState(Feature.State.DISABLED)
                .setWillTakeScreenshot(false)
                .setCrashReportingState(Feature.State.DISABLED)
                .build();
    }
}
