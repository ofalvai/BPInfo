package com.ofalvai.bpinfo.ui.notifications.routelist

import android.graphics.Color
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.base.BasePresenter

class RouteListPresenter : RouteListContract.Presenter, BasePresenter<RouteListContract.View>() {

    override fun fetchRoutes() {

        val textColor = Color.parseColor("#FFFFFF")
        val backgroundColor = Color.parseColor("#000000")

        val routes: List<Route> = listOf(
                Route(id = "1", shortName = "99", description = "Test desc", color = backgroundColor, textColor = textColor, type = RouteType.BUS, longName = null),
                Route(id = "1", shortName = "99", description = "Test desc", color = backgroundColor, textColor = textColor, type = RouteType.BUS, longName = null),
                Route(id = "1", shortName = "99", description = "Test desc", color = backgroundColor, textColor = textColor, type = RouteType.BUS, longName = null),
                Route(id = "1", shortName = "99", description = "Test desc", color = backgroundColor, textColor = textColor, type = RouteType.BUS, longName = null),
                Route(id = "1", shortName = "99", description = "Test desc", color = backgroundColor, textColor = textColor, type = RouteType.BUS, longName = null),
                Route(id = "1", shortName = "99", description = "Test desc", color = backgroundColor, textColor = textColor, type = RouteType.BUS, longName = null),
                Route(id = "1", shortName = "99", description = "Test desc", color = backgroundColor, textColor = textColor, type = RouteType.BUS, longName = null),
                Route(id = "1", shortName = "99", description = "Test desc", color = backgroundColor, textColor = textColor, type = RouteType.BUS, longName = null),
                Route(id = "1", shortName = "99", description = "Test desc", color = backgroundColor, textColor = textColor, type = RouteType.BUS, longName = null)
        )

        view?.displayRoutes(routes)
    }
}