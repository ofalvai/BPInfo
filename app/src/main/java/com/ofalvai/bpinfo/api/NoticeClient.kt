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

package com.ofalvai.bpinfo.api

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.ofalvai.bpinfo.Config
import com.ofalvai.bpinfo.R
import org.json.JSONArray
import timber.log.Timber
import javax.inject.Inject

/**
 * Fetches messages (notices) from our own backend. This is used to inform the users when the API is
 * down or broken, without updating the app itself.
 */
class NoticeClient
@Inject constructor(private val requestQueue: RequestQueue,
                    private val context: Context,
                    private val sharedPreferences: SharedPreferences
) : Response.ErrorListener {

    private val isDebugActivated: Boolean
        get() = sharedPreferences.getBoolean(
                context.getString(R.string.pref_key_debug_mode), false)

    interface NoticeListener {
        /**
         * Called only when there's at least 1 notice to display
         * @param noticeBody HTML string of 1 or more notices appended
         */
        fun onNoticeResponse(noticeBody: String)

        fun onNoNotice()
    }

    fun fetchNotice(noticeListener: NoticeListener, languageCode: String) {
        val url = Uri.parse(Config.BACKEND_URL).buildUpon()
                .appendEncodedPath(Config.BACKEND_NOTICE_PATH)
                .build()

        val request = JsonArrayRequest(
                url.toString(),
                Response.Listener { response ->
                    onResponseCallback(response, noticeListener, languageCode)
                },
                this
        )

        // Invalidating Volley's cache for this URL to always get the latest notice
        requestQueue.cache.remove(url.toString())
        requestQueue.add(request)
    }

    override fun onErrorResponse(error: VolleyError) {
        // We don't display anything on the UI because this feature is meant to be silent
        Timber.e(error.toString())
    }

    private fun onResponseCallback(response: JSONArray, listener: NoticeListener, languageCode: String) {
        try {
            val noticeBuilder = StringBuilder()

            // The response contains an array of notices, we display the ones marked as enabled
            for (i in 0 until response.length()) {
                val notice = response.getJSONObject(i)
                val enabled = notice.getBoolean(NoticeContract.ENABLED)
                val debugEnabled = notice.getBoolean(NoticeContract.ENABLED_DEBUG)

                // Only display notice if it's marked as enabled OR marked as enabled for debug mode
                // and debug mode is actually turned on:
                if (enabled || debugEnabled && isDebugActivated) {
                    val noticeText: String = if (languageCode == "hu") {
                        notice.getString(NoticeContract.TEXT_HU)
                    } else {
                        notice.getString(NoticeContract.TEXT_EN)
                    }
                    noticeBuilder.append(noticeText)
                    noticeBuilder.append("<br /><br />")
                }
            }

            if (noticeBuilder.isNotEmpty()) {
                listener.onNoticeResponse(noticeBuilder.toString())
            } else {
                listener.onNoNotice()
            }
        } catch (ex: Exception) {
            // We don't display anything on the UI because this feature is meant to be silent
            Timber.e(ex.toString())
        }
    }
}
