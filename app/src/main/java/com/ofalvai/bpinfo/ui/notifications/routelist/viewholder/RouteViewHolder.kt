package com.ofalvai.bpinfo.ui.notifications.routelist.viewholder

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.util.UiUtils

class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(route: Route) {
        UiUtils.addRouteIcon(itemView.context, itemView as ViewGroup, route)
    }

}