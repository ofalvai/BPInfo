package com.ofalvai.bpinfo.ui.alertlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.RouteType

/**
 * ViewModel of a tab (an alert list)
 * [AlertsViewModel] is the ViewModel of the whole screen
 */
class AlertListViewModel(
    private val alertListType: AlertListType,
    private val alertsRepository: AlertsRepository
) : ViewModel() {

    val alerts: LiveData<List<Alert>> = Transformations.map(
        when (alertListType) {
            AlertListType.ALERTS_TODAY -> alertsRepository.todayAlerts
            AlertListType.ALERTS_FUTURE -> alertsRepository.futureAlerts
        },
        this::sortAndFilter
    )
    val alertError: LiveData<AlertsRepository.Error> = alertsRepository.error

    fun refresh() {
        alertsRepository.fetchAlerts()
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