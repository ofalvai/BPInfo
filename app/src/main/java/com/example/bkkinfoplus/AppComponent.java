package com.example.bkkinfoplus;

import com.example.bkkinfoplus.ui.alertlist.AlertListPresenter;
import com.example.bkkinfoplus.ui.settings.SettingsActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Required interface for Dagger code generation.
 * The methods are used where field injection is needed.
 */

@Singleton
@Component(modules={AppModule.class, ApiModule.class})
public interface AppComponent {
    void inject(AlertListPresenter alertListPresenter);

    void inject(SettingsActivity settingsActivity);
}
