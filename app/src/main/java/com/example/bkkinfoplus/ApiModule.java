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

package com.example.bkkinfoplus;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.bkkinfoplus.api.FutarApiClient;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Dagger module providing networking and API classes.
 */

@Module
public class ApiModule {

    @Provides
    @Singleton
    RequestQueue provideRequestQueue(Context applicationContext) {
        return Volley.newRequestQueue(applicationContext);
    }

    @Provides
    @Singleton
    FutarApiClient provideFutarApiClient(RequestQueue requestQueue) {
        return new FutarApiClient(requestQueue);
    }
}
