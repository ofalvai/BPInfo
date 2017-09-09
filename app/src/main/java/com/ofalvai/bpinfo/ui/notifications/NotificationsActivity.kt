package com.ofalvai.bpinfo.ui.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.google.firebase.iid.FirebaseInstanceId
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.notifications.adapter.RouteListPagerAdapter
import com.ofalvai.bpinfo.util.bindView
import timber.log.Timber

class NotificationsActivity : AppCompatActivity(), NotificationsContract.View {

    private lateinit var presenter: NotificationsContract.Presenter

    private val tabLayout: TabLayout by bindView(R.id.notifications__tabs)

    private val viewPager: ViewPager by bindView(R.id.notifications__viewpager)

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

        presenter = NotificationsPresenter()
        presenter.attachView(this)

        presenter.fetchRouteList()

        Timber.d("FCM token: " + FirebaseInstanceId.getInstance().token)

        setupViewPager()
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

    private fun setupViewPager() {
        pagerAdapter = RouteListPagerAdapter(supportFragmentManager, this)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 99 // TODO
        tabLayout.setupWithViewPager(viewPager, false)
    }
}
