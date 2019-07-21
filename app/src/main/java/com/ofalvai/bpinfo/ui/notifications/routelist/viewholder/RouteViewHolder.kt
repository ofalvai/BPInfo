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

package com.ofalvai.bpinfo.ui.notifications.routelist.viewholder

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
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

        route.description?.let {
            descriptionTextView.text = HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }
}
