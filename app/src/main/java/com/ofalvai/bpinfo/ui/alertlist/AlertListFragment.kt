/*
 * Copyright 2016 Olivér Falvai
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.ofalvai.bpinfo.ui.alertlist

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.ListUpdateCallback
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.text.Html
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.android.volley.VolleyError
import com.ofalvai.bpinfo.BpInfoApplication
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
import kotterknife.bindView
import java.util.*

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

    private lateinit var presenter: AlertListContract.Presenter

    private lateinit var alertAdapter: AlertAdapter

    private lateinit var alertListType: AlertListType

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
            restoredFilter = savedInstanceState.getSerializable(KEY_ACTIVE_FILTER) as MutableSet<RouteType>
        }

        presenter = AlertListPresenter(alertListType)
        presenter.attachView(this)

        restoredFilter?.let {
            presenter.setFilter(restoredFilter)
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
                filterFragment.selectedRouteTypes = presenter.getFilter()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        refreshLayout.setOnRefreshListener {
            initRefresh()
            Analytics.logManualRefresh(requireContext())
        }

        initRefresh()
        updateFilterWarning()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(KEY_ALERT_LIST_TYPE, alertListType)

        // Casting to HashSet, because Set is not serializable :(
        val filter = presenter.getFilter() as HashSet<RouteType>?
        outState.putSerializable(KEY_ACTIVE_FILTER, filter)
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

        val updating = presenter.updateIfNeeded()
        if (updating && requireActivity().hasNetworkConnection()) {
            setUpdating(true)
        }
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun onDestroyView() {
        BpInfoApplication.getRefWatcher(requireContext()).watch(this)
        super.onDestroyView()
    }

    /**
     * Updates the Toolbar's subtitle to the number of current items in the RecyclerView's Adapter
     */
    override fun updateSubtitle() {
        // Update subtitle only if the fragment is attached and visible to the user (not preloaded
        // by ViewPager)
        if (isAdded) {
            val count = alertAdapter.itemCount
            val subtitle = resources.getQuantityString(R.plurals.actionbar_subtitle_alert_count, count, count)
            val activity = activity as AppCompatActivity
            activity.supportActionBar?.let {
                it.subtitle = subtitle
            }
        }
    }

    override fun onFilterChanged(selectedTypes: MutableSet<RouteType>) {
        presenter.setFilter(selectedTypes)
        presenter.getAlertList()

        updateFilterWarning()
    }

    override fun onFilterDismissed() {}

    override fun displayAlerts(alerts: List<Alert>) {
        // It's possible that the network response callback thread executes this faster than
        // the UI thread attaching the fragment to the activity. In that case getResources() or
        // getString() would throw an exception.
        if (isAdded) {
            setErrorView(false, null)

            alertAdapter.updateAlertData(alerts, AlertListUpdateCallback())

            setUpdating(false)
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

    override fun displayNotice(noticeText: String) {
        noticeView.apply {
            visibility = View.VISIBLE
            text = Html.fromHtml(noticeText)
            setOnClickListener {
                val fragment = NoticeFragment.newInstance(noticeText)
                val transaction = requireActivity().supportFragmentManager
                        .beginTransaction()
                fragment.show(transaction, NOTICE_DIALOG_TAG)

                Analytics.logNoticeDialogView(context)
            }
        }
    }

    override fun removeNotice() {
        noticeView.visibility = View.GONE
    }

    override fun launchAlertDetail(alert: Alert) {
        displayAlertDetail(alert)

        presenter.fetchAlert(alert.id)
    }

    override fun displayAlertDetail(alert: Alert) {
        val alertDetailFragment = AlertDetailFragment.newInstance(alert, presenter)
        alertDetailFragment.show(requireFragmentManager(), AlertDetailFragment.FRAGMENT_TAG)
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
        alertAdapter = AlertAdapter(ArrayList(), requireContext(), this)
        alertRecyclerView.adapter = alertAdapter

        val layoutManager = LinearLayoutManager(activity)
        alertRecyclerView.layoutManager = layoutManager

        val decoration = DividerItemDecoration(context, layoutManager.orientation)
        alertRecyclerView.addItemDecoration(decoration)

        alertRecyclerView.setEmptyView(emptyView)
    }

    private fun initRefresh() {
        setUpdating(true)

        presenter.fetchAlertList()
        presenter.fetchNotice()

        presenter.setLastUpdate()
    }

    private fun setUpdating(updating: Boolean) {
        refreshLayout.isRefreshing = updating
    }

    private fun displayFilterDialog() {
        val initialFilter = presenter.getFilter() ?: mutableSetOf()

        val filterFragment = AlertFilterFragment.newInstance(this, initialFilter, alertListType)
        val transaction = requireFragmentManager().beginTransaction()
        filterFragment.show(transaction, FILTER_DIALOG_TAG)

        Analytics.logFilterDialogOpened(requireContext())
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
        val selectedTypes: MutableSet<RouteType> = presenter.getFilter() ?: return

        if (selectedTypes.isEmpty()) {
            filterWarningView.visibility = View.GONE
        } else {
            val typeList = selectedTypes.joinToString(separator = ", ") { it.getName(requireContext()) }
            filterWarningView.text = getString(R.string.filter_message, typeList)
            filterWarningView.visibility = View.VISIBLE
        }
    }

    /**
     * Scrolls to top and calls updateSubtitle() after the Alert list changed visually.
     */
    private inner class AlertListUpdateCallback : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            // Only update the toolbar if this fragment is currently selected in the ViewPager
            if (userVisibleHint) {
                updateSubtitle()
            }
            alertRecyclerView.smoothScrollToPosition(0)
        }

        override fun onRemoved(position: Int, count: Int) {
            if (userVisibleHint) {
                updateSubtitle()
            }

            // For some reason, the usual RecyclerView.smoothScrollToPosition(0) doesn't work here,
            // the list scrolls to the bottom, instead of the top.
            if (alertRecyclerView.layoutManager is LinearLayoutManager) {
                alertRecyclerView.layoutManager.scrollToPosition(0)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            if (userVisibleHint) {
                updateSubtitle()
            }
            alertRecyclerView.smoothScrollToPosition(0)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            if (userVisibleHint) {
                updateSubtitle()
            }
            alertRecyclerView.smoothScrollToPosition(0)
        }
    }
}
