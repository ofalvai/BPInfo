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

package com.ofalvai.bpinfo.ui.alertlist

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.ui.alertlist.adapter.AlertListPagerAdapter
import com.ofalvai.bpinfo.ui.base.BaseActivity
import kotterknife.bindView

class AlertListActivity : BaseActivity() {

    private val viewPager: ViewPager by bindView(R.id.alert_list_pager)

    private val tabLayout: TabLayout by bindView(R.id.tabs)

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_list)

        setSupportActionBar(toolbar)

        val alertListPagerAdapter = AlertListPagerAdapter(supportFragmentManager, this)

        viewPager.adapter = alertListPagerAdapter
        viewPager.addOnPageChangeListener(alertListPagerAdapter)

        tabLayout.setupWithViewPager(viewPager, false)
    }
}
