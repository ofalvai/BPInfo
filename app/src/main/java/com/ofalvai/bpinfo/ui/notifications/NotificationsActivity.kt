package com.ofalvai.bpinfo.ui.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.NavUtils
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.google.firebase.iid.FirebaseInstanceId
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.base.BaseActivity
import com.ofalvai.bpinfo.ui.notifications.adapter.RouteListPagerAdapter
import com.ofalvai.bpinfo.ui.settings.SettingsActivity
import kotterknife.bindView
import timber.log.Timber

class NotificationsActivity : BaseActivity(), NotificationsContract.View {

    private lateinit var presenter: NotificationsContract.Presenter

    private val tabLayout: TabLayout by bindView(R.id.notifications__tabs)

    private val viewPager: ViewPager by bindView(R.id.notifications__viewpager)

    private val debugTextView: TextView by bindView(R.id.notifications__debug_text)

    private lateinit var pagerAdapter: RouteListPagerAdapter

    companion object {
        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.title_activity_notifications)


        presenter = NotificationsPresenter()
        presenter.attachView(this)

        presenter.fetchRouteList()
        presenter.fetchSubscriptions()

        Timber.d("FCM token: " + FirebaseInstanceId.getInstance().token)

        setupViewPager()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_notifications, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_item_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            android.R.id.home -> NavUtils.navigateUpFromSameTask(this)
        }

        return true
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun displayRouteList(routeList: List<Route>) {
        val groupedRoutes: Map<RouteType, List<Route>> = routeList.groupBy { it.type }
        groupedRoutes.forEach {
            val view = pagerAdapter.getView(it.key)
            val routes = it.value
            view?.let {
                view.displayRoutes(routes)
            }
        }
    }

    override fun onRouteClicked(route: Route) {
        presenter.subscribeTo(route.id)
    }

    override fun displaySubscriptions(routeIdList: List<String>) {
        debugTextView.text = routeIdList.joinToString(separator = ", ")
    }

    private fun setupViewPager() {
        pagerAdapter = RouteListPagerAdapter(supportFragmentManager, this)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 99 // TODO
        tabLayout.setupWithViewPager(viewPager, false)
    }
}
