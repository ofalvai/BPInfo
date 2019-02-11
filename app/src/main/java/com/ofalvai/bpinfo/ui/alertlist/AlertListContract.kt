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

package com.ofalvai.bpinfo.ui.alertlist

import com.android.volley.VolleyError
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.base.MvpPresenter
import com.ofalvai.bpinfo.ui.base.MvpView

interface AlertListContract {

    interface View : MvpView {

        fun displayAlerts(alerts: List<Alert>)

        /**
         * Displays the alert detail view.
         * If the alert object contains all required information, there's no need to call
         * updateAlertDetail() later, otherwise Alert.partial must be set to true.
         * @param alert data from a list item
         */
        fun displayAlertDetail(alert: Alert)

        /**
         * Updates the alert detail view with the full alert data
         * @param alert data coming from the alert detail API call
         */
        fun updateAlertDetail(alert: Alert)

        fun displayNetworkError(error: VolleyError)

        fun displayDataError()

        fun displayGeneralError()

        fun displayAlertDetailError()

        fun getAlertListType(): AlertListType

        fun updateSubtitle()

        fun displayNoNetworkWarning()

        /**
         * This is called by the adapter to launch the alert detail view
         */
        // TODO
        fun launchAlertDetail(alert: Alert)
    }

    interface Presenter : MvpPresenter<View> {

        var alertListType: AlertListType

        fun getFilter(): MutableSet<RouteType>?

        fun setFilter(routeTypes: MutableSet<RouteType>?)

        fun fetchAlertList()

        fun fetchAlert(alertId: String)

        fun getAlertList()

        /**
         * @return true if needed (will call fetch methods automatically)
         */
        fun updateIfNeeded(): Boolean

        fun setLastUpdate()
    }
}
