package com.example.bkkinfoplus.ui;

import android.content.Context;
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

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.bkkinfoplus.Alert;
import com.example.bkkinfoplus.AlertLoader;
import com.example.bkkinfoplus.BkkInfoApplication;
import com.example.bkkinfoplus.R;
import com.example.bkkinfoplus.Route;
import com.example.bkkinfoplus.Utils;
import com.wefika.flowlayout.FlowLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class AlertListFragment extends Fragment implements Response.Listener<JSONObject>, Response.ErrorListener{

    private static final String TAG = "AlertListFragment";

    private RecyclerView mAlertRecyclerView;
    private AlertAdapter mAlertAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Inject AlertLoader mAlertLoader;

    private long mLastUpdate;
    private static final int REFRESH_THRESHOLD_SEC = 30;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BkkInfoApplication.injector.inject(this);

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

        long currentTime = new GregorianCalendar().getTimeInMillis();
        long diff = currentTime - mLastUpdate;
        TimeUnit secondUnit = TimeUnit.SECONDS;
        long diffInSeconds = secondUnit.convert(diff, TimeUnit.MILLISECONDS);

        if (diffInSeconds > REFRESH_THRESHOLD_SEC) {
            mSwipeRefreshLayout.setRefreshing(true);
            initRefresh();
        }
    }

    private void initRefresh() {
        Context applicationContext = this.getActivity().getApplicationContext();

        if (mAlertLoader != null) {
            mAlertLoader.fetchAlertList(applicationContext, this, this);
        }

        mLastUpdate = new GregorianCalendar().getTimeInMillis();
    }

    private void updateUI(List<Alert> alerts) {
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
        mSwipeRefreshLayout.setRefreshing(false);
    }


    @Override
    public void onErrorResponse(VolleyError error) {
        mSwipeRefreshLayout.setRefreshing(false);
        Toast.makeText(getActivity(), R.string.error_response, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResponse(JSONObject response) {
        List<Alert> alerts = null;
        try {
           alerts = mAlertLoader.parseAlertList(response);
            //TODO: refactor
            mAlertLoader.updateRoutes(response);
        } catch (Exception ex) {
            Toast.makeText(getActivity(), R.string.error_list_display, Toast.LENGTH_LONG).show();
        }

        if (alerts == null || alerts.isEmpty()) {
            // TODO
        } else {
            updateUI(alerts);
        }

    }

    private class AlertHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView mTitleTextView;

        private TextView mDateTextView;

        private FlowLayout mRouteIconsWrapper;

        private Alert mAlert;

        private List<Route> mAffectedRoutes;

        public AlertHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView.findViewById(R.id.list_item_alert_description);
            mDateTextView = (TextView) itemView.findViewById(R.id.list_item_alert_date);
            mRouteIconsWrapper = (FlowLayout) itemView.findViewById(R.id.list_item_alert_route_icons_wrapper);

            itemView.setOnClickListener(this);

            mAffectedRoutes = new ArrayList<>();
        }

        public void bindAlert(Alert alert) {
            mAlert = alert;

            // Title (header text)
            mTitleTextView.setText(alert.getHeader());

            long timestampStart = mAlert.getStart();
            long timestampEnd = mAlert.getEnd();
            String dateString = UiUtils.createAlertDateString(
                    getActivity(), timestampStart, timestampEnd
            );
            mDateTextView.setText(dateString);

            // Route icons
            // First, removing any dynamically added icon views from the LinearLayout
            mRouteIconsWrapper.removeAllViews();
            mAffectedRoutes.clear();

            if (alert.getRouteIds() != null) {
                for (String routeId : alert.getRouteIds()) {
                    Route route = mAlertLoader.getRoute(routeId);

                    if (route == null) {
                        // Replacement routes are filtered out at the parse stage,
                        // getting a route by the returned routeId might be null.
                        continue;
                    }

                    // TODO: refactor
                    mAffectedRoutes.add(route);

                    UiUtils.addRouteIcon(getActivity(), mRouteIconsWrapper, route);
                }
                mAlert.setAffectedRoutes(mAffectedRoutes);
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

    private void updateSubtitle(int count) {
        String subtitle = getResources().getString(R.string.actionbar_subtitle_alert_count, count);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }
}
