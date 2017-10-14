package com.ofalvai.bpinfo.ui.notifications.routelist.viewholder

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.util.addRouteIcon
import com.ofalvai.bpinfo.util.bindView

class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val iconWrapperLayout: FrameLayout by bindView(R.id.list_item_route__icon_wrapper)

    private val descriptionTextView: TextView by bindView(R.id.list_item_route__description)

    fun bind(route: Route) {
        iconWrapperLayout.removeAllViews()
        addRouteIcon(itemView.context, iconWrapperLayout, route)

        descriptionTextView.text = Html.fromHtml(route.description)
    }

}