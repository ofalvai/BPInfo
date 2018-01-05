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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import com.ofalvai.bpinfo.Config
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.ui.base.BaseActivity
import com.ofalvai.bpinfo.util.underline
import kotterknife.bindView

class AboutActivity : BaseActivity() {

    private val licensesView: TextView by bindView(R.id.about_licenses)

    private val sourceCodeView: TextView by bindView(R.id.about_source_code)

    private val privacyPolicyView: TextView by bindView(R.id.about_privacy_policy)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        licensesView.underline()
        licensesView.setOnClickListener {
            val licencesIntent = LicensesActivity.newIntent(this@AboutActivity)
            startActivity(licencesIntent)
        }

        sourceCodeView.underline()
        sourceCodeView.setOnClickListener {
            val sourceCodeIntent = Intent(Intent.ACTION_VIEW)
            sourceCodeIntent.data = Uri.parse(Config.SOURCE_CODE_URL)
            startActivity(sourceCodeIntent)
        }

        privacyPolicyView.underline()
        privacyPolicyView.setOnClickListener {
            val privacyPolicyIntent = Intent(Intent.ACTION_VIEW)
            privacyPolicyIntent.data = Uri.parse(Config.PRIVACY_POLICY_URL)
            startActivity(privacyPolicyIntent)
        }
    }
}
