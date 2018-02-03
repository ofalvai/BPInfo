package com.ofalvai.bpinfo.ui.notifications

import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.ui.base.MvpPresenter
import com.ofalvai.bpinfo.ui.base.MvpView

interface NotificationsContract {

    interface View : MvpView {

        /**
         * List of all routes, before grouped by route type
         */
        fun displayRouteList(routeList: List<Route>)
        fun onRouteClicked(route: Route)
        fun displaySubscriptions(routeIdList: List<String>)

    }

    interface Presenter : MvpPresenter<View> {

        fun fetchRouteList()
        fun subscribeTo(routeID: String)
        fun fetchSubscriptions()

    }

}