package com.ofalvai.bpinfo.ui.base

import android.content.Context
import android.support.v7.app.AppCompatActivity
import com.ofalvai.bpinfo.util.LocaleManager

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Updating locale
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }
}