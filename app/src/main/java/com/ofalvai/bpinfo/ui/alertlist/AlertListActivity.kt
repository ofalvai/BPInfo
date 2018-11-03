/*
 * Copyright 2018 Olivér Falvai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ofalvai.bpinfo.ui.alertlist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.notifications.NotificationMaker
import com.ofalvai.bpinfo.ui.alertlist.adapter.AlertListPagerAdapter
import com.ofalvai.bpinfo.ui.base.BaseActivity
import com.ofalvai.bpinfo.util.Analytics
import com.ofalvai.bpinfo.util.bindView
import org.koin.android.ext.android.inject

class AlertListActivity : BaseActivity() {

    /**
     * ID of an Alert after a notification launches the Activity.
     * This is later accessed by [AlertListFragment].
     */
    var pendingNavigationAlertId: String? = null

    private val viewPager: ViewPager by bindView(R.id.alert_list_pager)
    private val tabLayout: TabLayout by bindView(R.id.tabs)
    private val toolbar: Toolbar by bindView(R.id.toolbar)

    private lateinit var pagerAdapter: AlertListPagerAdapter

    private val analytics: Analytics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_list)

        setSupportActionBar(toolbar)

        pagerAdapter = AlertListPagerAdapter(supportFragmentManager, this)

        viewPager.adapter = pagerAdapter
        viewPager.addOnPageChangeListener(pagerAdapter)

        tabLayout.setupWithViewPager(viewPager, false)

        handlePendingNavigation(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handlePendingNavigation(intent)
    }

    /**
     * This Activity's launchMode is singleTop, so either onNewIntent() or onCreate() is called.
     * This method must be called from both.
     */
    private fun handlePendingNavigation(intent: Intent?) {
        pendingNavigationAlertId = intent?.getStringExtra(NotificationMaker.INTENT_EXTRA_ALERT_ID)

        pendingNavigationAlertId?.let {
            analytics.logNotificationOpen(it)

            // Prevent triggering it again in the future (eg. back navigation from another Activity)
            intent?.removeExtra(NotificationMaker.INTENT_EXTRA_ALERT_ID)
        }
    }
}
