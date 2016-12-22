package com.ofalvai.bpinfo.ui.alertlist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.model.Alert;

import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertHolder> {

    private List<Alert> mAlerts;

    private Context mContext;

    private AlertListContract.View mView;

    public AlertAdapter(List<Alert> alerts, Context context, AlertListContract.View view) {
        mAlerts = alerts;
        mContext = context;
        mView = view;
    }

    public void updateAlertData(List<Alert> alerts) {
        mAlerts = alerts;
    }

    @Override
    public AlertHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View view = layoutInflater.inflate(R.layout.list_item_alert, parent, false);

        final AlertHolder holder = new AlertHolder(view, mView.getAlertListType());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Alert alert = mAlerts.get(holder.getAdapterPosition());
                mView.displayAlertDetail(alert);
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(AlertHolder holder, int position) {
        Alert alert = mAlerts.get(position);
        holder.bindAlert(alert, mContext);
    }

    @Override
    public int getItemCount() {
        return mAlerts.size();
    }
}
