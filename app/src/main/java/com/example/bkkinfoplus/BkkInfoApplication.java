package com.example.bkkinfoplus;

import android.app.Application;

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
    }
}
