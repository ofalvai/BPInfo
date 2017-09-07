package com.ofalvai.bpinfo.ui.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v7.app.AppCompatActivity
import com.google.firebase.iid.FirebaseInstanceId
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.util.bindView
import timber.log.Timber

class NotificationsActivity : AppCompatActivity(), NotificationsContract.View {

    lateinit var presenter: NotificationsContract.Presenter

    val tabLayout: TabLayout by bindView(R.id.notifications__tabs)

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

        tabLayout.addTab(tabLayout.newTab().setText("Busz"))
        tabLayout.addTab(tabLayout.newTab().setText("Metró"))
        tabLayout.addTab(tabLayout.newTab().setText("Villamos"))
        tabLayout.addTab(tabLayout.newTab().setText("Trolibusz"))
        tabLayout.addTab(tabLayout.newTab().setText("HÉV"))
        tabLayout.addTab(tabLayout.newTab().setText("Hajó"))
        tabLayout.addTab(tabLayout.newTab().setText("Egyéb"))
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }
}
