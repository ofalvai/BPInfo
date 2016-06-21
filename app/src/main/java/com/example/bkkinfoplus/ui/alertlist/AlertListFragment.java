package com.example.bkkinfoplus.ui.alertlist;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bkkinfoplus.model.Alert;
import com.example.bkkinfoplus.R;
import com.example.bkkinfoplus.model.Route;
import com.example.bkkinfoplus.Utils;
import com.example.bkkinfoplus.ui.SimpleDividerItemDecoration;
import com.example.bkkinfoplus.ui.UiUtils;
import com.wefika.flowlayout.FlowLayout;

import java.util.Collections;
import java.util.List;

public class AlertListFragment extends Fragment implements AlertListPresenter.AlertInteractionListener {

    private static final String TAG = "AlertListFragment";

    private RecyclerView mAlertRecyclerView;
    private AlertAdapter mAlertAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private AlertListPresenter mAlertListPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: inject
        mAlertListPresenter = new AlertListPresenter(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_alert_list, container, false);

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
    public void onStart() {
        super.onStart();

        mAlertListPresenter.checkIfUpdateNeeded();
    }

    private void initRefresh() {
        mAlertListPresenter.getAlertList();

        mAlertListPresenter.setLastUpdate();
    }

    private void updateSubtitle(int count) {
        String subtitle = getResources().getString(R.string.actionbar_subtitle_alert_count, count);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }

    @Override
    public void displayAlerts(List<Alert> alerts) {
        // Sort: descending by alert start time
        Collections.sort(alerts, new Utils.AlertStartTimestampComparator());
        Collections.reverse(alerts);

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
    public void displayNetworkError() {
        // TODO
    }

    @Override
    public void displayGeneralError() {
        setUpdating(false);
        Toast.makeText(getActivity(), R.string.error_list_display, Toast.LENGTH_LONG).show();
    }

    @Override
    public void setUpdating(boolean state) {
        mSwipeRefreshLayout.setRefreshing(state);
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
            String dateString = UiUtils.createAlertDateString(
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
