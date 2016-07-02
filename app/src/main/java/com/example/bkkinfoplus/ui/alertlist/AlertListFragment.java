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
import com.example.bkkinfoplus.ui.SimpleDividerItemDecoration;
import com.example.bkkinfoplus.ui.UiUtils;
import com.example.bkkinfoplus.ui.alert.AlertDetailFragment;
import com.wefika.flowlayout.FlowLayout;

import java.util.List;
import java.util.Set;

public class AlertListFragment extends Fragment
        implements AlertListPresenter.AlertInteractionListener, AlertFilterFragment.AlertFilterListener {

    private static final String TAG = "AlertListFragment";

    private RecyclerView mAlertRecyclerView;
    private AlertAdapter mAlertAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinearLayout mErrorLayout;

    private AlertListPresenter mAlertListPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlertListPresenter = new AlertListPresenter(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_alert_list, container, false);

        mErrorLayout = (LinearLayout) view.findViewById(R.id.error_with_action);

        mAlertRecyclerView = (RecyclerView) view.findViewById(R.id.alerts_recycler_view);
        mAlertRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAlertRecyclerView.addItemDecoration(
                new SimpleDividerItemDecoration(getActivity().getApplicationContext()));

        initRefresh();

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.alerts_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initRefresh();
            }
        });

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
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        mAlertListPresenter.checkIfUpdateNeeded();
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
        AlertFilterFragment filterFragment = AlertFilterFragment.newInstance(this,
                mAlertListPresenter.getFilter());
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        filterFragment.show(transaction, "dialog");
    }

    @Override
    public void onFilterChanged(Set<RouteType> selectedTypes) {
        mAlertListPresenter.setFilter(selectedTypes);
        mAlertListPresenter.getAlertList();
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

            mAlertRecyclerView.setVisibility(View.GONE);
        } else {
            mAlertRecyclerView.setVisibility(View.VISIBLE);
            mErrorLayout.setVisibility(View.GONE);
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
