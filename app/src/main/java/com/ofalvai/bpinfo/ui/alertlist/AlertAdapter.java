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

import android.content.Context;
import android.os.Handler;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.model.Alert;

import java.util.ArrayList;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertHolder> {

    private List<Alert> mAlerts;

    private final Context mContext;

    private final AlertListContract.View mView;

    public AlertAdapter(List<Alert> alerts, Context context, AlertListContract.View view) {
        mAlerts = alerts;
        mContext = context;
        mView = view;
    }

    public void updateAlertData(final List<Alert> alerts) {
        //mAlerts = alerts;
        //notifyDataSetChanged();


        final List<Alert> oldAlerts = new ArrayList<>(mAlerts);

        new Runnable() {
            @Override
            public void run() {
                final DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return oldAlerts.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return alerts.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        return oldAlerts.get(oldItemPosition).getId().equals(alerts.get(newItemPosition).getId());
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return oldAlerts.get(oldItemPosition).equals(alerts.get(newItemPosition));
                    }
                });

                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        mAlerts = alerts;
                        diff.dispatchUpdatesTo(AlertAdapter.this);
                        diff.dispatchUpdatesTo(new ListUpdateCallback() {
                            @Override
                            public void onInserted(int position, int count) {
                                AlertListFragment alertListFragment = (AlertListFragment) mView;
                                alertListFragment.mAlertRecyclerView.smoothScrollToPosition(0);
                            }

                            @Override
                            public void onRemoved(int position, int count) {

                            }

                            @Override
                            public void onMoved(int fromPosition, int toPosition) {

                            }

                            @Override
                            public void onChanged(int position, int count, Object payload) {

                            }
                        });
                        //notifyDataSetChanged();
                    }
                });

            }
        }.run();

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
                mView.launchAlertDetail(alert);
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
