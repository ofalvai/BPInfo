package com.example.bkkinfoplus.ui.alert;

import android.app.Dialog;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bkkinfoplus.R;
import com.example.bkkinfoplus.model.Alert;
import com.example.bkkinfoplus.model.Route;
import com.example.bkkinfoplus.ui.UiUtils;
import com.wefika.flowlayout.FlowLayout;

import org.sufficientlysecure.htmltextview.HtmlTextView;

public class AlertDetailFragment extends BottomSheetDialogFragment {
    private static final String ARG_ALERT_OBJECT = "alert_object";

    private Alert mAlert;

    private TextView mTitleTextView;
    private TextView mDateTextView;
    private TextView mDateModifiedTextView;
    private FlowLayout mRouteIconsLayout;
    private HtmlTextView mDescriptionTextView;
    private TextView mUrlTextView;

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback =
            new AlertDetailCallback();

    public static AlertDetailFragment newInstance(Alert alert) {
        AlertDetailFragment fragment = new AlertDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ALERT_OBJECT, alert);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Sets up the fragment as a child of a BottomSheetDialogFragment
     * @param dialog
     * @param style
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

        if (behavior != null && behavior instanceof BottomSheetBehavior ) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAlert = (Alert) getArguments().getSerializable(ARG_ALERT_OBJECT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alert_detail, container, false);
        mTitleTextView = (TextView) view.findViewById(R.id.alert_detail_title);
        mDateTextView = (TextView) view.findViewById(R.id.alert_detail_date);
        mDateModifiedTextView = (TextView) view.findViewById(R.id.alert_detail_date_modified);
        mRouteIconsLayout = (FlowLayout) view.findViewById(R.id.alert_detail_route_icons_wrapper);
        mDescriptionTextView = (HtmlTextView) view.findViewById(R.id.alert_detail_description);
        mUrlTextView = (TextView) view.findViewById(R.id.alert_detail_url);

        mTitleTextView.setText(mAlert.getHeader());

        String dateString = UiUtils.alertDateFormatter(getActivity(), mAlert.getStart(), mAlert.getEnd());
        mDateTextView.setText(dateString);

        //DateFormat dateFormat = new SimpleDateFormat(UiUtils.DATE_FORMAT);
        //String dateModifiedString = dateFormat.format(new Date(mAlert.getTimestamp() * 1000));
        //mDateModifiedTextView.setText(
        //        getResources().getString(R.string.alert_label_date_modified, dateModifiedString)
        //);
        // TODO

        if (mAlert.getRouteIds() != null) {
            for (Route route : mAlert.getAffectedRoutes()) {
                UiUtils.addRouteIcon(getActivity(), mRouteIconsLayout, route);
            }
        }

        mDescriptionTextView.setHtmlFromString(mAlert.getDescription(), new HtmlTextView.LocalImageGetter());

        mUrlTextView.setPaintFlags(mUrlTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mUrlTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAlert.getUrl() != null) {
                    Uri url = Uri.parse(mAlert.getUrl());
                    UiUtils.openCustomTab(getActivity(), url);
                }
            }
        });

        return view;
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
