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

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.VolleyError
import com.google.android.material.snackbar.Snackbar
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Alert
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.alert.AlertDetailFragment
import com.ofalvai.bpinfo.ui.alertlist.adapter.AlertAdapter
import com.ofalvai.bpinfo.ui.alertlist.dialog.AlertFilterFragment
import com.ofalvai.bpinfo.ui.alertlist.dialog.NoticeFragment
import com.ofalvai.bpinfo.ui.notifications.NotificationsActivity
import com.ofalvai.bpinfo.ui.settings.SettingsActivity
import com.ofalvai.bpinfo.util.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class AlertListFragment : Fragment(), AlertListContract.View, AlertFilterFragment.AlertFilterListener {

    companion object {

        const val KEY_ACTIVE_FILTER = "active_filter"

        const val KEY_ALERT_LIST_TYPE = "alert_list_type"

        const val FILTER_DIALOG_TAG = "filter_dialog"

        const val NOTICE_DIALOG_TAG = "notice_dialog"

        @JvmStatic
        fun newInstance(type: AlertListType): AlertListFragment {
            val fragment = AlertListFragment()
            fragment.alertListType = type
            return fragment
        }
    }

    private val viewModel by viewModel<AlertListViewModel>{ parametersOf(alertListType) }

    private val parentViewModel by sharedViewModel<AlertsViewModel>()

//    private val presenter: AlertListContract.Presenter by inject()

    private val analytics: Analytics by inject()

    private val alertAdapter = AlertAdapter(this)

    private lateinit var alertListType: AlertListType

    private var pendingNavigationAlertId: String? = null

    private val alertRecyclerView: EmptyRecyclerView by bindView(R.id.alerts_recycler_view)
    private val refreshLayout: SwipeRefreshLayout by bindView(R.id.alerts_swipe_refresh_layout)
    private val errorLayout: LinearLayout by bindView(R.id.error_with_action)
    private val filterWarningView: TextView by bindView(R.id.alert_list_filter_active_message)
    private val emptyView: TextView by bindView(R.id.empty_view)
    private val noticeView: TextView by bindView(R.id.alert_list_notice)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var restoredFilter: MutableSet<RouteType>? = null

        if (savedInstanceState != null) {
            alertListType = savedInstanceState.getSerializable(KEY_ALERT_LIST_TYPE) as AlertListType
            restoredFilter = savedInstanceState.getSerializable(KEY_ACTIVE_FILTER) as? MutableSet<RouteType>
        }

//        presenter.attachView(this)
//        presenter.alertListType = alertListType

        restoredFilter?.let {
            //            presenter.setFilter(restoredFilter)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_alert_list, container, false)
        setHasOptionsMenu(true)

        // If this fragment got recreated while the filter dialog was open, we need to update
        // the listener reference
        if (savedInstanceState != null) {
            val filterFragment = requireFragmentManager().findFragmentByTag(FILTER_DIALOG_TAG) as AlertFilterFragment?

            // Only attach to the filter fragment if it filters our type of list
            if (filterFragment != null && alertListType == filterFragment.alertListType) {
                filterFragment.filterListener = this
//                filterFragment.selectedRouteTypes = presenter.getFilter()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        refreshLayout.setOnRefreshListener {
            initRefresh()
            analytics.logManualRefresh()
        }

//        initRefresh()
        updateFilterWarning()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        observe(viewModel.alerts, this::displayAlerts)

        observe(viewModel.alertError) {
            when (it) {
                is AlertsRepository.Error.NetworkError -> displayNetworkError(it.volleyError)
                AlertsRepository.Error.DataError -> displayDataError()
                AlertsRepository.Error.GeneralError -> displayGeneralError()
            }
        }
        if (alertListType == AlertListType.ALERTS_TODAY) {
            observe(parentViewModel.notice, this::displayNotice)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(KEY_ALERT_LIST_TYPE, alertListType)

        // Casting to HashSet, because Set is not serializable :(
//        val filter = presenter.getFilter() as HashSet<RouteType>?
//        outState.putSerializable(KEY_ACTIVE_FILTER, filter)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_filter_alerts -> displayFilterDialog()
            R.id.menu_item_settings -> startActivity(SettingsActivity.newIntent(requireContext()))
            R.id.menu_item_notifications -> startActivity(NotificationsActivity.newIntent(requireContext()))
        }
        return true
    }

    override fun onStart() {
        super.onStart()

//        val updating = presenter.updateIfNeeded()
//        if (updating && requireActivity().hasNetworkConnection()) {
//            setUpdating(true)
//        }

        if (activity is AlertListActivity && alertListType == AlertListType.ALERTS_TODAY) {
            pendingNavigationAlertId = (activity!! as AlertListActivity).pendingNavigationAlertId
        }
    }

    override fun onDestroy() {
//        presenter.detachView()
        super.onDestroy()
    }

    override fun updateSubtitle() {
        updateSubtitle(alertAdapter.itemCount)
    }

    override fun onFilterChanged(selectedTypes: MutableSet<RouteType>) {
//        presenter.setFilter(selectedTypes)
//        presenter.getAlertList()

        updateFilterWarning()
    }

    override fun onFilterDismissed() {}

    override fun displayAlerts(alerts: List<Alert>) {
        // It's possible that the network response callback thread executes this faster than
        // the UI thread attaching the fragment to the activity. In that case getResources() or
        // getString() would throw an exception.
        if (isAdded) {
            setErrorView(false, null)

            alertAdapter.submitList(alerts)

            // Only update the toolbar if this fragment is currently selected in the ViewPager
            if (userVisibleHint) {
                updateSubtitle(alerts.size)
            }
            alertRecyclerView.smoothScrollToPosition(0)

            setUpdating(false)

            pendingNavigationAlertId?.let { id ->
                val alert: Alert? = alerts.find { it.id == id }
                if (alert != null) {
                    launchAlertDetail(alert)
                } else {
                    Timber.w("Pending alert navigation: no alert found for ID %s", id)
                }
            }
            pendingNavigationAlertId = null
            if (activity is AlertListActivity && alertListType == AlertListType.ALERTS_TODAY) {
                (activity!! as AlertListActivity).pendingNavigationAlertId = null
            }
        }
    }

    override fun displayNetworkError(error: VolleyError) {
        // It's possible that the network response callback thread executes this faster than
        // the UI thread attaching the fragment to the activity. In that case getResources() would
        // throw an exception.
        if (isAdded) {
            val errorMessage = resources.getString(error.toStringRes())
            setErrorView(true, errorMessage)
        }
    }

    override fun displayDataError() {
        if (isAdded) {
            setErrorView(true, getString(R.string.error_list_display))
        }
    }

    override fun displayGeneralError() {
        if (isAdded) {
            setErrorView(true, getString(R.string.error_list_display))
        }
    }

    override fun displayNoNetworkWarning() {
        if (isAdded) {
            setUpdating(false)

            val snackbar = Snackbar.make(refreshLayout, R.string.error_no_connection, Snackbar.LENGTH_LONG)
            snackbar.setAction(R.string.label_retry) { initRefresh() }
            snackbar.show()
        }
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

    override fun launchAlertDetail(alert: Alert) {
        displayAlertDetail(alert)

//        presenter.fetchAlert(alert.id)
    }

    override fun displayAlertDetail(alert: Alert) {
//        val alertDetailFragment = AlertDetailFragment.newInstance(alert, presenter)
//        alertDetailFragment.show(requireFragmentManager(), AlertDetailFragment.FRAGMENT_TAG)
    }

    override fun updateAlertDetail(alert: Alert) {
        val manager = requireFragmentManager()
        val fragment = manager.findFragmentByTag(AlertDetailFragment.FRAGMENT_TAG) as AlertDetailFragment?

        // It's possible that the presenter calls this method instantly, when the fragment is not
        // yet attached.
        fragment?.updateAlert(alert)
    }

    override fun displayAlertDetailError() {
        val manager = requireFragmentManager()
        val fragment = manager.findFragmentByTag(AlertDetailFragment.FRAGMENT_TAG) as AlertDetailFragment?

        // It's possible that the presenter calls this method instantly, when the fragment is not
        // yet attached.
        fragment?.onAlertUpdateFailed()
    }

    override fun getAlertListType() = alertListType

    private fun setupRecyclerView() {
        alertRecyclerView.adapter = alertAdapter

        val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
        alertRecyclerView.layoutManager = layoutManager

        val decoration =
                androidx.recyclerview.widget.DividerItemDecoration(context, layoutManager.orientation)
        alertRecyclerView.addItemDecoration(decoration)

        alertRecyclerView.setEmptyView(emptyView)
    }

    private fun initRefresh() {
        setUpdating(true)

//        presenter.fetchAlertList()
//        presenter.fetchNotice()

//        presenter.setLastUpdate()
    }

    private fun setUpdating(updating: Boolean) {
        refreshLayout.isRefreshing = updating
    }

    private fun displayFilterDialog() {
//        val initialFilter = presenter.getFilter() ?: mutableSetOf()

//        val filterFragment = AlertFilterFragment.newInstance(this, initialFilter, alertListType)
//        val transaction = requireFragmentManager().beginTransaction()
//        filterFragment.show(transaction, FILTER_DIALOG_TAG)

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
            setUpdating(false)

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
        // Might be null, because it gets called by onCreate() too
//        val selectedTypes: MutableSet<RouteType> = presenter.getFilter() ?: return

//        if (selectedTypes.isEmpty()) {
//            filterWarningView.visibility = View.GONE
//        } else {
//            val typeList = selectedTypes.joinToString(separator = ", ") { it.getName(requireContext()) }
//            filterWarningView.text = getString(R.string.filter_message, typeList)
//            filterWarningView.visibility = View.VISIBLE
//        }
    }

    private fun updateSubtitle(count: Int) {
        // Update subtitle only if the fragment is attached and visible to the user (not preloaded
        // by ViewPager)
        if (isAdded) {
            val subtitle = resources.getQuantityString(R.plurals.actionbar_subtitle_alert_count, count, count)
            val activity = activity as AppCompatActivity
            activity.supportActionBar?.let {
                it.subtitle = subtitle
            }
        }
    }
}
