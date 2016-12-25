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

package com.ofalvai.bpinfo.ui.alert;

import android.app.Dialog;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.Route;
import com.ofalvai.bpinfo.util.FabricUtils;
import com.ofalvai.bpinfo.util.FixedHtmlTextView;
import com.ofalvai.bpinfo.util.UiUtils;
import com.ofalvai.bpinfo.util.Utils;
import com.wefika.flowlayout.FlowLayout;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AlertDetailFragment extends BottomSheetDialogFragment {
    private static final String ARG_ALERT_OBJECT = "alert_object";

    private Alert mAlert;

    @BindView(R.id.alert_detail_title)
    TextView mTitleTextView;

    @BindView((R.id.alert_detail_date))
    TextView mDateTextView;

    @BindView(R.id.alert_detail_route_icons_wrapper)
    FlowLayout mRouteIconsLayout;

    @BindView(R.id.alert_detail_description)
    FixedHtmlTextView mDescriptionTextView;

    @BindView(R.id.alert_detail_url)
    TextView mUrlTextView;

    /**
     * List of currently displayed route icons. This list is needed in order to find visually
     * duplicate route data, and not to display them twice.
     */
    private final List<Route> mDisplayedRoutes = new ArrayList<>();

    private final BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback =
            new AlertDetailCallback();

    public static AlertDetailFragment newInstance(@NonNull Alert alert) {
        AlertDetailFragment fragment = new AlertDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ALERT_OBJECT, alert);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Sets up the fragment as a child of a BottomSheetDialogFragment
     */
    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        View contentView = View.inflate(getActivity(), R.layout.fragment_alert_detail, null);
        dialog.setContentView(contentView);

        View parentView = (View) contentView.getParent();
        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) parentView.getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAlert = (Alert) getArguments().getSerializable(ARG_ALERT_OBJECT);
        }

        FabricUtils.logAlertContentView(mAlert);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alert_detail, container, false);
        ButterKnife.bind(this, view);

        mTitleTextView.setText(mAlert.getHeader());

        String dateString = UiUtils.alertDateFormatter(getActivity(), mAlert.getStart(), mAlert.getEnd());
        mDateTextView.setText(dateString);

        // There are alerts without affected routes, eg. announcements
        for (Route route : mAlert.getAffectedRoutes()) {
            // Some affected routes are visually identical to others in the list, no need
            // to diplay them again.
            if (!Utils.isRouteVisuallyDuplicate(route, mDisplayedRoutes)) {
                mDisplayedRoutes.add(route);
                UiUtils.addRouteIcon(getActivity(), mRouteIconsLayout, route);
            }
        }

        if (mAlert.getDescription() != null) {
            mDescriptionTextView.setHtmlFromString(mAlert.getDescription(), new HtmlTextView.LocalImageGetter());
        }

        mUrlTextView.setPaintFlags(mUrlTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mUrlTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAlert.getUrl() != null) {
                    Uri url = Uri.parse(mAlert.getUrl());
                    UiUtils.openCustomTab(getActivity(), url);
                    FabricUtils.logAlertUrlClick(mAlert);
                }
            }
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // By default, the BottomSheetDialog changes the statusbar's color to black.
        // Found this solution here: https://code.google.com/p/android/issues/detail?id=202691
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        /**
         *  Bottom sheets on tablets should have a smaller width than the screen width.
         */
        int width = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_width);
        int actualWidth = width > 0 ? width : ViewGroup.LayoutParams.MATCH_PARENT;
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setLayout(actualWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private class AlertDetailCallback extends BottomSheetBehavior.BottomSheetCallback {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                // Dismiss dialog fragment
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

        }
    }
}
