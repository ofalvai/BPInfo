/*
 * Copyright 2016. 12. 23. Olivér Falvai
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

package com.ofalvai.bpinfo.api;

import android.support.annotation.NonNull;

import com.ofalvai.bpinfo.model.Alert;

import java.util.List;

public interface AlertApiClient {

    void fetchAlertList(@NonNull AlertListListener listener,
                        @NonNull AlertRequestParams params);

    void fetchAlert(@NonNull String id,
                    @NonNull AlertListener listener,
                    @NonNull AlertRequestParams params);

    interface AlertListListener {

        void onAlertListResponse(List<Alert> alerts);

        void onError(Exception ex);
    }

    interface AlertListener {

        void onAlertResponse(Alert alert);

        void onError(Exception ex);
    }

}
