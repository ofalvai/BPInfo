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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.ofalvai.bpinfo.R;

public class AlertListActivity extends AppCompatActivity {

    @Nullable
    private ViewPager mViewPager;

    @Nullable
    private AlertListPagerAdapter mAlertListPagerAdapter;

    @Nullable
    private TabLayout mTabLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alert_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        mAlertListPagerAdapter = new AlertListPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.alert_list_pager);

        if (mViewPager != null) {
            mViewPager.setAdapter(mAlertListPagerAdapter);
        }

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        if (mViewPager != null && mViewPager.getAdapter() != null) {
            mTabLayout.setupWithViewPager(mViewPager, false);
        }
    }

    private class AlertListPagerAdapter extends FragmentPagerAdapter {

        public AlertListPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new AlertListFragment();
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String title;
            switch (position) {
                case 0:
                    title = getString(R.string.tab_title_today);
                break;
                case 1:
                    title = getString(R.string.tab_title_future);
                break;
                default:
                    title = "Default tab";
            }
            return title;
        }
    }
}
