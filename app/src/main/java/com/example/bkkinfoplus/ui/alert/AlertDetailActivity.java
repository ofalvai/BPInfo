package com.example.bkkinfoplus.ui.alert;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.example.bkkinfoplus.model.Alert;
import com.example.bkkinfoplus.ui.SingleFragmentActivity;

public class AlertDetailActivity extends SingleFragmentActivity {

    private static final String EXTRA_ALERT_OBJECT = "com.example.bkkinfoplus.alert_object";


    @Override
    protected Fragment createFragment() {
        Alert alert = (Alert) getIntent().getSerializableExtra(EXTRA_ALERT_OBJECT);

        return AlertDetailFragment.newInstance(alert);
    }

    public static Intent newIntent(Context packageContext, Alert alert) {
        Intent intent = new Intent(packageContext, AlertDetailActivity.class);
        intent.putExtra(EXTRA_ALERT_OBJECT, alert);
        return intent;
    }
}
