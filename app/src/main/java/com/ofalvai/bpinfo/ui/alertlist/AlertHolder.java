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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.Route;
import com.ofalvai.bpinfo.model.RouteType;
import com.ofalvai.bpinfo.util.UtilsKt;
import com.wefika.flowlayout.FlowLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

class AlertHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.list_item_alert_description)
    TextView mTitleTextView;

    @BindView(R.id.list_item_alert_date)
    TextView mDateTextView;

    @BindView(R.id.list_item_alert_route_icons_wrapper)
    FlowLayout mRouteIconsWrapper;

    @BindView(R.id.list_item_alert_recent)
    TextView mRecentTextView;

    private final AlertListType mAlertListType;

    /**
     * List of currently displayed route icons. This list is needed in order to find visually
     * duplicate route data, and not to display them twice.
     */
    private final List<Route> mDisplayedRoutes = new ArrayList<>();

    AlertHolder(View itemView, AlertListType alertListType) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        mAlertListType = alertListType;
    }

    void bindAlert(@NonNull Alert alert, Context context) {

        // Title (header text)
        mTitleTextView.setText(alert.getHeader());

        // Start - end dates
        String dateString = UtilsKt.formatDate(alert, context);
        mDateTextView.setText(dateString);

        // Route icons
        // First, removing any previously added icons
        mRouteIconsWrapper.removeAllViews();
        mDisplayedRoutes.clear();

        // There are alerts without affected routes, eg. announcements
        for (Route route : alert.getAffectedRoutes()) {
            // Some affected routes are visually identical to others in the list, no need
            // to diplay them again.
            if (!UtilsKt.isRouteVisuallyDuplicate(route, mDisplayedRoutes)) {
                mDisplayedRoutes.add(route);
                UtilsKt.addRouteIcon(context, mRouteIconsWrapper, route);

                if (route.getType() == RouteType._OTHER_) {
                    Timber.d("Unknown route type: " + route.getShortName() + "(" + route.getId() + ")");
                }
            }
        }

        if (mAlertListType == AlertListType.ALERTS_TODAY) {
            mRecentTextView.setVisibility(UtilsKt.isRecent(alert) ? View.VISIBLE : View.GONE);
        } else {
            mRecentTextView.setVisibility(View.GONE);
        }
    }
}
