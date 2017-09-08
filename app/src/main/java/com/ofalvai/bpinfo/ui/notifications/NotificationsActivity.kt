package com.ofalvai.bpinfo.ui.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.google.firebase.iid.FirebaseInstanceId
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.ui.notifications.adapter.RouteListPagerAdapter
import com.ofalvai.bpinfo.util.bindView
import timber.log.Timber

class NotificationsActivity : AppCompatActivity(), NotificationsContract.View {

    lateinit var presenter: NotificationsContract.Presenter

    val tabLayout: TabLayout by bindView(R.id.notifications__tabs)

    val viewPager: ViewPager by bindView(R.id.notifications__viewpager)

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

    private fun setupViewPager() {
        val adapter = RouteListPagerAdapter(supportFragmentManager, this)
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager, false)
    }
}
