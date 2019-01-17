package com.ofalvai.bpinfo.ui.alertlist

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ofalvai.bpinfo.api.notice.NoticeClient

/**
 * ViewModel of the whole alerts screen with the ViewPager and 2 tabs.
 * [AlertListViewModel] is the ViewModel of each tab.
 */
class AlertsViewModel(
        private val alertsRepository: AlertsRepository,
        private val noticeClient: NoticeClient,
        private val sharedPreferences: SharedPreferences
) : ViewModel() {

    val notice = MutableLiveData<String?>()

    init {
        fetchNotices()
    }

    private fun fetchNotices() {
        // TODO: parameter order
        // TODO: merge two methods into a String? parameter
        noticeClient.fetchNotice(object : NoticeClient.NoticeListener {
            override fun onNoticeResponse(noticeBody: String) {
                notice.value = noticeBody
            }

            override fun onNoNotice() {
                notice.value = null
            }
        }, getCurrentLanguageCode())
    }

    // TODO: implement without context
    // TODO: return enum, not string
    private fun getCurrentLanguageCode(): String {
        return "hu"
    }




}