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

package com.ofalvai.bpinfo.ui.alertlist;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.VolleyError;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.RouteType;
import com.ofalvai.bpinfo.ui.base.MvpPresenter;
import com.ofalvai.bpinfo.ui.base.MvpView;

import java.util.List;
import java.util.Set;

public interface AlertListContract {

    interface View extends MvpView {
        void displayAlerts(@NonNull List<Alert> alerts);

        void displayAlertDetail(@NonNull Alert alert);

        void displayNetworkError(@NonNull VolleyError error);

        void displayDataError();

        void displayGeneralError();

        void setUpdating(boolean state);

        void updateSubtitle();

        void displayNoNetworkWarning();

        void displayNotice(String noticeText);

        void removeNotice();

        AlertListType getAlertListType();
    }

    interface Presenter extends MvpPresenter<View> {

        void fetchAlertList();

        void getAlertList();

        void updateIfNeeded();

        void setLastUpdate();

        void setFilter(@Nullable Set<RouteType> routeTypes);

        @Nullable
        Set<RouteType> getFilter();

        void fetchNotice();
    }
}
