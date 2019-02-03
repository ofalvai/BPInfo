package com.ofalvai.bpinfo.ui.alertlist

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.model.Status
import com.ofalvai.bpinfo.util.SingleLiveEvent
import com.ofalvai.bpinfo.util.hasNetworkConnection

/**
 * ViewModel of a tab (an alert list)
 * [AlertsViewModel] is the ViewModel of the whole screen
 */
class AlertListViewModel(
    private val alertListType: AlertListType,
    private val alertsRepository: AlertsRepository,
    private val appContext: Context
) : ViewModel() {

    val alerts: LiveData<List<Alert>> = Transformations.map(
        when (alertListType) {
            AlertListType.ALERTS_TODAY -> alertsRepository.todayAlerts
            AlertListType.ALERTS_FUTURE -> alertsRepository.futureAlerts
        },
        this::sortAndFilter
    )

    val alertError: LiveData<AlertsRepository.Error> = alertsRepository.error
    val status: LiveData<Status> = alertsRepository.status
    val noConnectionWarning = SingleLiveEvent<Void>()

    fun refresh() {
        if (appContext.hasNetworkConnection()) {
            alertsRepository.fetchAlerts()
        } else {
            noConnectionWarning.call()
        }
    }

    private var activeFilter: MutableSet<RouteType> = mutableSetOf()

    private val alertComparator = compareBy<Alert> { it.start }.thenBy { it.description }

    private fun sortAndFilter(alertList: List<Alert>): List<Alert> {
        // TODO: filtering goes here
        val sortedList = alertList.sortedWith(alertComparator)
        return if (alertListType == AlertListType.ALERTS_TODAY) {
            sortedList.reversed()
        } else {
            sortedList
        }
    }
}