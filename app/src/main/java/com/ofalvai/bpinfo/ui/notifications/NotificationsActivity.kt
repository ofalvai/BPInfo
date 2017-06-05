package com.ofalvai.bpinfo.ui.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ofalvai.bpinfo.R

class NotificationsActivity : AppCompatActivity(), NotificationsContract.View {

    lateinit var mPresenter: NotificationsContract.Presenter

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
    }
}
