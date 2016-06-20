package com.example.bkkinfoplus.ui;

import android.support.v4.app.Fragment;

public class AlertListActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new AlertListFragment();
    }
}
