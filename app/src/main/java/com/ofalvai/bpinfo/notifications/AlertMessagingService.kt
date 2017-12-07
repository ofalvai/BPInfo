package com.ofalvai.bpinfo.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.ui.alertlist.AlertListActivity
import timber.log.Timber
import java.util.*

const val DATA_KEY_ID = "id"
const val DATA_KEY_TITLE = "title"

const val REQUEST_CODE = 0

const val NOTIF_CHANNEL_ID_ALERTS = "channel_alerts"

class AlertMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        if (remoteMessage == null) return

        Timber.d("Message from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            if (data.containsKey(DATA_KEY_TITLE) && data.containsKey(DATA_KEY_ID)) {
                val id = data[DATA_KEY_ID]
                val title = data[DATA_KEY_TITLE]
                val text = DescriptionMaker.makeDescription(data, baseContext)
                if (title != null && id != null) {
                    Timber.d("Creating notification")
                    Timber.d("ID: $id")
                    Timber.d("Title: $title")
                    Timber.d("Text: $text")

                    createNotification(id, title, text)
                } else {
                    Timber.e("Message data is null")
                }
            } else {
                Timber.e("Message data is invalid")
            }
        } else {
            Timber.e("Message data is empty")
        }
    }

    private fun createNotification(id: String, title: String, text: String) {
        val intent = Intent(this, AlertListActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID_ALERTS)
                .setSmallIcon(R.drawable.ic_notification_default)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(ContextCompat.getColor(baseContext, R.color.colorPrimary))
                .setShowWhen(true)
                .setWhen(Date().time)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = parseAlertNumericalId(id)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun parseAlertNumericalId(id: String): Int {
        return try {
            id.split("-")[1].toInt()
        } catch (ex: Exception) {
            -1
        }
    }
}
