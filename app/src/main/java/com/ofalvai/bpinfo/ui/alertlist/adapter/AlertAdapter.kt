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

package com.ofalvai.bpinfo.ui.alertlist.adapter

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.ui.alertlist.AlertListContract
import com.ofalvai.bpinfo.ui.alertlist.viewholder.AlertHolder

class AlertAdapter(
    private val view: AlertListContract.View
) : ListAdapter<Alert, AlertHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Alert>() {
            override fun areItemsTheSame(oldItem: Alert, newItem: Alert) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Alert, newItem: Alert) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.list_item_alert, parent, false)

        val holder = AlertHolder(view, this.view.getAlertListType())
        holder.itemView.setOnClickListener {
            val alert = getItem(holder.adapterPosition)
            this.view.launchAlertDetail(alert)
        }

        return holder
    }

    override fun onBindViewHolder(holder: AlertHolder, position: Int) {
        holder.bindAlert(getItem(position))
    }


}
