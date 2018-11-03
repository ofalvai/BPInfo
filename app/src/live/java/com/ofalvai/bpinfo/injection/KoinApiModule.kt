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
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.api.AlertApiClient
import com.ofalvai.bpinfo.api.bkkfutar.FutarApiClient
import com.ofalvai.bpinfo.api.bkkinfo.BkkInfoClient
import com.ofalvai.bpinfo.api.bkkinfo.RouteListClient
import com.ofalvai.bpinfo.api.notice.NoticeClient
import com.ofalvai.bpinfo.api.subscription.SubscriptionClient
import com.ofalvai.bpinfo.util.Analytics
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val apiModule = module {

    single<RequestQueue> { Volley.newRequestQueue(androidContext()) }

    @Suppress("RemoveExplicitTypeArguments")
    single<AlertApiClient> {
        val sharedPreferences: SharedPreferences = get()

        val keyBkkFutar = androidContext().getString(R.string.pref_key_data_source_futar)
        val keyDefault = androidContext().getString(R.string.pref_key_data_source_default)
        val keyCurrent = sharedPreferences.getString(
            androidContext().getString(R.string.pref_key_data_source),
            keyDefault
        )

        if (keyCurrent == keyBkkFutar) {
            Analytics.setDataSource(androidContext(), Analytics.DATA_SOURCE_FUTAR)
            FutarApiClient(get(), sharedPreferences, androidContext())
        } else {
            Analytics.setDataSource(androidContext(), Analytics.DATA_SOURCE_BKKINFO)
            BkkInfoClient(get(), sharedPreferences, androidContext())
        }
    }

    single { NoticeClient(get(), androidContext(), get()) }

    single { RouteListClient(get()) }

    single { SubscriptionClient(get()) }
}