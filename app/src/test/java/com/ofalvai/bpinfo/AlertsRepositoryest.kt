package com.ofalvai.bpinfo

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ofalvai.bpinfo.api.AlertApiClient
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.repository.AlertsRepository
import com.ofalvai.bpinfo.util.any
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class AlertsRepositoryest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var alertsRepository: AlertsRepository

    @Before
    fun setUp() {
        val alertApiClient = Mockito.mock(AlertApiClient::class.java)
        val appContext = Mockito.mock(Context::class.java)

        alertsRepository = AlertsRepository(alertApiClient, appContext)


        Mockito.`when`(alertApiClient.fetchAlertList(any())).thenAnswer {
            val alertListCallback = it.arguments[0] as AlertApiClient.AlertListCallback
            alertListCallback.onAlertListResponse(listOf(), listOf())
        }
    }

    @Test
    fun `test test test`() {
        alertsRepository.fetchAlerts()

        assertEquals(emptyList<Alert>(), alertsRepository.todayAlerts.value)
        assertEquals(emptyList<Alert>(), alertsRepository.futureAlerts.value)


    }

}