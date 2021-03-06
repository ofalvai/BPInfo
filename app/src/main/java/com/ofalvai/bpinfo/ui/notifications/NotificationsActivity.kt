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

package com.ofalvai.bpinfo.ui.notifications

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.iid.FirebaseInstanceId
import com.ofalvai.bpinfo.Config
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.notifications.NotificationMaker
import com.ofalvai.bpinfo.ui.base.BaseActivity
import com.ofalvai.bpinfo.ui.notifications.adapter.RouteListPagerAdapter
import com.ofalvai.bpinfo.ui.settings.PreferencesActivity
import com.ofalvai.bpinfo.util.*
import com.wefika.flowlayout.FlowLayout
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class NotificationsActivity : BaseActivity() {

    private val viewModel by viewModel<NotificationsViewModel>()

    private val analytics: Analytics by inject()

    private val tabLayout: TabLayout by bindView(R.id.notifications__tabs)
    private val viewPager: ViewPager by bindView(R.id.notifications__viewpager)
    private val subscribedRoutesLayout: FlowLayout by bindView(R.id.notifications__subscribed_routes)
    private val subscribedEmptyView: TextView by bindView(R.id.notifications__subscribed_empty)
    private val progressBar: ContentLoadingProgressBar by bindView(R.id.notifications__progress_bar)
    private val errorView: View by bindView(R.id.notifications__error)

    private lateinit var pagerAdapter: RouteListPagerAdapter

    companion object {
        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationsActivity::class.java)
        }
    }

    @SuppressLint("MissingFirebaseInstanceTokenRefresh")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setTitle(R.string.title_activity_notifications)
        }

        setupViewPager()

        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)

        @Suppress("DEPRECATION")
        Timber.i("FCM token: %s", FirebaseInstanceId.getInstance().token)

        observe(viewModel.routeListError, this::showRouteListError)

        observe(viewModel.subscriptions, this::displaySubscriptions)

        observe(viewModel.subscriptionProgress, this::showSubscriptionProgress)

        observe(viewModel.subscriptionError) {
            showSubscriptionError()
        }

        observe(viewModel.newSubscribedRoute, this::addSubscribedRoute)

        observe(viewModel.removedSubscribedRoute, this::removeSubscribedRoute)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_notifications, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_settings -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
            }
            R.id.menu_item_test_notification -> {
                NotificationMaker.make(
                    this, Config.Behavior.TEST_NOTIFICATION_ALERT_ID,
                    getString(R.string.notif_test_title),
                    getString(R.string.notif_test_desc)
                )
            }
            android.R.id.home -> NavUtils.navigateUpFromSameTask(this)
        }

        return true
    }

    private fun addSubscribedRoute(route: Route) {
        analytics.logNotificationSubscribe(route.id)

        subscribedEmptyView.hide()

        addSubscribedRouteIcon(route)
    }

    private fun removeSubscribedRoute(route: Route) {
        analytics.logNotificationUnsubscribe(route.id)

        removeSubscribedRouteIcon(route)

        if (subscribedRoutesLayout.childCount == 0) {
            subscribedEmptyView.show()
        }
    }

    fun onRouteClicked(route: Route) {
        viewModel.subscribeTo(route.id)
    }

    private fun displaySubscriptions(routeList: List<Route>) {
        subscribedRoutesLayout.removeAllViews()

        if (routeList.isEmpty()) {
            subscribedEmptyView.show()
        } else {
            subscribedEmptyView.hide()
        }

        for (route in routeList) {
            addSubscribedRouteIcon(route)
        }
    }

    private fun showSubscriptionProgress(show: Boolean) {
        if (show) {
            progressBar.show()
        } else {
            progressBar.hide()
        }
    }

    private fun showRouteListError(show: Boolean) {
        if (show) {
            showSubscriptionProgress(false)

            errorView.visibility = View.VISIBLE
            viewPager.visibility = View.GONE

            val errorMessageView = errorView.findViewById<TextView>(R.id.error_message)
            val refreshButton = errorView.findViewById<Button>(R.id.error_action_button)

            if (!refreshButton.hasOnClickListeners()) {
                refreshButton.setOnClickListener {
                    viewModel.fetchRouteList()
                    viewModel.fetchSubscriptions()
                }
            }
            refreshButton.text = getString(R.string.label_retry)
            errorMessageView.text = getString(R.string.error_routes_load)
        } else {
            errorView.visibility = View.GONE
            viewPager.visibility = View.VISIBLE
        }
    }

    private fun showSubscriptionError() {
        val snackbar = Snackbar.make(
            viewPager,
            getString(R.string.error_subscriptions_action),
            Snackbar.LENGTH_LONG
        )
        snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.accent))
        snackbar.show()
    }

    private fun setupViewPager() {
        pagerAdapter = RouteListPagerAdapter(supportFragmentManager, this)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = RouteListPagerAdapter.OFFSCREEN_PAGE_LIMIT
        tabLayout.setupWithViewPager(viewPager, false)
    }

    @SuppressLint("RestrictedApi")
    private fun addSubscribedRouteIcon(route: Route) {
        val iconContextTheme = ContextThemeWrapper(this, R.style.RouteIcon_Big)
        val iconView = TextView(iconContextTheme)

        iconView.text = route.shortName
        iconView.setTextColor(route.textColor)
        iconView.contentDescription = route.getContentDescription(this)
        iconView.tag = route.id
        iconView.alpha = 0.0f
        subscribedRoutesLayout.addView(iconView)

        // Layout attributes defined in R.style.RouteIcon were ignored before attaching the view to
        // a parent, so we need to manually set them
        val params = iconView.layoutParams as ViewGroup.MarginLayoutParams
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        val margin = resources.getDimension(R.dimen.route_icon_margin).toInt()
        params.rightMargin = margin
        params.topMargin = margin

        // Setting a custom colored rounded background drawable as background
        val iconBackground = ContextCompat.getDrawable(this, R.drawable.rounded_corner_5dp)
        if (iconBackground != null) {
            val colorFilter = LightingColorFilter(Color.rgb(1, 1, 1), route.color)
            iconBackground.mutate().colorFilter = colorFilter
            iconView.background = iconBackground
        }

        iconView.setOnClickListener { showDeleteDialog(route) }

        iconView.animate().apply {
            alpha(1.0f)
            duration = 225
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun removeSubscribedRouteIcon(route: Route) {
        for (i in 0 until subscribedRoutesLayout.childCount) {
            val view: View? = subscribedRoutesLayout.getChildAt(i)
            if (view?.tag == route.id) {
                subscribedRoutesLayout.removeView(view)
            }
        }
    }

    private fun showDeleteDialog(route: Route) {
        AlertDialog.Builder(this)
            .setTitle(route.shortName) // TODO: localized long name
            .setMessage(R.string.notif_remove_dialog_message)
            .setPositiveButton(R.string.notif_remove_dialog_positive) { dialog, _ ->
                dialog.dismiss()
                viewModel.removeSubscription(route.id)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
