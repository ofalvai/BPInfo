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

    lateinit var mPresenter: NotificationsContract.Presenter

    val mTabLayout: TabLayout by bindView(R.id.notifications__tabs)

    companion object {
        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        mPresenter = NotificationsPresenter()
        mPresenter.attachView(this)

        Timber.d("FCM token: " + FirebaseInstanceId.getInstance().token)

        mTabLayout.addTab(mTabLayout.newTab().setText("Busz"))
        mTabLayout.addTab(mTabLayout.newTab().setText("Metró"))
        mTabLayout.addTab(mTabLayout.newTab().setText("Villamos"))
        mTabLayout.addTab(mTabLayout.newTab().setText("Trolibusz"))
        mTabLayout.addTab(mTabLayout.newTab().setText("HÉV"))
        mTabLayout.addTab(mTabLayout.newTab().setText("Hajó"))
        mTabLayout.addTab(mTabLayout.newTab().setText("Egyéb"))
    }
}
