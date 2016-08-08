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
