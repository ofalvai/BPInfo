package com.ofalvai.bpinfo.ui.notifications.routelist.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.ui.notifications.routelist.viewholder.RouteViewHolder

interface RouteClickListener {
    fun onRouteClicked(route: Route)
}

class RouteAdapter(private val context: Context,
                   private val clickListener: RouteClickListener
) : RecyclerView.Adapter<RouteViewHolder>() {

    var routeList: List<Route> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(routeList[position])
    }

    override fun getItemCount(): Int {
        return routeList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.list_item_route, parent, false)
        val viewHolder = RouteViewHolder(itemView)
        initClickListener(viewHolder)
        return viewHolder
    }

    private fun initClickListener(viewHolder: RouteViewHolder) {
        viewHolder.itemView.setOnClickListener {
            val route = routeList[viewHolder.adapterPosition]
            clickListener.onRouteClicked(route)
        }
    }
}
