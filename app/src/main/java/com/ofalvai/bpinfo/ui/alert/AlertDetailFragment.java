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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ofalvai.bpinfo.BpInfoApplication;
import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.Route;
import com.ofalvai.bpinfo.ui.alertlist.AlertListContract;
import com.ofalvai.bpinfo.util.FabricUtils;
import com.ofalvai.bpinfo.util.UtilsKt;
import com.wefika.flowlayout.FlowLayout;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AlertDetailFragment extends BottomSheetDialogFragment {

    public static final String FRAGMENT_TAG = "alert_detail";

    private static final String ARG_ALERT_OBJECT = "alert_object";

    private Alert mAlert;

    @Nullable
    private AlertListContract.Presenter mPresenter;

    @BindView(R.id.alert_detail_title)
    TextView mTitleTextView;

    @BindView((R.id.alert_detail_date))
    TextView mDateTextView;

    @BindView(R.id.alert_detail_route_icons_wrapper)
    FlowLayout mRouteIconsLayout;

    @BindView(R.id.alert_detail_description)
    HtmlTextView mDescriptionTextView;

    @BindView(R.id.alert_detail_url)
    TextView mUrlTextView;

    @BindView(R.id.alert_detail_progress_bar)
    ContentLoadingProgressBar mProgressBar;

    @BindView(R.id.error_with_action)
    LinearLayout mErrorLayout;

    @BindView(R.id.error_message)
    TextView mErrorMessage;

    @BindView(R.id.error_action_button)
    Button mErrorButton;

    /**
     * List of currently displayed route icons. This list is needed in order to find visually
     * duplicate route data, and not to display them twice.
     */
    private final List<Route> mDisplayedRoutes = new ArrayList<>();

    public static AlertDetailFragment newInstance(@NonNull Alert alert,
                                                  @NonNull AlertListContract.Presenter presenter) {
        AlertDetailFragment fragment = new AlertDetailFragment();
        fragment.mPresenter = presenter;
        Bundle args = new Bundle();
        args.putSerializable(ARG_ALERT_OBJECT, alert);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mAlert = (Alert) getArguments().getSerializable(ARG_ALERT_OBJECT);
        }

        if (savedInstanceState != null) {
            mAlert = (Alert) savedInstanceState.getSerializable(ARG_ALERT_OBJECT);
        }

        FabricUtils.INSTANCE.logAlertContentView(mAlert);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(ARG_ALERT_OBJECT, mAlert);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Note: container is null because this is a subclass on DialogFragment
        View contentView = inflater.inflate(R.layout.fragment_alert_detail, container, false);
        ButterKnife.bind(this, contentView);

        displayAlert(mAlert);

        if (!mAlert.isPartial()) {
            mProgressBar.hide();
        }

        return contentView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // By default, the BottomSheetDialog changes the statusbar's color to black.
        // Found this solution here: https://code.google.com/p/android/issues/detail?id=202691
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Bottom sheets on tablets should have a smaller width than the screen width.
        int width = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_width);
        int actualWidth = width > 0 ? width : ViewGroup.LayoutParams.MATCH_PARENT;
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setLayout(actualWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BpInfoApplication.Companion.getRefWatcher(getContext()).watch(this);
    }

    public void updateAlert(final Alert alert) {
        mAlert = alert;
        mDisplayedRoutes.clear();
        mRouteIconsLayout.removeAllViews();

        // Updating views
        displayAlert(alert);

        // View animations
        // For some reason, ObjectAnimator doesn't work here (skips animation states, just shows the
        // last frame), we need to use ValueAnimators.
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new FastOutSlowInInterpolator());

        // We can't measure the TextView's height before a layout happens because of the setText() call
        // Note: even though displayAlert() was called earlier, the TextView's height is still 0.
        int heightEstimate = mDescriptionTextView.getLineHeight() *
                mDescriptionTextView.getLineCount() + 10;

        ValueAnimator descriptionHeight = ValueAnimator.ofInt(mDescriptionTextView.getHeight(), heightEstimate);
        descriptionHeight.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mDescriptionTextView.getLayoutParams().height = (int) animation.getAnimatedValue();
                mDescriptionTextView.requestLayout();
            }
        });
        descriptionHeight.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mDescriptionTextView.setAlpha(1.0f);
                mDescriptionTextView.setVisibility(View.VISIBLE);
            }
        });

        ValueAnimator descriptionAlpha = ValueAnimator.ofFloat(0, 1.0f);
        descriptionAlpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mDescriptionTextView.setAlpha((float) animation.getAnimatedValue());
            }
        });

        ValueAnimator progressHeight = ValueAnimator.ofInt(mProgressBar.getHeight(), 0);
        progressHeight.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mProgressBar.getLayoutParams().height = (int) animation.getAnimatedValue();
                mProgressBar.requestLayout();
            }
        });
        progressHeight.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressBar.hide();
            }
        });

        animatorSet.playTogether(progressHeight, descriptionHeight, descriptionAlpha);
        animatorSet.start();
    }

    public void onAlertUpdateFailed() {
        mProgressBar.hide();
        mErrorMessage.setText(R.string.error_alert_detail_load);
        mErrorButton.setText(R.string.label_retry);
        mErrorLayout.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.error_action_button)
    void clickErrorRetry() {
        if (mPresenter != null) {
            mErrorLayout.setVisibility(View.GONE);
            mProgressBar.show();
            mPresenter.fetchAlert(mAlert.getId());
        }
    }

    private void displayAlert(final Alert alert) {
        mTitleTextView.setText(alert.getHeader());

        String dateString = UtilsKt.formatDate(alert, getActivity());
        mDateTextView.setText(dateString);

        // There are alerts without affected routes, eg. announcements
        for (Route route : alert.getAffectedRoutes()) {
            // Some affected routes are visually identical to others in the list, no need
            // to diplay them again.
            if (!UtilsKt.isRouteVisuallyDuplicate(route, mDisplayedRoutes)) {
                mDisplayedRoutes.add(route);
                UtilsKt.addRouteIcon(getActivity(), mRouteIconsLayout, route);
            }
        }

        if (alert.getDescription() != null) {
            mDescriptionTextView.setHtml(alert.getDescription());
        }

        if (alert.getUrl() == null) {
            mUrlTextView.setVisibility(View.GONE);
        } else {
            mUrlTextView.setPaintFlags(mUrlTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            mUrlTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri url = Uri.parse(alert.getUrl());
                    UtilsKt.openCustomTab(getActivity(), url);
                    FabricUtils.INSTANCE.logAlertUrlClick(alert);
                }
            });
        }
    }
}
