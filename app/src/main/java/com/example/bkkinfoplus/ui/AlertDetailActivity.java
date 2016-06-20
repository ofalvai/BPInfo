package com.example.bkkinfoplus.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.bkkinfoplus.Alert;
import com.example.bkkinfoplus.R;
import com.example.bkkinfoplus.Route;

import java.util.List;

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
