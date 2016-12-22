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
import com.ofalvai.bpinfo.util.UiUtils;
import com.ofalvai.bpinfo.util.Utils;
import com.wefika.flowlayout.FlowLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.ofalvai.bpinfo.util.LogUtils.LOGD;

public class AlertHolder extends RecyclerView.ViewHolder {

    private final String TAG = "AlertHolder";

    @BindView(R.id.list_item_alert_description)
    TextView mTitleTextView;

    @BindView(R.id.list_item_alert_date)
    TextView mDateTextView;

    @BindView(R.id.list_item_alert_route_icons_wrapper)
    FlowLayout mRouteIconsWrapper;

    @BindView(R.id.list_item_alert_recent)
    TextView mRecentTextView;

    private final AlertListType mAlertListType;

    private Alert mAlert;

    /**
     * List of currently displayed route icons. This list is needed in order to find visually
     * duplicate route data, and not to display them twice.
     */
    private final List<Route> mDisplayedRoutes = new ArrayList<>();

    public AlertHolder(View itemView, AlertListType alertListType) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        mAlertListType = alertListType;
    }

    public void bindAlert(@NonNull Alert alert, Context context) {
        mAlert = alert;

        // Title (header text)
        mTitleTextView.setText(alert.getHeader());

        // Start - end dates
        String dateString = UiUtils.alertDateFormatter(
                context, mAlert.getStart(), mAlert.getEnd()
        );
        mDateTextView.setText(dateString);

        // Route icons
        // First, removing any previously added icons
        mRouteIconsWrapper.removeAllViews();
        mDisplayedRoutes.clear();

        // There are alerts without affected routes, eg. announcements
        if (alert.getRouteIds() != null) {
            for (Route route : alert.getAffectedRoutes()) {
                // Some affected routes are visually identical to others in the list, no need
                // to diplay them again.
                if (!Utils.isRouteVisuallyDuplicate(route, mDisplayedRoutes)) {
                    mDisplayedRoutes.add(route);
                    UiUtils.addRouteIcon(context, mRouteIconsWrapper, route);

                    if (route.getType() == RouteType._OTHER_) {
                        LOGD(TAG, "Unknown route type: " + route.getShortName() + "(" + route.getId() + ")");
                    }
                }
            }
        }

        if (mAlertListType == AlertListType.ALERTS_TODAY) {
            mRecentTextView.setVisibility(Utils.isAlertRecent(alert) ? View.VISIBLE : View.GONE);
        } else {
            mRecentTextView.setVisibility(View.GONE);
        }
    }
}
