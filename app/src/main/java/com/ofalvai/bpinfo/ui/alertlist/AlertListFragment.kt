/*
 * Copyright 2018 Olivér Falvai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ofalvai.bpinfo.ui.alertlist

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.VolleyError
import com.google.android.material.snackbar.Snackbar
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.Resource
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.model.Status
import com.ofalvai.bpinfo.repository.AlertsRepository
import com.ofalvai.bpinfo.ui.alertdetail.AlertDetailFragment
import com.ofalvai.bpinfo.ui.alertlist.adapter.AlertAdapter
import com.ofalvai.bpinfo.ui.alertlist.dialog.AlertFilterFragment
import com.ofalvai.bpinfo.ui.alertlist.dialog.NoticeFragment
import com.ofalvai.bpinfo.ui.notifications.NotificationsActivity
import com.ofalvai.bpinfo.ui.settings.PreferencesActivity
import com.ofalvai.bpinfo.util.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class AlertListFragment : Fragment(), AlertFilterFragment.AlertFilterListener {

    companion object {
        const val KEY_ALERT_LIST_TYPE = "alert_list_type"
        const val FILTER_DIALOG_TAG = "filter_dialog"
        const val NOTICE_DIALOG_TAG = "notice_dialog"

        fun newInstance(type: AlertListType): AlertListFragment {
            val fragment = AlertListFragment()
            fragment.alertListType = type
            return fragment
        }
    }

    private val viewModel by viewModel<AlertListViewModel> { parametersOf(alertListType) }

    private val parentViewModel by sharedViewModel<AlertsViewModel>()

    private val analytics: Analytics by inject()

    private lateinit var alertAdapter: AlertAdapter

    private lateinit var alertListType: AlertListType

    private var pendingNavigationAlertId: String? = null

    private val alertRecyclerView: EmptyRecyclerView by bindView(R.id.alerts_recycler_view)
    private val refreshLayout: SwipeRefreshLayout by bindView(R.id.alerts_swipe_refresh_layout)
    private val errorLayout: LinearLayout by bindView(R.id.error_with_action)
    private val filterWarningView: TextView by bindView(R.id.alert_list_filter_active_message)
    private val emptyView: TextView by bindView(R.id.empty_view)
    private val noticeView: TextView by bindView(R.id.alert_list_notice)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alert_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            alertListType = savedInstanceState.getSerializable(KEY_ALERT_LIST_TYPE) as AlertListType
        }

        // If this fragment got recreated while the filter dialog was open, we need to update
        // the listener reference and the current filter
        if (savedInstanceState != null) {
            val filterFragment =
                requireFragmentManager().findFragmentByTag(FILTER_DIALOG_TAG) as AlertFilterFragment?

            // Only attach to the filter fragment if it filters our type of list
            if (filterFragment != null && alertListType == filterFragment.alertListType) {
                filterFragment.filterListener = this
                filterFragment.selectedRouteTypes = viewModel.activeFilter
            }
        }

        setupRecyclerView()

        setHasOptionsMenu(true)

        refreshLayout.setOnRefreshListener {
            initRefresh()
            analytics.logManualRefresh()
        }

        updateFilterWarning()

        observeEvents()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_ALERT_LIST_TYPE, alertListType)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_filter_alerts -> displayFilterDialog()
            R.id.menu_item_settings -> startActivity(Intent(context, PreferencesActivity::class.java))
            R.id.menu_item_notifications -> startActivity(
                NotificationsActivity.newIntent(requireContext())
            )
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        if (activity is AlertListActivity && alertListType == AlertListType.Today) {
            pendingNavigationAlertId = (activity!! as AlertListActivity).pendingNavigationAlertId
        }
    }

    fun updateSubtitle() {
        updateSubtitle(alertAdapter.itemCount)
    }

    override fun onFilterChanged(selectedTypes: MutableSet<RouteType>) {
        viewModel.activeFilter = selectedTypes

        updateFilterWarning()
    }

    private fun observeEvents() {
        observe(viewModel.alerts, this::displayAlerts)

        observe(viewModel.alertError) {
            when (it) {
                is AlertsRepository.Error.NetworkError -> displayNetworkError(it.volleyError)
                AlertsRepository.Error.DataError -> displayDataError()
                AlertsRepository.Error.GeneralError -> displayGeneralError()
            }
        }

        observe(viewModel.status) {
            when (it) {
                Status.Loading -> setUpdating(true)
                else -> setUpdating(false)
            }
        }

        observe(viewModel.noConnectionWarning) { displayNoNetworkWarning() }

        if (alertListType == AlertListType.Today) {
            observe(parentViewModel.notice, this::displayNotice)
        }
    }

    private fun launchAlertDetail(alert: Alert) {
        displayAlertDetail(alert)

        val alertLiveData = viewModel.fetchAlert(alert.id)
        observe(alertLiveData) {
            when (it) {
                is Resource.Success -> updateAlertDetail(it.value)
                is Resource.Error -> displayAlertDetailError()
                // is Resource.Loading -> AlertDetailFragment handles its loading state
            }
        }
    }

    private fun displayAlerts(alerts: List<Alert>) {
        setErrorView(false, null)

        alertAdapter.submitList(alerts)

        // Only update the toolbar if this fragment is currently selected in the ViewPager
        if (userVisibleHint) {
            updateSubtitle(alerts.size)
        }
        alertRecyclerView.smoothScrollToPosition(0)

        pendingNavigationAlertId?.let { id ->
            val alert: Alert? = alerts.find { it.id == id }
            if (alert != null) {
                launchAlertDetail(alert)
            } else {
                Timber.w("Pending alert navigation: no alert found for ID %s", id)
            }
        }
        pendingNavigationAlertId = null
        if (activity is AlertListActivity && alertListType == AlertListType.Today) {
            (activity!! as AlertListActivity).pendingNavigationAlertId = null
        }
    }

    private fun displayNetworkError(error: VolleyError) {
        val errorMessage = resources.getString(error.toStringRes())
        setErrorView(true, errorMessage)
    }

    private fun displayDataError() {
        setErrorView(true, getString(R.string.error_list_display))
    }

    private fun displayGeneralError() {
        setErrorView(true, getString(R.string.error_list_display))
    }

    private fun displayNoNetworkWarning() {
        setUpdating(false)

        val snackbar = Snackbar
            .make(refreshLayout, R.string.error_no_connection, Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.label_retry) { initRefresh() }
        snackbar.show()
    }

    private fun displayNotice(noticeText: String?) {
        if (noticeText == null) {
            noticeView.hide()
        } else {
            noticeView.apply {
                show()
                text = HtmlCompat.fromHtml(noticeText, HtmlCompat.FROM_HTML_MODE_COMPACT)
                setOnClickListener {
                    val fragment = NoticeFragment.newInstance(noticeText)
                    val transaction = requireActivity().supportFragmentManager
                        .beginTransaction()
                    fragment.show(transaction, NOTICE_DIALOG_TAG)

                    analytics.logNoticeDialogView()
                }
            }
        }
    }

    /**
     * Displays the alert detail view.
     * If the alert object contains all required information, there's no need to call
     * updateAlertDetail() later, otherwise Alert.partial must be set to true.
     * @param alert data from a list item
     */
    private fun displayAlertDetail(alert: Alert) {
        val alertDetailFragment = AlertDetailFragment.newInstance(alert, alertListType)
        alertDetailFragment.show(requireFragmentManager(), AlertDetailFragment.FRAGMENT_TAG)
    }

    /**
     * Updates the alert detail view with the full alert data
     * @param alert data coming from the alert detail API call
     */
    private fun updateAlertDetail(alert: Alert) {
        val fragment = requireFragmentManager()
            .findFragmentByTag(AlertDetailFragment.FRAGMENT_TAG) as AlertDetailFragment?

        fragment?.updateAlert(alert)
    }

    private fun displayAlertDetailError() {
        val fragment = requireFragmentManager()
            .findFragmentByTag(AlertDetailFragment.FRAGMENT_TAG) as AlertDetailFragment?

        fragment?.onAlertUpdateFailed()
    }

    private fun setupRecyclerView() {
        alertAdapter = AlertAdapter(alertListType) { launchAlertDetail(it) }

        alertRecyclerView.adapter = alertAdapter

        val layoutManager = LinearLayoutManager(activity)
        alertRecyclerView.layoutManager = layoutManager

        val decoration = DividerItemDecoration(context, layoutManager.orientation)
        alertRecyclerView.addItemDecoration(decoration)

        alertRecyclerView.setEmptyView(emptyView)
    }

    private fun initRefresh() {
        // TODO: move SwipeRefreshLayout from Fragment to Activity
        viewModel.refresh()
        parentViewModel.fetchNotices()
    }

    private fun setUpdating(updating: Boolean) {
        refreshLayout.isRefreshing = updating
    }

    private fun displayFilterDialog() {
        val filterFragment = AlertFilterFragment.newInstance(
            this, viewModel.activeFilter, alertListType
        )
        val transaction = requireFragmentManager().beginTransaction()
        filterFragment.show(transaction, FILTER_DIALOG_TAG)

        analytics.logFilterDialogOpened()
    }

    /**
     * Displays or hides the error view. If displaying, it also sets the retry button's event listener
     * and the error message.
     *
     * @param state true to display, false to hide
     */
    private fun setErrorView(state: Boolean, errorMessage: String?) {
        if (state) {
            alertRecyclerView.visibility = View.GONE

            val errorMessageView = errorLayout.findViewById<TextView>(R.id.error_message)
            val refreshButton = errorLayout.findViewById<Button>(R.id.error_action_button)

            if (!refreshButton.hasOnClickListeners()) {
                refreshButton.setOnClickListener { initRefresh() }
            }
            refreshButton.text = getString(R.string.label_retry)

            errorLayout.visibility = View.VISIBLE
            errorMessageView.text = errorMessage
        } else {
            alertRecyclerView.visibility = View.VISIBLE
            errorLayout.visibility = View.GONE
        }
    }

    /**
     * Updates the filter warning bar above the list based on the currently selected RouteTypes.
     * Hides the bar if nothing is selected as filter.
     */
    private fun updateFilterWarning() {
        if (viewModel.activeFilter.isEmpty()) {
            filterWarningView.visibility = View.GONE
        } else {
            val typeList = viewModel.activeFilter.joinToString(separator = ", ") {
                it.getName(requireContext())
            }
            filterWarningView.text = getString(R.string.filter_message, typeList)
            filterWarningView.visibility = View.VISIBLE
        }
    }

    private fun updateSubtitle(count: Int) {
        // Update subtitle only if the fragment is attached and visible to the user (not preloaded
        // by ViewPager)
        if (isAdded) {
            val subtitle =
                resources.getQuantityString(R.plurals.actionbar_subtitle_alert_count, count, count)
            val activity = activity as AppCompatActivity
            activity.supportActionBar?.let {
                it.subtitle = subtitle
            }
        }
    }
}
