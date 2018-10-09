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

import android.content.Context
import android.os.Handler
import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.ui.alertlist.AlertListContract
import com.ofalvai.bpinfo.ui.alertlist.viewholder.AlertHolder
import java.util.*

class AlertAdapter(private var alerts: List<Alert>,
                   private val context: Context,
                   private val view: AlertListContract.View) : RecyclerView.Adapter<AlertHolder>() {

    fun updateAlertData(alerts: List<Alert>,
                        listUpdateCallback: ListUpdateCallback) {
        val oldAlerts = ArrayList(this.alerts)
        if (oldAlerts == alerts) return

        // Running diff calculation on a worker thread, because it can be too expensive
        // Note: returns to the UI thread to update alerts and dispatch updates.
        val diffCalculation = Runnable {
            val callback = AlertDiffCallback(oldAlerts, alerts)
            val diff = DiffUtil.calculateDiff(callback)

            // Returning to the UI thread to apply the diff
            Handler(context.mainLooper).post {
                this.alerts = alerts
                diff.dispatchUpdatesTo(this@AlertAdapter)
                diff.dispatchUpdatesTo(listUpdateCallback)
            }
        }

        Thread(diffCalculation).start()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertHolder {
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.list_item_alert, parent, false)

        val holder = AlertHolder(view, this.view.getAlertListType())
        holder.itemView.setOnClickListener {
            val alert = alerts[holder.adapterPosition]
            this.view.launchAlertDetail(alert)
        }

        return holder
    }

    override fun onBindViewHolder(holder: AlertHolder, position: Int) {
        holder.bindAlert(alerts[position], context)
    }

    override fun getItemCount() = alerts.size

    private class AlertDiffCallback(private val oldAlerts: List<Alert>,
                                          private val newAlerts: List<Alert>) : DiffUtil.Callback() {

        override fun getOldListSize() = oldAlerts.size

        override fun getNewListSize() = newAlerts.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldAlerts[oldItemPosition].id == newAlerts[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldAlerts[oldItemPosition] == newAlerts[newItemPosition]
        }
    }
}
