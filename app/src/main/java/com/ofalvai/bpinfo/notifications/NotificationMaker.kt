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

package com.ofalvai.bpinfo.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.ui.alertlist.AlertListActivity
import java.util.*

object NotificationMaker {

    const val INTENT_EXTRA_ALERT_ID = "alert_id"
    private const val INTENT_REQUEST_CODE = 0

    fun make(context: Context, id: String, title: String, text: String) {
        val intent = Intent(context, AlertListActivity::class.java)
        intent.putExtra(INTENT_EXTRA_ALERT_ID, id)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context, INTENT_REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
        bigTextStyle.setBigContentTitle(title)
        bigTextStyle.bigText(text)

        val channelId = context.getString(R.string.notif_channel_alerts_id)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_default)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(bigTextStyle)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setShowWhen(true)
            .setWhen(Date().time)

        val notificationManager =
            context.getSystemService<NotificationManager>()
        val notificationId = parseAlertNumericalId(id)
        notificationManager?.notify(notificationId, notificationBuilder.build())
    }

    private fun parseAlertNumericalId(id: String): Int {
        return try {
            id.split("-")[1].toInt()
        } catch (ex: Exception) {
            -1
        }
    }

}