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
import java.util.*

const val DATA_KEY_ID = "id"
const val DATA_KEY_TITLE = "title"
const val DATA_KEY_ROUTE_BUS = "route_bus"
const val DATA_KEY_ROUTE_FERRY = "route_ferry"
const val DATA_KEY_ROUTE_RAIL = "route_rail"
const val DATA_KEY_ROUTE_TRAM = "route_tram"
const val DATA_KEY_ROUTE_TROLLEYBUS = "route_trolleybus"
const val DATA_KEY_ROUTE_SUBWAY = "route_subway"
const val DATA_KEY_ROUTE_OTHER = "route_other"

const val DATA_KEY_ROUTE_SEPARATOR = "|"

const val REQUEST_CODE = 0

class AlertMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        if (remoteMessage == null) return

        Timber.d("Message from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            if (data.containsKey(DATA_KEY_TITLE) && data.containsKey(DATA_KEY_ID)) {
                val id = data[DATA_KEY_ID]
                val title = data[DATA_KEY_TITLE]
                val text = makeDescription(data)
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

        val notificationBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification_default)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(resources.getColor(R.color.colorPrimary))
                .setShowWhen(true)
                .setWhen(Date().time)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id.toInt(), notificationBuilder.build())
    }

    private fun makeDescription(remoteData: Map<String, String>): String {
        val sb = StringBuilder()

        sb.append(makeRouteLine(remoteData[DATA_KEY_ROUTE_BUS], baseContext.getString(R.string.route_bus)))
        sb.append(makeRouteLine(remoteData[DATA_KEY_ROUTE_FERRY], baseContext.getString(R.string.route_ferry)))
        sb.append(makeRouteLine(remoteData[DATA_KEY_ROUTE_RAIL], baseContext.getString(R.string.route_rail)))
        sb.append(makeRouteLine(remoteData[DATA_KEY_ROUTE_TRAM], baseContext.getString(R.string.route_tram)))
        sb.append(makeRouteLine(remoteData[DATA_KEY_ROUTE_TROLLEYBUS], baseContext.getString(R.string.route_trolleybus)))
        sb.append(makeRouteLine(remoteData[DATA_KEY_ROUTE_SUBWAY], baseContext.getString(R.string.route_subway)))
        sb.append(makeRouteLine(remoteData[DATA_KEY_ROUTE_OTHER], baseContext.getString(R.string.route_other)))

        return sb.toString()
    }

    private fun makeRouteLine(routeData: String?, title: String): String {
        val sb = StringBuilder()
        val routes: String? = routeData
        if (routes != null && routes.isNotEmpty()) {
            sb.append("$title: ")
            val routeList = routes
                    .split(DATA_KEY_ROUTE_SEPARATOR)
                    .map { it.trim() }
                    .joinToString(separator=", ")
            sb.append(routeList)
            sb.append("\n")
        }
        return sb.toString()
    }
}