package com.example.bkkinfoplus.ui.alertlist;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.example.bkkinfoplus.R;
import com.example.bkkinfoplus.Utils;
import com.example.bkkinfoplus.model.Alert;
import com.example.bkkinfoplus.model.Route;
import com.example.bkkinfoplus.model.RouteType;
import com.example.bkkinfoplus.ui.EmptyRecyclerView;
import com.example.bkkinfoplus.ui.SimpleDividerItemDecoration;
import com.example.bkkinfoplus.ui.UiUtils;
import com.example.bkkinfoplus.ui.alert.AlertDetailFragment;
import com.example.bkkinfoplus.ui.settings.SettingsActivity;
import com.wefika.flowlayout.FlowLayout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class AlertListFragment extends Fragment
        implements AlertListPresenter.AlertInteractionListener, AlertFilterFragment.AlertFilterListener {

    private static final String TAG = "AlertListFragment";

    private static final String KEY_ACTIVE_FILTER = "active_filter";

    private EmptyRecyclerView mAlertRecyclerView;
    private AlertAdapter mAlertAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinearLayout mErrorLayout;
    private AlertFilterFragment mFilterFragment;
    private TextView mFilterWarningView;

    @Inject
    public AlertListPresenter mAlertListPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlertListPresenter = new AlertListPresenter(this);

        if (savedInstanceState != null) {
            @SuppressWarnings("unchecked")
            HashSet<RouteType> filter = (HashSet<RouteType>) savedInstanceState.getSerializable(KEY_ACTIVE_FILTER);
            onFilterChanged(filter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_alert_list, container, false);

        mErrorLayout = (LinearLayout) view.findViewById(R.id.error_with_action);
        mFilterWarningView = (TextView) view.findViewById(R.id.alert_list_filter_active_message);

        mAlertRecyclerView = (EmptyRecyclerView) view.findViewById(R.id.alerts_recycler_view);
        mAlertRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAlertRecyclerView.addItemDecoration(
                new SimpleDividerItemDecoration(getActivity().getApplicationContext()));
        final View emptyView = view.findViewById(R.id.empty_view);
        mAlertRecyclerView.setEmptyView(emptyView);

        initRefresh();
        updateFilterWarning();

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.alerts_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initRefresh();
            }
        });

        // If this fragment got recreated while the filter dialog was open, we need to update
        // the listener reference
        if (mFilterFragment != null) {
            mFilterFragment.setFilterListener(this);
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_filter_alerts:
                displayFilter();
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

        mAlertListPresenter.checkIfUpdateNeeded();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Casting to HashSet, because Set is not serializable :(
        HashSet<RouteType> filter = (HashSet<RouteType>) mAlertListPresenter.getFilter();

        outState.putSerializable(KEY_ACTIVE_FILTER, filter);
    }

    private void initRefresh() {
        mAlertListPresenter.fetchAlertList();

        mAlertListPresenter.setLastUpdate();
    }

    private void updateSubtitle(int count) {
        String subtitle = getResources().getString(R.string.actionbar_subtitle_alert_count, count);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }

    private void displayFilter() {
        mFilterFragment = AlertFilterFragment.newInstance(this,
                mAlertListPresenter.getFilter());
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        mFilterFragment.show(transaction, "dialog");
    }

    @Override
    public void onFilterChanged(Set<RouteType> selectedTypes) {
        mAlertListPresenter.setFilter(selectedTypes);
        mAlertListPresenter.getAlertList();

        updateFilterWarning();
    }

    @Override
    public void displayAlerts(List<Alert> alerts) {
        setErrorView(false, null);

        if (mAlertAdapter == null) {
            mAlertAdapter = new AlertAdapter(alerts);
            mAlertRecyclerView.setAdapter(mAlertAdapter);
        } else {
            mAlertAdapter.updateAlertData(alerts);
            mAlertAdapter.notifyDataSetChanged();
        }

        updateSubtitle(alerts.size());
        setUpdating(false);
    }

    @Override
    public void displayNetworkError(VolleyError error) {
        int errorMessageId = Utils.volleyErrorTypeHandler(error);
        String errorMessage = getResources().getString(errorMessageId);

        setErrorView(true, errorMessage);
    }

    @Override
    public void displayDataError() {
        setErrorView(true, getString(R.string.error_list_display));
    }

    @Override
    public void displayGeneralError() {
        setErrorView(true, getString(R.string.error_list_display));
    }

    @Override
    public void setUpdating(boolean state) {
        mSwipeRefreshLayout.setRefreshing(state);
    }

    /**
     * Displays or hides the error view. If displaying, it also sets the retry button's event listener
     * and the error message.
     * @param state true to display, false to hide
     * @param errorMessage
     */
    private void setErrorView(boolean state, String errorMessage) {
        if (state) {
            setUpdating(false);
            mAlertRecyclerView.setVisibility(View.GONE);

            if (mErrorLayout != null) {
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
            }
        } else {
            mAlertRecyclerView.setVisibility(View.VISIBLE);

            if (mErrorLayout != null) {
                mErrorLayout.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Updates the filter warning bar above the list based on the currently selected RouteTypes.
     * Hides the bar if nothing is selected as filter.
     */
    private void updateFilterWarning() {
        // Might be null, because it gets called by onCreate() too
        if (mFilterWarningView != null && mAlertListPresenter != null) {
            Set<RouteType> selectedTypes = mAlertListPresenter.getFilter();

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



    private class AlertHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView mTitleTextView;

        private TextView mDateTextView;

        private FlowLayout mRouteIconsWrapper;

        private Alert mAlert;

        public AlertHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView.findViewById(R.id.list_item_alert_description);
            mDateTextView = (TextView) itemView.findViewById(R.id.list_item_alert_date);
            mRouteIconsWrapper = (FlowLayout) itemView.findViewById(R.id.list_item_alert_route_icons_wrapper);

            itemView.setOnClickListener(this);
        }

        public void bindAlert(Alert alert) {
            mAlert = alert;

            // Title (header text)
            mTitleTextView.setText(alert.getHeader());

            // Start - end dates
            String dateString = UiUtils.alertDateFormatter(
                    getActivity(), mAlert.getStart(), mAlert.getEnd()
            );
            mDateTextView.setText(dateString);

            // Route icons
            // First, removing any previously added icon views from the layout
            mRouteIconsWrapper.removeAllViews();

            // There are alerts without affected routes, eg. announcements
            if (alert.getRouteIds() != null) {
                for (Route route : alert.getAffectedRoutes()) {
                    UiUtils.addRouteIcon(getActivity(), mRouteIconsWrapper, route);

                    if (route.getType() == RouteType._OTHER_) {
                        Toast.makeText(getContext(),"Unknown route type: " + route.getShortName() + "(" + route.getId() + ")", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }

        @Override
        public void onClick(View v) {
            //Intent intent = AlertDetailActivity.newIntent(getContext(), mAlert);
            //startActivity(intent);

            AlertDetailFragment alertDetailFragment = AlertDetailFragment.newInstance(mAlert);
            alertDetailFragment.show(getActivity().getSupportFragmentManager(), alertDetailFragment.getTag());
        }
    }

    private class AlertAdapter extends RecyclerView.Adapter<AlertHolder> {

        private List<Alert> mAlerts;

        public AlertAdapter(List<Alert> alerts) {
            mAlerts = alerts;
        }

        public void updateAlertData(List<Alert> alerts) {
            mAlerts = alerts;
        }

        @Override
        public AlertHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_alert, parent, false);
            return new AlertHolder(view);
        }

        @Override
        public void onBindViewHolder(AlertHolder holder, int position) {
            Alert alert = mAlerts.get(position);
            holder.bindAlert(alert);
        }

        @Override
        public int getItemCount() {
            return mAlerts.size();
        }
    }
}
