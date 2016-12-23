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

package com.ofalvai.bpinfo.injection;

import com.ofalvai.bpinfo.BpInfoApplication;
import com.ofalvai.bpinfo.api.bkkfutar.FutarApiClient;
import com.ofalvai.bpinfo.api.NoticeClient;
import com.ofalvai.bpinfo.ui.alertlist.AlertListPresenter;
import com.ofalvai.bpinfo.ui.settings.SettingsActivity;

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

    void inject(BpInfoApplication bpInfoApplication);

    void inject(FutarApiClient futarApiClient);

    void inject(NoticeClient noticeClient);
}
