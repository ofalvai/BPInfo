package com.ofalvai.bpinfo

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.content.getSystemService
import androidx.lifecycle.Observer
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.ofalvai.bpinfo.api.AlertApiClient
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.Resource
import com.ofalvai.bpinfo.model.Status
import com.ofalvai.bpinfo.repository.AlertsRepository
import com.ofalvai.bpinfo.ui.alertlist.AlertListType
import com.ofalvai.bpinfo.util.Analytics
import com.ofalvai.bpinfo.util.any
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import org.threeten.bp.Duration

class AlertsRepositoryTest {

    companion object {
        private val testTodayAlerts = listOf(
            Alert("mock-1", 0, 0, 0, null, null, null, emptyList(), false),
            Alert("mock-2", 0, 0, 0, null, null, null, emptyList(), false)
        )
        private val testFutureAlerts = listOf(
            Alert("mock-3", 0, 0, 0, null, null, null, emptyList(), false),
            Alert("mock-4", 0, 0, 0, null, null, null, emptyList(), false),
            Alert("mock-5", 0, 0, 0, null, null, null, emptyList(), false)
        )
        private val testAlert = Alert("mock-4", 0, 0, 0, null, null, null, emptyList(), false)
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var alertsRepository: AlertsRepository
    private lateinit var alertApiClient: AlertApiClient
    private lateinit var networkInfo: NetworkInfo

    @Before
    fun setUp() {
        alertApiClient = mock(AlertApiClient::class.java)
        networkInfo = mock(NetworkInfo::class.java)
        val analytics = mock(Analytics::class.java)
        val appContext = mock(Context::class.java)
        val connectivityManager = mock(ConnectivityManager::class.java)

        doNothing().`when`(analytics).logException(any())
        `when`(appContext.getSystemService<ConnectivityManager>())
            .thenReturn(connectivityManager)
        `when`(connectivityManager.activeNetworkInfo).thenReturn(networkInfo)
        // networkInfo.isConnected is mocked in each test case

        alertsRepository = AlertsRepository(alertApiClient, appContext, analytics)
    }

    @Test
    fun `first list fetch with network connection`() {
        `when`(networkInfo.isConnected).thenReturn(true)

        `when`(alertApiClient.fetchAlertList(any())).thenAnswer {
            val alertListCallback = it.arguments[0] as AlertApiClient.AlertListCallback
            alertListCallback.onAlertListResponse(testTodayAlerts, testFutureAlerts)
        }

        alertsRepository.fetchAlerts()

        assertEquals(Status.Success, alertsRepository.status.value)
        assertEquals(testTodayAlerts, alertsRepository.todayAlerts.value)
        assertEquals(testFutureAlerts, alertsRepository.futureAlerts.value)
        assertNull(alertsRepository.error.value)

    }

    @Test
    fun `first list fetch without network connection`() {
        `when`(networkInfo.isConnected).thenReturn(false)

        alertsRepository.fetchAlerts()

        assertEquals(Status.Error, alertsRepository.status.value)
        assertTrue(alertsRepository.error.value is AlertsRepository.Error.NetworkError)
        val networkError = alertsRepository.error.value as AlertsRepository.Error.NetworkError
        assertTrue(networkError.volleyError is NoConnectionError)
        assertNull(alertsRepository.todayAlerts.value)
        assertNull(alertsRepository.futureAlerts.value)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `multiple list fetches within refresh threshold`() {
        `when`(networkInfo.isConnected).thenReturn(true)

        `when`(alertApiClient.fetchAlertList(any())).thenAnswer {
            val alertListCallback = it.arguments[0] as AlertApiClient.AlertListCallback
            alertListCallback.onAlertListResponse(testTodayAlerts, testFutureAlerts)
        }

        val todayAlertsObserver = mock(Observer::class.java) as Observer<List<Alert>>
        val futureAlertsObserver = mock(Observer::class.java) as Observer<List<Alert>>

        alertsRepository.todayAlerts.observeForever(todayAlertsObserver)
        alertsRepository.futureAlerts.observeForever(futureAlertsObserver)

        alertsRepository.fetchAlerts()
        Thread.sleep(1000)
        alertsRepository.fetchAlerts()
        Thread.sleep(3000)
        alertsRepository.fetchAlerts()

        verify<Observer<List<Alert>>>(todayAlertsObserver, times(1)).onChanged(testTodayAlerts)
        verify<Observer<List<Alert>>>(futureAlertsObserver, times(1)).onChanged(testFutureAlerts)
        assertEquals(Status.Success, alertsRepository.status.value)
        assertNull(alertsRepository.error.value)


    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `multiple list fetches after refresh threshold`() {
        `when`(networkInfo.isConnected).thenReturn(true)

        `when`(alertApiClient.fetchAlertList(any())).thenAnswer {
            val alertListCallback = it.arguments[0] as AlertApiClient.AlertListCallback
            alertListCallback.onAlertListResponse(testTodayAlerts, testFutureAlerts)
        }

        val todayAlertsObserver = mock(Observer::class.java) as Observer<List<Alert>>
        val futureAlertsObserver = mock(Observer::class.java) as Observer<List<Alert>>

        alertsRepository.todayAlerts.observeForever(todayAlertsObserver)
        alertsRepository.futureAlerts.observeForever(futureAlertsObserver)

        alertsRepository.fetchAlerts()
        val sleepTime = Duration.ofSeconds(Config.Behavior.REFRESH_THRESHOLD_SEC.toLong())
        Thread.sleep(sleepTime.toMillis() + 100)
        alertsRepository.fetchAlerts()

        verify<Observer<List<Alert>>>(todayAlertsObserver, times(2)).onChanged(testTodayAlerts)
        verify<Observer<List<Alert>>>(futureAlertsObserver, times(2)).onChanged(testFutureAlerts)
        assertEquals(Status.Success, alertsRepository.status.value)
        assertNull(alertsRepository.error.value)

    }

    @Test
    fun `error returned from AlertApiClient is VolleyError`() {
        `when`(networkInfo.isConnected).thenReturn(true)

        `when`(alertApiClient.fetchAlertList(any())).thenAnswer {
            val alertListCallback = it.arguments[0] as AlertApiClient.AlertListCallback
            alertListCallback.onError(VolleyError())
        }

        alertsRepository.fetchAlerts()

        assertTrue(alertsRepository.error.value is AlertsRepository.Error.NetworkError)
        assertNull(alertsRepository.todayAlerts.value)
        assertNull(alertsRepository.futureAlerts.value)
        assertEquals(Status.Error, alertsRepository.status.value)
    }

    @Test
    fun `error returned from AlertApiClient is JSONException`() {
        `when`(networkInfo.isConnected).thenReturn(true)

        `when`(alertApiClient.fetchAlertList(any())).thenAnswer {
            val alertListCallback = it.arguments[0] as AlertApiClient.AlertListCallback
            alertListCallback.onError(JSONException("Mocked JSON Exception"))
        }

        alertsRepository.fetchAlerts()

        assertTrue(alertsRepository.error.value is AlertsRepository.Error.DataError)
        assertNull(alertsRepository.todayAlerts.value)
        assertNull(alertsRepository.futureAlerts.value)
        assertEquals(Status.Error, alertsRepository.status.value)
    }

    @Test
    fun `error returned from AlertApiClient is something else`() {
        `when`(networkInfo.isConnected).thenReturn(true)

        `when`(alertApiClient.fetchAlertList(any())).thenAnswer {
            val alertListCallback = it.arguments[0] as AlertApiClient.AlertListCallback
            alertListCallback.onError(Exception())
        }

        alertsRepository.fetchAlerts()

        assertTrue(alertsRepository.error.value is AlertsRepository.Error.GeneralError)
        assertNull(alertsRepository.todayAlerts.value)
        assertNull(alertsRepository.futureAlerts.value)
        assertEquals(Status.Error, alertsRepository.status.value)
    }

    @Test
    fun `successful alert detail fetch`() {
        `when`(alertApiClient.fetchAlert(any(), any(), any())).thenAnswer {
            val alertDetailCallback = it.arguments[2] as AlertApiClient.AlertDetailCallback
            alertDetailCallback.onAlertResponse(testAlert)
        }

        val alertLiveData = alertsRepository.fetchAlert(testAlert.id, AlertListType.Today)

        assertTrue(alertLiveData.value is Resource.Success)
        assertEquals(testAlert, (alertLiveData.value as Resource.Success).value)
    }

    @Test
    fun `unsuccessful alert detail fetch`() {
        `when`(alertApiClient.fetchAlert(any(), any(), any())).thenAnswer {
            val alertDetailCallback = it.arguments[2] as AlertApiClient.AlertDetailCallback
            alertDetailCallback.onError(Exception())
        }

        val alertLiveData = alertsRepository.fetchAlert(testAlert.id, AlertListType.Today)

        assertTrue(alertLiveData.value is Resource.Error)
    }

}