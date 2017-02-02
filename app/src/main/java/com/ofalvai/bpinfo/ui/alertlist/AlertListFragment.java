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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.ofalvai.bpinfo.BpInfoApplication;
import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.RouteType;
import com.ofalvai.bpinfo.ui.alert.AlertDetailFragment;
import com.ofalvai.bpinfo.ui.settings.SettingsActivity;
import com.ofalvai.bpinfo.util.EmptyRecyclerView;
import com.ofalvai.bpinfo.util.FabricUtils;
import com.ofalvai.bpinfo.util.SimpleDividerItemDecoration;
import com.ofalvai.bpinfo.util.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AlertListFragment extends Fragment implements AlertListContract.View,
        AlertFilterFragment.AlertFilterListener {

    private static final String TAG = "AlertListFragment";

    private static final String KEY_ACTIVE_FILTER = "active_filter";

    private static final String KEY_ALERT_LIST_TYPE = "alert_list_type";

    private static final String FILTER_DIALOG_TAG = "filter_dialog";

    private static final String NOTICE_DIALOG_TAG = "notice_dialog";

    private AlertListContract.Presenter mPresenter;

    @Nullable
    private AlertAdapter mAlertAdapter;

    private AlertListType mAlertListType;

    @Nullable
    private AlertFilterFragment mFilterFragment;

    @BindView(R.id.alerts_recycler_view)
    EmptyRecyclerView mAlertRecyclerView;

    @BindView(R.id.alerts_swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @BindView(R.id.error_with_action)
    LinearLayout mErrorLayout;

    @BindView(R.id.alert_list_filter_active_message)
    TextView mFilterWarningView;

    @BindView(R.id.empty_view)
    TextView mEmptyView;

    @BindView(R.id.alert_list_notice)
    TextView mNoticeView;

    public static AlertListFragment newInstance(@NonNull AlertListType type) {
        AlertListFragment fragment = new AlertListFragment();
        fragment.mAlertListType = type;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Set<RouteType> restoredFilter = null;

        if (savedInstanceState != null) {
            mAlertListType = (AlertListType) savedInstanceState.getSerializable(KEY_ALERT_LIST_TYPE);
            restoredFilter = (HashSet<RouteType>) savedInstanceState.getSerializable(KEY_ACTIVE_FILTER);
        }

        mPresenter = new AlertListPresenter(mAlertListType);
        mPresenter.attachView(this);

        if (restoredFilter != null) {
            mPresenter.setFilter(restoredFilter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_alert_list, container, false);
        ButterKnife.bind(this, view);

        setupRecyclerView();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initRefresh();
                FabricUtils.logManualRefresh();
            }
        });

        // If this fragment got recreated while the filter dialog was open, we need to update
        // the listener reference
        if (savedInstanceState != null) {
            mFilterFragment = (AlertFilterFragment) getFragmentManager().findFragmentByTag(FILTER_DIALOG_TAG);

            // Only attach to the filter fragment if it filters our type of list
            if (mFilterFragment != null && mAlertListType == mFilterFragment.getAlertListType()) {
                mFilterFragment.setFilterListener(this);
                mFilterFragment.setFilter(mPresenter.getFilter());
            }
        }

        return view;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        initRefresh();
        updateFilterWarning();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(KEY_ALERT_LIST_TYPE, mAlertListType);

        // Casting to HashSet, because Set is not serializable :(
        //noinspection CollectionDeclaredAsConcreteClass
        HashSet<RouteType> filter = (HashSet<RouteType>) mPresenter.getFilter();

        outState.putSerializable(KEY_ACTIVE_FILTER, filter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_filter_alerts:
                displayFilterDialog();
                break;
            case R.id.menu_item_settings:
                startActivity(SettingsActivity.newIntent(getContext()));
                break;
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        mPresenter.updateIfNeeded();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mPresenter != null) {
            mPresenter.detachView();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BpInfoApplication.getRefWatcher(getContext()).watch(this);
    }

    /**
     * Updates the Toolbar's subtitle to the number of current items in the RecyclerView's Adapter
     */
    @Override
    public void updateSubtitle() {
        if (mAlertAdapter != null && isAdded()) {
            int count = mAlertAdapter.getItemCount();
            String subtitle = getResources().getQuantityString(R.plurals.actionbar_subtitle_alert_count, count, count);
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setSubtitle(subtitle);
            }
        }
    }

    @Override
    public void onFilterChanged(@NonNull Set<RouteType> selectedTypes) {
        // Prefent leaking the fragment
        mFilterFragment = null;

        mPresenter.setFilter(selectedTypes);
        mPresenter.getAlertList();

        updateFilterWarning();
    }

    @Override
    public void onFilterDismissed() {
        // Prevents leaking the fragment
        mFilterFragment = null;
    }

    @Override
    public void displayAlerts(@NonNull List<Alert> alerts) {
        // It's possible that the network response callback thread executes this faster than
        // the UI thread attaching the fragment to the activity. In that case getResources() or
        // getString() would throw an exception.
        if (isAdded()) {
            setErrorView(false, null);

            if (mAlertAdapter == null) {
                mAlertAdapter = new AlertAdapter(alerts, getContext(), this);
                mAlertRecyclerView.setAdapter(mAlertAdapter);
            } else {
                mAlertAdapter.updateAlertData(alerts);
                mAlertAdapter.notifyDataSetChanged();
                mAlertRecyclerView.smoothScrollToPosition(0);
            }

            // Only update the subtitle if the fragment is visible (and not preloading by ViewPager)
            if (getUserVisibleHint()) {
                updateSubtitle();
            }

            setUpdating(false);
        }
    }

    @Override
    public void displayNetworkError(@NonNull VolleyError error) {
        // It's possible that the network response callback thread executes this faster than
        // the UI thread attaching the fragment to the activity. In that case getResources() would
        // throw an exception.
        if (isAdded()) {
            int errorMessageId = Utils.volleyErrorTypeHandler(error);
            String errorMessage = getResources().getString(errorMessageId);

            setErrorView(true, errorMessage);
        }
    }

    @Override
    public void displayDataError() {
        if (isAdded()) {
            setErrorView(true, getString(R.string.error_list_display));
        }
    }

    @Override
    public void displayGeneralError() {
        if (isAdded()) {
            setErrorView(true, getString(R.string.error_list_display));
        }
    }

    @Override
    public void setUpdating(final boolean updating) {
        mSwipeRefreshLayout.setRefreshing(updating);
    }

    @Override
    public void displayNoNetworkWarning() {
        if (isAdded()) {
            setUpdating(false);

            Snackbar snackbar =
                    Snackbar.make(mSwipeRefreshLayout, R.string.error_no_connection, Snackbar.LENGTH_LONG);

            snackbar.setAction(R.string.label_retry, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initRefresh();
                }
            });
            snackbar.show();
        }
    }

    @Override
    public void displayNotice(final String noticeText) {
        mNoticeView.setVisibility(View.VISIBLE);
        mNoticeView.setText(Html.fromHtml(noticeText));
        mNoticeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NoticeFragment fragment = NoticeFragment.newInstance(noticeText);
                FragmentTransaction transaction = getActivity().getSupportFragmentManager()
                        .beginTransaction();
                fragment.show(transaction, NOTICE_DIALOG_TAG);

                FabricUtils.logNoticeDialogView();
            }
        });
    }

    @Override
    public void removeNotice() {
        mNoticeView.setVisibility(View.GONE);
    }

    public void launchAlertDetail(@NonNull Alert alert) {
        displayAlertDetail(alert);

        mPresenter.fetchAlert(alert.getId());
    }

    @Override
    public void displayAlertDetail(@NonNull Alert alert) {
        AlertDetailFragment alertDetailFragment = AlertDetailFragment.newInstance(alert, mPresenter);
        alertDetailFragment.show(getActivity().getSupportFragmentManager(), AlertDetailFragment.FRAGMENT_TAG);
    }

    @Override
    public void updateAlertDetail(@NonNull Alert alert) {
        FragmentManager manager = getActivity().getSupportFragmentManager();
        AlertDetailFragment fragment = (AlertDetailFragment)
                manager.findFragmentByTag(AlertDetailFragment.FRAGMENT_TAG);

        // It's possible that the presenter calls this method instantly, when the fragment is not
        // yet attached.
        if (fragment != null) {
            fragment.updateAlert(alert);
        }
    }

    @Override
    public void displayAlertDetailError() {
        FragmentManager manager = getActivity().getSupportFragmentManager();
        AlertDetailFragment fragment = (AlertDetailFragment)
                manager.findFragmentByTag(AlertDetailFragment.FRAGMENT_TAG);

        // It's possible that the presenter calls this method instantly, when the fragment is not
        // yet attached.
        if (fragment != null) {
            fragment.onAlertUpdateFailed();
        }
    }

    @Override
    public AlertListType getAlertListType() {
        return mAlertListType;
    }

    private void setupRecyclerView() {
        mAlertAdapter = new AlertAdapter(new ArrayList<Alert>(), getActivity(), this);
        mAlertRecyclerView.setAdapter(mAlertAdapter);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mAlertRecyclerView.setLayoutManager(layoutManager);

        mAlertRecyclerView.addItemDecoration(
                new SimpleDividerItemDecoration(getActivity()));

        mAlertRecyclerView.setEmptyView(mEmptyView);

        // Fixing overscroll effect at the bottom of the list. If a SwipeRefreshLayout is the parent
        // of the RecyclerView, we need to disable that when the user scrolls down.
        mAlertRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                try {
                    int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                    mSwipeRefreshLayout.setEnabled(firstVisiblePosition == 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initRefresh() {
        setUpdating(true);

        mPresenter.fetchAlertList();
        mPresenter.fetchNotice();

        mPresenter.setLastUpdate();
    }

    private void displayFilterDialog() {
        mFilterFragment = AlertFilterFragment.newInstance(this,
                mPresenter.getFilter(), mAlertListType);
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        mFilterFragment.show(transaction, FILTER_DIALOG_TAG);

        FabricUtils.logFilterDialogOpened();
    }

    /**
     * Displays or hides the error view. If displaying, it also sets the retry button's event listener
     * and the error message.
     *
     * @param state true to display, false to hide
     */
    private void setErrorView(boolean state, String errorMessage) {
        if (state) {
            setUpdating(false);

            mAlertRecyclerView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);

            TextView errorMessageView = (TextView) mErrorLayout.findViewById(R.id.error_message);
            Button refreshButton = (Button) mErrorLayout.findViewById(R.id.error_action_button);

            if (!refreshButton.hasOnClickListeners()) {
                refreshButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        initRefresh();
                    }
                });
            }
            refreshButton.setText(getString(R.string.label_retry));

            mErrorLayout.setVisibility(View.VISIBLE);
            errorMessageView.setText(errorMessage);
        } else {
            mAlertRecyclerView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.VISIBLE);
            mErrorLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the filter warning bar above the list based on the currently selected RouteTypes.
     * Hides the bar if nothing is selected as filter.
     */
    private void updateFilterWarning() {
        // Might be null, because it gets called by onCreate() too
        if (mFilterWarningView != null && mPresenter != null) {
            Set<RouteType> selectedTypes = mPresenter.getFilter();

            if (selectedTypes == null) {
                return;
            }

            if (selectedTypes.isEmpty()) {
                mFilterWarningView.setVisibility(View.GONE);
            } else {
                StringBuilder typeList = new StringBuilder();
                final String separator = ", ";
                for (RouteType type : selectedTypes) {
                    typeList.append(Utils.routeTypeToString(getContext(), type));
                    typeList.append(separator);
                }

                // Removing the last separator at the end of the list
                String completeString = getString(R.string.filter_message, typeList.toString());
                if (completeString.endsWith(separator)) {
                    completeString = completeString.substring(0, completeString.length() - separator.length());
                }

                mFilterWarningView.setText(completeString);
                mFilterWarningView.setVisibility(View.VISIBLE);
            }
        }
    }
}
