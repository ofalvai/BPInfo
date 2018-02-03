package com.ofalvai.bpinfo.ui.notifications

import com.android.volley.VolleyError
import com.ofalvai.bpinfo.BpInfoApplication
import com.ofalvai.bpinfo.api.bkkinfo.RouteListClient
import com.ofalvai.bpinfo.api.subscription.SubscriptionClient
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.ui.base.BasePresenter
import timber.log.Timber
import javax.inject.Inject

class NotificationsPresenter : BasePresenter<NotificationsContract.View>(),
        NotificationsContract.Presenter, RouteListClient.RouteListListener,
    SubscriptionClient.Callback {

    @Inject lateinit var routeListClient: RouteListClient
    @Inject lateinit var subscriptionClient: SubscriptionClient

    init {
        BpInfoApplication.injector.inject(this)
    }

    override fun fetchRouteList() {
        routeListClient.fetchRouteList(this)
    }

    override fun onRouteListResponse(routeList: List<Route>) {
        view?.displayRouteList(routeList)
    }

    override fun onRouteListError(ex: Exception) {
        Timber.e(ex) // TODO
    }

    override fun subscribeTo(routeID: String) {
        subscriptionClient.postSubscription(routeID, this)
    }

    override fun onSubscriptionError(error: VolleyError) {
        Timber.e(error) // TODO
    }

    override fun onPostSubscriptionResponse() {

    }
}