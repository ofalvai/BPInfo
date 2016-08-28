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

import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.model.RouteType;
import com.ofalvai.bpinfo.util.FabricUtils;

import java.util.HashSet;
import java.util.Set;

import static com.ofalvai.bpinfo.util.LogUtils.LOGD;

/**
 * A DialogFragment containing a multi-choice list of RouteTypes.
 * The class maintains the selected RouteTypes.
 */
public class AlertFilterFragment extends DialogFragment {

    private static final String TAG = "AlertFilterFragment";

    @Nullable
    private Set<RouteType> mSelectedRouteTypes = new HashSet<>();

    public interface AlertFilterListener {
        void onFilterChanged(@NonNull Set<RouteType> selectedTypes);
    }

    @Nullable
    private AlertFilterListener mFilterListener;

    @NonNull
    public static AlertFilterFragment newInstance(AlertFilterListener listener, Set<RouteType> initialFilter) {
        AlertFilterFragment fragment = new AlertFilterFragment();
        fragment.mFilterListener = listener;
        fragment.mSelectedRouteTypes = initialFilter;
        return fragment;
    }

    public void setFilterListener(@Nullable AlertFilterListener listener) {
        mFilterListener = listener;
    }

    public void setFilter(@Nullable Set<RouteType> filter) {
        mSelectedRouteTypes = filter;
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

                            FabricUtils.logFilterApplied(mSelectedRouteTypes);
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

    /**
     * Returns the corresponding RouteType value to the menu item index.
     * The order is based on R.array.route_types
     */
    @Nullable
    private RouteType indexToRouteType(int index) {
        RouteType type;
        switch (index) {
            case 0:
                type = RouteType.SUBWAY;
                break;
            case 1:
                type = RouteType.TRAM;
                break;
            case 2:
                type = RouteType.TROLLEYBUS;
                break;
            case 3:
                type = RouteType.BUS;
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

    /**
     * Returns the corresponding menu item index to the RouteType value.
     * The order is based on R.array.route_types
     */
    private int routeTypeToIndex(RouteType type) {
        int index;
        switch(type) {
            case SUBWAY:
                index = 0;
                break;
            case TRAM:
                index = 1;
                break;
            case TROLLEYBUS:
                index = 2;
                break;
            case BUS:
                index = 3;
                break;
            case RAIL:
                index = 4;
                break;
            case FERRY:
                index = 5;
                break;
            default:
                index = -1;
        }

        return index;
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
            LOGD(TAG, "Unable to find a RouteType to index " + which);
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
                LOGD(TAG, "Unable to find a RouteType to index " + i);
            }
        }

        return checkedItems;
    }
}
