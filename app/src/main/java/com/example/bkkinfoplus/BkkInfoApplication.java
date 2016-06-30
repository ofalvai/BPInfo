package com.example.bkkinfoplus;

import android.app.Application;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Subclassing Application in order to build the Dagger injector.
 */
public class BkkInfoApplication extends Application {

    public static AppComponent injector;

    @Override
    public void onCreate() {
        super.onCreate();

        injector = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .apiModule(new ApiModule())
                .build();

        Locale locale = new Locale("hu");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getApplicationContext().getResources().updateConfiguration(config, null);
    }
}
