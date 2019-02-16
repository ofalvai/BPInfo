package com.ofalvai.bpinfo.ui.alertlist

import android.content.SharedPreferences
import androidx.lifecycle.*
import com.ofalvai.bpinfo.api.notice.NoticeClient
import com.ofalvai.bpinfo.repository.AlertsRepository
import com.ofalvai.bpinfo.util.LocaleManager

/**
 * ViewModel of the whole alerts screen with the ViewPager and 2 tabs.
 * [AlertListViewModel] is the ViewModel of each tab.
 */
class AlertsViewModel(
        private val alertsRepository: AlertsRepository,
        private val noticeClient: NoticeClient,
        private val sharedPreferences: SharedPreferences
) : ViewModel(), LifecycleObserver {

    val notice = MutableLiveData<String?>()

    init {
        fetchNotices()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun updateIfNeeded() {
        alertsRepository.fetchAlerts()
    }

    fun fetchNotices() {
        val languageCode = LocaleManager.getCurrentLanguageCode(sharedPreferences)

        noticeClient.fetchNotice(object : NoticeClient.NoticeListener {
            override fun onNoticeResponse(noticeBody: String) {
                notice.value = noticeBody
            }

            override fun onNoNotice() {
                notice.value = null
            }
        }, languageCode)
    }
}