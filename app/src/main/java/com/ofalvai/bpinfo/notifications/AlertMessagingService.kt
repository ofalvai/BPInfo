package com.ofalvai.bpinfo.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.ui.alertlist.AlertListActivity
import timber.log.Timber

const val DATA_KEY_TITLE = "title"
const val DATA_KEY_TEXT = "text"
const val REQUEST_CODE = 0

class AlertMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        if (remoteMessage == null) return

        Timber.d("Message from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            if (data.containsKey(DATA_KEY_TITLE) && data.containsKey(DATA_KEY_TEXT)) {
                val title = data[DATA_KEY_TITLE]
                val text = data[DATA_KEY_TEXT]
                if (title != null && text != null) {
                    Timber.d("Creating notification")
                    Timber.d("Title: $title")
                    Timber.d("Text: $text")

                    createNotification(title, text)
                } else {
                    Timber.d("Message data is null")
                }
            } else {
                Timber.d("Message data is invalid")
            }
        } else {
            Timber.d("Message data is empty")
        }
    }

    private fun createNotification(title: String, text: String) {
        val intent = Intent(this, AlertListActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT)

        val notificationBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification_default)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }
}