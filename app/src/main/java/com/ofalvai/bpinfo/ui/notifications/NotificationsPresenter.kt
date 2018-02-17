package com.ofalvai.bpinfo.ui.notifications

import com.android.volley.VolleyError
import com.ofalvai.bpinfo.BpInfoApplication
import com.ofalvai.bpinfo.api.bkkinfo.RouteListClient
import com.ofalvai.bpinfo.api.subscription.SubscriptionClient
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteSubscription
import com.ofalvai.bpinfo.ui.base.BasePresenter
import timber.log.Timber
import javax.inject.Inject

class NotificationsPresenter : BasePresenter<NotificationsContract.View>(),
        NotificationsContract.Presenter, RouteListClient.RouteListListener,
    SubscriptionClient.Callback {

    @Inject lateinit var routeListClient: RouteListClient
    @Inject lateinit var subscriptionClient: SubscriptionClient

    private var routeListResponse: List<Route>? = null
    private var subscribedRouteIDListResponse: List<String>? = null

    init {
        BpInfoApplication.injector.inject(this)
    }

    override fun fetchRouteList() {
        routeListClient.fetchRouteList(this)
    }

    override fun onRouteListResponse(routeList: List<Route>) {
        routeListResponse = routeList
        view?.displayRouteList(routeList)

        subscribedRouteIDListResponse?.let {
            displaySubscribedRoutes(it, routeList)
        }
    }

    override fun onRouteListError(ex: Exception) {
        view?.showProgress(false)
        Timber.e(ex) // TODO
    }

    override fun subscribeTo(routeID: String) {
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
        Timber.e(error) // TODO
    }

    override fun onPostSubscriptionResponse(subscription: RouteSubscription) {
        view?.showProgress(false)

        val route: Route? = routeListResponse?.find { it.id == subscription.routeID }

        route?.let {
            view?.addSubscribedRoute(it)
        }
    }

    override fun onGetSubscriptionResponse(routeIDList: List<String>) {
        subscribedRouteIDListResponse = routeIDList

        routeListResponse?.let {
            displaySubscribedRoutes(routeIDList, it)
        }
    }

    override fun onDeleteSubscriptionResponse(subscription: RouteSubscription) {
        view?.showProgress(false)

        val route: Route? = routeListResponse?.find { it.id == subscription.routeID }

        route?.let {
            view?.removeSubscribedRoute(route)
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