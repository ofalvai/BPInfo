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

package com.example.bkkinfoplus.ui.settings;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.example.bkkinfoplus.Config;
import com.example.bkkinfoplus.R;

public class AboutActivity extends AppCompatActivity {

    @Nullable
    private TextView mLicencesTextView;

    @Nullable
    private TextView mSourceCodeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        mLicencesTextView = (TextView) findViewById(R.id.about_licences);
        mSourceCodeTextView = (TextView) findViewById(R.id.about_source_code);

        if (mLicencesTextView != null) {
            mLicencesTextView.setPaintFlags(mLicencesTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            final Intent licencesIntent = LicencesActivity.newIntent(this);
            mLicencesTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(licencesIntent);
                }
            });
        }

        if (mSourceCodeTextView != null) {
            mSourceCodeTextView.setPaintFlags(mSourceCodeTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            Uri url = Uri.parse(Config.SOURCE_CODE_URL);
            final Intent sourceCodeIntent = new Intent(Intent.ACTION_VIEW);
            sourceCodeIntent.setData(url);
            mSourceCodeTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(sourceCodeIntent);
                }
            });
        }
    }
}
