package com.ofalvai.bpinfo.notifications

import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ofalvai.bpinfo.util.LocaleManager
import timber.log.Timber

class AlertMessagingService : FirebaseMessagingService() {

    companion object {
        const val DATA_KEY_ID = "id"
        const val DATA_KEY_TITLE = "title"
    }

    override fun attachBaseContext(base: Context) {
        // Updating locale
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

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

                    NotificationMaker.make(this, id, title, text)
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
}
