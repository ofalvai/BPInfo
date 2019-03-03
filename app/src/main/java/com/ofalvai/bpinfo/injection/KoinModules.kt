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

package com.ofalvai.bpinfo.injection

import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.ofalvai.bpinfo.repository.AlertsRepository
import com.ofalvai.bpinfo.ui.alertdetail.AlertDetailViewModel
import com.ofalvai.bpinfo.ui.alertlist.AlertListType
import com.ofalvai.bpinfo.ui.alertlist.AlertListViewModel
import com.ofalvai.bpinfo.ui.alertlist.AlertsViewModel
import com.ofalvai.bpinfo.ui.notifications.NotificationsViewModel
import com.ofalvai.bpinfo.util.Analytics
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val appModule = module {

    single<SharedPreferences> {
        PreferenceManager.getDefaultSharedPreferences(androidContext())
    }

    single { Analytics(androidContext()) }

    single { AlertsRepository(get(), androidContext(), get()) }
}

val screenModule = module {

    viewModel { NotificationsViewModel(get(), get(), get()) }

    viewModel { AlertsViewModel(get(), get(), get()) }

    viewModel { (type: AlertListType) ->
        AlertListViewModel(type, get(), androidContext())
    }

    viewModel { AlertDetailViewModel(get()) }
}
// Note: apiModule depends on selected product flavor (mock/live)
val allModules = listOf(appModule, screenModule, apiModule)