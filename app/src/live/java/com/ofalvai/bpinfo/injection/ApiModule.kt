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

import android.content.Context
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
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Dagger module providing networking and API classes.
 */
@Module
class ApiModule {

    @Provides
    @Singleton
    internal fun provideRequestQueue(applicationContext: Context) =
        Volley.newRequestQueue(applicationContext)

    @Provides
    @Singleton
    internal fun provideAlertApiClient(
        requestQueue: RequestQueue, sharedPreferences: SharedPreferences, context: Context
    ): AlertApiClient {
        val keyBkkFutar = context.getString(R.string.pref_key_data_source_futar)
        val keyDefault = context.getString(R.string.pref_key_data_source_default)
        val keyCurrent = sharedPreferences.getString(
            context.getString(R.string.pref_key_data_source),
            keyDefault
        )

        return if (keyCurrent == keyBkkFutar) {
            Analytics.setDataSource(context, Analytics.DATA_SOURCE_FUTAR)
            FutarApiClient(requestQueue, sharedPreferences, context)
        } else {
            Analytics.setDataSource(context, Analytics.DATA_SOURCE_BKKINFO)
            BkkInfoClient(requestQueue, sharedPreferences, context)
        }
    }

    @Provides
    @Singleton
    internal fun provideNoticeClient(
        requestQueue: RequestQueue, context: Context, sharedPreferences: SharedPreferences
    ) = NoticeClient(requestQueue, context, sharedPreferences)

    @Provides
    @Singleton
    internal fun provideRouteListClient(requestQueue: RequestQueue) = RouteListClient(requestQueue)

    @Provides
    @Singleton
    internal fun provideSubscriptionClient(requestQueue: RequestQueue, sharedPreferences: SharedPreferences) =
        SubscriptionClient(requestQueue, sharedPreferences)
}
