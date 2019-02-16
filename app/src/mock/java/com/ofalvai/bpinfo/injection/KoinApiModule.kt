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

import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.ofalvai.bpinfo.api.AlertApiClient
import com.ofalvai.bpinfo.api.MockApiClient
import com.ofalvai.bpinfo.api.bkkinfo.RouteListClient
import com.ofalvai.bpinfo.api.notice.NoticeClient
import com.ofalvai.bpinfo.api.subscription.SubscriptionClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val apiModule = module {

    single<RequestQueue> { Volley.newRequestQueue(androidContext()) }

    single<AlertApiClient> { MockApiClient() }

    single { NoticeClient(get(), androidContext(), get()) }

    single { RouteListClient(get()) }

    single { SubscriptionClient(get()) }
}