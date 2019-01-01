package com.ofalvai.bpinfo.ui.alertlist

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.crashlytics.android.Crashlytics
import com.ofalvai.bpinfo.api.AlertApiClient
import com.ofalvai.bpinfo.api.AlertListErrorMessage
import com.ofalvai.bpinfo.api.AlertListMessage
import com.ofalvai.bpinfo.api.AlertRequestParams
import com.ofalvai.bpinfo.api.notice.NoticeClient
import com.ofalvai.bpinfo.model.Alert
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.json.JSONException
import org.threeten.bp.LocalDateTime
import timber.log.Timber

/**
 * ViewModel of the whole alerts screen with the ViewPager and 2 tabs.
 * [AlertListViewModel] is the ViewModel of each tab.
 */
class AlertsViewModel(
        private val alertApiClient: AlertApiClient,
        private val noticeClient: NoticeClient,
        private val sharedPreferences: SharedPreferences
) : ViewModel() {

    val todayAlerts = MutableLiveData<List<Alert>>()
    val futureAlerts = MutableLiveData<List<Alert>>()

    val notice = MutableLiveData<String?>()

    private var lastUpdate: LocalDateTime = LocalDateTime.now()

    private val alertRequestParams: AlertRequestParams
        get() = AlertRequestParams(AlertListType.ALERTS_TODAY, getCurrentLanguageCode())
        // TODO: remove AlertListType from params

    init {
        EventBus.getDefault().register(this)
        fetchAlerts()
        fetchNotices()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    private fun fetchAlerts() {
        // TODO: network detection
        alertApiClient.fetchAlertList(alertRequestParams)
    }

    @Subscribe
    fun onAlertListEvent(message: AlertListMessage) {
        lastUpdate = LocalDateTime.now()

        todayAlerts.value = message.todayAlerts
        futureAlerts.value = message.futureAlerts
    }

    @Subscribe
    fun onAlertListErrorEvent(message: AlertListErrorMessage) {
        val ex = message.exception
        Timber.e(ex.toString())
        when (ex) {
//            is VolleyError -> view?.displayNetworkError(ex)
            is JSONException -> {
//                view?.displayDataError()
                Crashlytics.logException(ex)
            }
            else -> {
//                view?.displayGeneralError()
                Crashlytics.logException(ex)
            }
        }
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