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

package com.ofalvai.bpinfo.ui.alertlist.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.alertlist.AlertListType
import com.ofalvai.bpinfo.util.Analytics
import timber.log.Timber

/**
 * A DialogFragment containing a multi-choice list of RouteTypes.
 * The class maintains the selected RouteTypes.
 */
class AlertFilterFragment : DialogFragment() {

    companion object {

        const val KEY_ALERT_LIST_TYPE = "alert_list_type"

        @JvmStatic
        fun newInstance(listener: AlertFilterListener, initialFilter: MutableSet<RouteType>,
                        alertListType: AlertListType): AlertFilterFragment {
            return AlertFilterFragment().apply {
                filterListener = listener
                selectedRouteTypes = initialFilter
                this.alertListType = alertListType
            }
        }
    }

    var selectedRouteTypes: MutableSet<RouteType>? = HashSet()

    var filterListener: AlertFilterListener? = null

    /**
     * Type of alert list the dialog filters. This is needed because there are two AlertListFragments
     * in the ViewPager, and we need to remember which one was visible when the user opened the
     * filter dialog. Otherwise the dialog might filter the wrong alert list after state restore.
     */
    var alertListType: AlertListType? = null
        private set

    interface AlertFilterListener {

        fun onFilterChanged(selectedTypes: MutableSet<RouteType>)

        /**
         * If the listener keeps a reference to this Fragment, it needs to know when to release
         * that reference.
         */
        fun onFilterDismissed()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
                .setTitle(R.string.filter_title)
                .setMultiChoiceItems(R.array.route_types, defaultCheckedItems(), this::onItemClick)
                .setPositiveButton(R.string.filter_positive_button, this::onApplyClick)
                .setNegativeButton(R.string.filter_negative_button) { _, _ -> dismiss() }
                .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(KEY_ALERT_LIST_TYPE, alertListType)
        // Note: The selected filters are stored in the fragment and restored here with setFilter()
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.run {
            alertListType = getSerializable(KEY_ALERT_LIST_TYPE) as AlertListType
        }
    }

    override fun onDestroyView() {
        filterListener?.onFilterDismissed()
        super.onDestroyView()
    }

    /**
     * Returns the corresponding RouteType value to the menu item index.
     * The order is based on R.array.route_types
     */
    private fun indexToRouteType(index: Int): RouteType? {
        return when (index) {
            0 -> RouteType.BUS
            1 -> RouteType.TRAM
            2 -> RouteType.SUBWAY
            3 -> RouteType.TROLLEYBUS
            4 -> RouteType.RAIL
            5 -> RouteType.FERRY
            else -> null
        }
    }

    private fun onItemClick(dialogInterface: DialogInterface, which: Int, isChecked: Boolean) {
        val type = indexToRouteType(which)

        if (type != null) {
            if (isChecked) {
                selectedRouteTypes?.add(type)
            } else {
                selectedRouteTypes?.remove(type)
            }
        } else {
            Timber.d("Unable to find a RouteType to index %s", which)
        }
    }

    private fun onApplyClick(dialogInterface: DialogInterface, which: Int) {
        if (filterListener != null && selectedRouteTypes != null) {
            filterListener!!.onFilterChanged(selectedRouteTypes!!)

            Analytics.logFilterApplied(requireContext(), selectedRouteTypes!!)
        }
    }

    /**
     * Returns a boolean[] based on the selected route types. This is used to indicate already
     * checked items
     */
    private fun defaultCheckedItems(): BooleanArray {
        val routeTypeArray = resources.getTextArray(R.array.route_types)
        val checkedItems = BooleanArray(routeTypeArray.size)

        for (i in routeTypeArray.indices) {
            val type = indexToRouteType(i)

            if (type != null) {
                checkedItems[i] = selectedRouteTypes != null && selectedRouteTypes!!.contains(type)
            } else {
                Timber.d("Unable to find a RouteType to index %s", i)
            }
        }

        return checkedItems
    }
}
