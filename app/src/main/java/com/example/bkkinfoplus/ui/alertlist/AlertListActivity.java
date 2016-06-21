package com.example.bkkinfoplus.ui.alertlist;

import android.support.v4.app.Fragment;

import com.example.bkkinfoplus.ui.SingleFragmentActivity;

public class AlertListActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new AlertListFragment();
    }
}
