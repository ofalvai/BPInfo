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

package com.ofalvai.bpinfo.ui.notifications.routelist.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
