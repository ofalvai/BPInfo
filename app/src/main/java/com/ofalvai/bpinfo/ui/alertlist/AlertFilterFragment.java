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

package com.ofalvai.bpinfo.ui.alertlist;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.ofalvai.bpinfo.BpInfoApplication;
import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.model.RouteType;
import com.ofalvai.bpinfo.util.FabricUtils;

import java.util.HashSet;
import java.util.Set;

import timber.log.Timber;

/**
 * A DialogFragment containing a multi-choice list of RouteTypes.
 * The class maintains the selected RouteTypes.
 */
public class AlertFilterFragment extends DialogFragment {

    private static final String KEY_ALERT_LIST_TYPE = "alert_list_type";

    @Nullable
    private Set<RouteType> mSelectedRouteTypes = new HashSet<>();

    public interface AlertFilterListener {

        void onFilterChanged(@NonNull Set<RouteType> selectedTypes);

        /**
         * If the listener keeps a reference to this Fragment, it needs to know when to release
         * that reference.
         */
        void onFilterDismissed();
    }

    @Nullable
    private AlertFilterListener mFilterListener;

    /**
     * Type of alert list the dialog filters. This is needed because there are two AlertListFragments
     * in the ViewPager, and we need to remember which one was visible when the user opened the
     * filter dialog. Otherwise the dialog might filter the wrong alert list after state restore.
     */
    private AlertListType mAlertListType;

    @NonNull
    public static AlertFilterFragment newInstance(
            @NonNull AlertFilterListener listener,
            Set<RouteType> initialFilter,
            @NonNull AlertListType alertListType) {
        AlertFilterFragment fragment = new AlertFilterFragment();
        fragment.mFilterListener = listener;
        fragment.mSelectedRouteTypes = initialFilter;
        fragment.mAlertListType = alertListType;
        return fragment;
    }

    public void setFilterListener(@Nullable AlertFilterListener listener) {
        mFilterListener = listener;
    }

    public void setFilter(@Nullable Set<RouteType> filter) {
        mSelectedRouteTypes = filter;
    }

    @NonNull
    public AlertListType getAlertListType() {
        return mAlertListType;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                // Title
                .setTitle(R.string.filter_title)

                // Items
                .setMultiChoiceItems(R.array.route_types, defaultCheckedItems(),
                        new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        onItemClick(which, isChecked);
                    }
                })

                // Filter button
                .setPositiveButton(R.string.filter_positive_button,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mFilterListener != null && mSelectedRouteTypes != null) {
                            mFilterListener.onFilterChanged(mSelectedRouteTypes);

                            FabricUtils.INSTANCE.logFilterApplied(mSelectedRouteTypes);
                        }
                    }
                })

                // Cancel button
                .setNegativeButton(R.string.filter_negative_button,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(KEY_ALERT_LIST_TYPE, mAlertListType);
        // Note: The selected filters are stored in the fragment and restored here with setFilter()
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mAlertListType = (AlertListType) savedInstanceState.getSerializable(KEY_ALERT_LIST_TYPE);
        }
    }

    @Override
    public void onDestroyView() {
        if (mFilterListener != null) {
            mFilterListener.onFilterDismissed();
        }
        BpInfoApplication.Companion.getRefWatcher(getContext()).watch(this);
        super.onDestroyView();

    }

    /**
     * Returns the corresponding RouteType value to the menu item index.
     * The order is based on R.array.route_types
     */
    @Nullable
    private RouteType indexToRouteType(int index) {
        RouteType type;
        switch (index) {
            case 0:
                type = RouteType.BUS;
                break;
            case 1:
                type = RouteType.TRAM;
                break;
            case 2:
                type = RouteType.SUBWAY;
                break;
            case 3:
                type = RouteType.TROLLEYBUS;
                break;
            case 4:
                type = RouteType.RAIL;
                break;
            case 5:
                type = RouteType.FERRY;
                break;
            default:
                type = null;
        }

        return type;
    }

    private void onItemClick(int which, boolean isChecked) {
        RouteType type = indexToRouteType(which);

        if (type != null) {
            if (isChecked) {
                if (mSelectedRouteTypes != null) {
                    mSelectedRouteTypes.add(type);
                }
            } else {
                if (mSelectedRouteTypes != null) {
                    mSelectedRouteTypes.remove(type);
                }
            }
        } else {
            Timber.d("Unable to find a RouteType to index " + which);
        }
    }

    /**
     * Returns a boolean[] based on the selected route types. This is used to indicate already
     * checked items
     */
    @NonNull
    private boolean[] defaultCheckedItems() {
        CharSequence[] routeTypeArray = getResources().getTextArray(R.array.route_types);
        boolean[] checkedItems = new boolean[routeTypeArray.length];

        for (int i = 0; i < routeTypeArray.length; i++) {
            RouteType type = indexToRouteType(i);

            if (type != null) {
                checkedItems[i] = mSelectedRouteTypes != null && mSelectedRouteTypes.contains(type);
            } else {
                Timber.d("Unable to find a RouteType to index " + i);
            }
        }

        return checkedItems;
    }
}
