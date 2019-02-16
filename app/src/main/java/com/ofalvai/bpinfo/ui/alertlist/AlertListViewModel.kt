package com.ofalvai.bpinfo.ui.alertlist

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.Resource
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
                AlertListType.Today -> alertsRepository.todayAlerts
                AlertListType.Future -> alertsRepository.futureAlerts
            },
            this::sortAndFilter
    )

    val alertError: LiveData<AlertsRepository.Error> = alertsRepository.error
    val status: LiveData<Status> = alertsRepository.status
    val noConnectionWarning = SingleLiveEvent<Void>()

    var activeFilter: MutableSet<RouteType> = mutableSetOf()
        set(value) {
            field = value

            // Triggering a change in order to re-run Transformations.map() above
            // with the changed filters
            alertsRepository.todayAlerts.value = alertsRepository.todayAlerts.value
            alertsRepository.futureAlerts.value = alertsRepository.futureAlerts.value
        }


    private val alertComparator = compareBy<Alert> { it.start }.thenBy { it.description }

    fun refresh() {
        if (appContext.hasNetworkConnection()) {
            alertsRepository.fetchAlerts()
        } else {
            noConnectionWarning.call()
        }
    }

    fun fetchAlert(id: String): LiveData<Resource<Alert>> =
            alertsRepository.fetchAlert(id, alertListType)

    private fun sortAndFilter(alertList: List<Alert>): List<Alert> {
        val filteredList = if (activeFilter.isEmpty()) alertList else {
            alertList.filter { alert ->
                alert.affectedRoutes.any { activeFilter.contains(it.type) }
            }
        }

        val sortedList = filteredList.sortedWith(alertComparator)
        return if (alertListType == AlertListType.Today) {
            sortedList.reversed()
        } else {
            sortedList
        }
    }
}