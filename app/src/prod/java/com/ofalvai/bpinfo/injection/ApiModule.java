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

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.api.AlertApiClient;
import com.ofalvai.bpinfo.api.NoticeClient;
import com.ofalvai.bpinfo.api.bkkfutar.FutarApiClient;
import com.ofalvai.bpinfo.api.bkkinfo.BkkInfoClient;

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
    AlertApiClient provideAlertApiClient(RequestQueue requestQueue,
                                         SharedPreferences sharedPreferences, Context context) {
        String keyBkkFutar = context.getString(R.string.pref_key_data_source_futar);
        String keyBkkInfo = context.getString(R.string.pref_key_data_source_bkk_info);
        String keyCurrent = sharedPreferences.
                getString(context.getString(R.string.pref_key_data_source), keyBkkInfo);

        if (keyCurrent.equals(keyBkkFutar)) {
            return new FutarApiClient(requestQueue);
        } else {
            return new BkkInfoClient(requestQueue);
        }
    }

    @Provides
    @Singleton
    NoticeClient provideNoticeClient(RequestQueue requestQueue) {
        return new NoticeClient(requestQueue);
    }
}
