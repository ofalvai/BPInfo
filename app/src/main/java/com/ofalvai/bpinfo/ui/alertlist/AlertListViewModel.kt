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
        private val parentViewModel: AlertsViewModel
) : ViewModel() {

    val alerts: LiveData<List<Alert>> = Transformations.map(parentViewModel.todayAlerts, this::alertListMapper)

    private lateinit var alertListType: AlertListType

    private var activeFilter: MutableSet<RouteType> = mutableSetOf()



    private fun alertListMapper(alertList: List<Alert>): List<Alert> {
        return alertList // TODO
    }

}