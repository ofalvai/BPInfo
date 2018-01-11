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

package com.ofalvai.bpinfo.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.ui.base.BaseActivity

class LicensesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenses)
        setTitle(R.string.title_activity_licences)

        val webView = findViewById<WebView>(R.id.webview_licences)
        webView?.loadUrl("file:///android_asset/licenses.html")
    }

    companion object {

        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, LicensesActivity::class.java)
        }
    }
}
