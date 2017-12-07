package com.ofalvai.bpinfo.ui.notifications.routelist

import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.ui.base.MvpPresenter
import com.ofalvai.bpinfo.ui.base.MvpView

interface RouteListContract {

    interface View : MvpView {

        fun displayRoutes(routes: List<Route>)

    }

    interface Presenter : MvpPresenter<View>
}
