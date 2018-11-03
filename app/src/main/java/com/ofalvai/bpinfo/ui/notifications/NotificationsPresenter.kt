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

package com.ofalvai.bpinfo.ui.notifications

import com.android.volley.VolleyError
import com.ofalvai.bpinfo.api.bkkinfo.RouteListClient
import com.ofalvai.bpinfo.api.subscription.SubscriptionClient
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteSubscription
import com.ofalvai.bpinfo.ui.base.BasePresenter
import timber.log.Timber

class NotificationsPresenter(
    private val routeListClient: RouteListClient,
    private val subscriptionClient: SubscriptionClient
) : BasePresenter<NotificationsContract.View>(), NotificationsContract.Presenter,
    RouteListClient.RouteListListener,
    SubscriptionClient.Callback {

    private var routeList: List<Route>? = null
    private var subscribedRouteIDList: MutableList<String>? = null

    override fun fetchRouteList() {
        routeListClient.fetchRouteList(this)
    }

    override fun onRouteListResponse(routeList: List<Route>) {
        this.routeList = routeList
        view?.displayRouteList(routeList)
        view?.showRouteListError(false)

        subscribedRouteIDList?.let {
            displaySubscribedRoutes(it, routeList)
        }
    }

    override fun onRouteListError(ex: Exception) {
        view?.showRouteListError(true)
        Timber.e(ex)
    }

    override fun subscribeTo(routeID: String) {
        subscribedRouteIDList?.let {
            if (it.contains(routeID)) {
                return
            }
        }

        view?.showProgress(true)
        subscriptionClient.postSubscription(routeID, this)
    }

    override fun removeSubscription(routeID: String) {
        view?.showProgress(true)
        subscriptionClient.deleteSubscription(routeID, this)
    }

    override fun fetchSubscriptions() {
        view?.showProgress(true)
        subscriptionClient.getSubscriptions(this)
    }

    override fun onSubscriptionError(error: VolleyError) {
        view?.showProgress(false)
        view?.showSubscriptionError()
    }

    override fun onPostSubscriptionResponse(subscription: RouteSubscription) {
        view?.showProgress(false)

        subscribedRouteIDList?.let {
            if (it.contains(subscription.routeID)) {
                return
            }
        }

        subscribedRouteIDList?.add(subscription.routeID)

        val route: Route? = routeList?.find { it.id == subscription.routeID }
        route?.let {
            view?.addSubscribedRoute(it)
        }
    }

    override fun onGetSubscriptionResponse(routeIDList: List<String>) {
        subscribedRouteIDList = routeIDList.toMutableList()

        routeList?.let {
            displaySubscribedRoutes(routeIDList, it)
        }
    }

    override fun onDeleteSubscriptionResponse(subscription: RouteSubscription) {
        view?.showProgress(false)

        subscribedRouteIDList?.removeAll { it == subscription.routeID }

        val route: Route? = routeList?.find { it.id == subscription.routeID }
        route?.let {
            view?.removeSubscribedRoute(it)
        }
    }

    /**
     * Calls the View with the full Route objects when both the subscribed route IDs and
     * the list of all Route objects are available
     */
    private fun displaySubscribedRoutes(routeIDList: List<String>, allRoutes: List<Route>) {
        val routes: List<Route> = allRoutes.filter { routeIDList.contains(it.id) }
        view?.displaySubscriptions(routes)
        view?.showProgress(false)
    }
}