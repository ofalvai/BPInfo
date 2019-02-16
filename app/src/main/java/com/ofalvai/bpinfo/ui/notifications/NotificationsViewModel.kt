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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.crashlytics.android.Crashlytics
import com.ofalvai.bpinfo.api.bkkinfo.RouteListClient
import com.ofalvai.bpinfo.api.subscription.SubscriptionClient
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteSubscription
import com.ofalvai.bpinfo.util.SingleLiveEvent
import timber.log.Timber

class NotificationsViewModel(
    private val routeListClient: RouteListClient,
    private val subscriptionClient: SubscriptionClient
) : ViewModel(), RouteListClient.RouteListListener,
    SubscriptionClient.Callback {

    /**
     * List of all routes, before grouped by route type
     */
    val routeList = MutableLiveData<List<Route>>()

    val routeListError = MutableLiveData<Boolean>()

    val subscriptions = MutableLiveData<List<Route>>()

    /**
     * Progress of any subscription action (get list, add, remove)
     */
    val subscriptionProgress = MutableLiveData<Boolean>()

    val subscriptionError = SingleLiveEvent<Void>()

    val newSubscribedRoute = MutableLiveData<Route>()

    val removedSubscribedRoute = MutableLiveData<Route>()

    private var subscribedRouteIDList: MutableList<String>? = null

    init {
        fetchRouteList()
        fetchSubscriptions()
    }

    fun fetchRouteList() {
        routeListClient.fetchRouteList(this)
    }

    fun fetchSubscriptions() {
        subscriptionProgress.value = true
        subscriptionClient.getSubscriptions(this)
    }

    fun subscribeTo(routeID: String) {
        subscribedRouteIDList?.let {
            if (it.contains(routeID)) {
                return
            }
        }

        subscriptionProgress.value = true
        subscriptionClient.postSubscription(routeID, this)
    }

    fun removeSubscription(routeID: String) {
        subscriptionProgress.value = true
        subscriptionClient.deleteSubscription(routeID, this)
    }

    override fun onRouteListResponse(routeList: List<Route>) {
        this.routeList.value = routeList
        routeListError.value = false

        subscribedRouteIDList?.let {
            displaySubscribedRoutes(it, routeList)
        }
    }

    override fun onRouteListError(ex: Exception) {
        Timber.e(ex)
        routeListError.value = true
    }

    override fun onSubscriptionError(error: Throwable) {
        Timber.e(error)
        Crashlytics.logException(error)
        subscriptionProgress.value = false
        subscriptionError.call()
    }

    override fun onPostSubscriptionResponse(subscription: RouteSubscription) {
        subscriptionProgress.value = false

        subscribedRouteIDList?.let {
            if (it.contains(subscription.routeID)) {
                return
            }
        }

        subscribedRouteIDList?.add(subscription.routeID)

        val route: Route? = routeList.value?.find { it.id == subscription.routeID }
        route?.let {
            newSubscribedRoute.value = it
        }
    }

    override fun onGetSubscriptionResponse(routeIDList: List<String>) {
        subscribedRouteIDList = routeIDList.toMutableList()

        routeList.value?.let {
            displaySubscribedRoutes(routeIDList, it)
        }
    }

    override fun onDeleteSubscriptionResponse(subscription: RouteSubscription) {
        subscriptionProgress.value = false

        subscribedRouteIDList?.removeAll { it == subscription.routeID }

        val route: Route? = routeList.value?.find { it.id == subscription.routeID }
        route?.let {
            removedSubscribedRoute.value = it
        }
    }

    /**
     * Calls the View with the full Route objects when both the subscribed route IDs and
     * the list of all Route objects are available
     */
    private fun displaySubscribedRoutes(routeIDList: List<String>, allRoutes: List<Route>) {
        val routes: List<Route> = allRoutes.filter { routeIDList.contains(it.id) }
        subscriptions.value = routes
        subscriptionProgress.value = false
    }
}