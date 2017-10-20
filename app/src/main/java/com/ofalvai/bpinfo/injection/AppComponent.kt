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

package com.ofalvai.bpinfo.injection

import com.ofalvai.bpinfo.BpInfoApplication
import com.ofalvai.bpinfo.api.NoticeClient
import com.ofalvai.bpinfo.api.bkkfutar.FutarApiClient
import com.ofalvai.bpinfo.api.bkkinfo.BkkInfoClient
import com.ofalvai.bpinfo.ui.alertlist.AlertListPresenter
import com.ofalvai.bpinfo.ui.notifications.NotificationsPresenter
import com.ofalvai.bpinfo.ui.settings.SettingsActivity
import dagger.Component
import javax.inject.Singleton

/**
 * Required interface for Dagger code generation.
 * The methods are used where field injection is needed.
 */

// TODO: only use field injection with Android classes
@Singleton
@Component(modules = arrayOf(AppModule::class, ApiModule::class))
interface AppComponent {
    fun inject(alertListPresenter: AlertListPresenter)

    fun inject(settingsActivity: SettingsActivity)

    fun inject(bpInfoApplication: BpInfoApplication)

    fun inject(futarApiClient: FutarApiClient)

    fun inject(noticeClient: NoticeClient)

    fun inject(bkkInfoClient: BkkInfoClient)

    fun inject(notificationsPresenter: NotificationsPresenter)
}
