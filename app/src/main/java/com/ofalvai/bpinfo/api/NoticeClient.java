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

package com.ofalvai.bpinfo.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.crashlytics.android.Crashlytics;
import com.ofalvai.bpinfo.BpInfoApplication;
import com.ofalvai.bpinfo.Config;
import com.ofalvai.bpinfo.R;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Fetches messages (notices) from our own backend. This is used to inform the users when the API is
 * down or broken, without updating the app itself.
 */
public class NoticeClient implements Response.ErrorListener {

    public interface NoticeListener {
        /**
         * Called only when there's at least 1 notice to display
         * @param noticeBody HTML string of 1 or more notices appended
         */
        void onNoticeResponse(String noticeBody);

        void onNoNotice();
    }

    private static final String TAG = "NoticeClient";

    private final RequestQueue mRequestQueue;

    @Inject Context mContext;

    @Inject SharedPreferences mSharedPreferences;

    public NoticeClient(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;

        BpInfoApplication.Companion.getInjector().inject(this);
    }

    public void fetchNotice(@NonNull final NoticeListener noticeListener, @NonNull final String languageCode) {
        final Uri url = Uri.parse(Config.BACKEND_URL).buildUpon()
                .appendEncodedPath(Config.BACKEND_NOTICE_PATH)
                .build();

        final JsonArrayRequest request = new JsonArrayRequest(
                url.toString(),
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        onResponseCallback(response, noticeListener, languageCode);
                    }
                },
                this
        );

        // Invalidating Volley's cache for this URL to always get the latest notice
        mRequestQueue.getCache().remove(url.toString());
        mRequestQueue.add(request);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        // We don't display anything on the UI because this feature is meant to be silent
        Timber.e(error.toString());
        Crashlytics.logException(error);
    }

    private void onResponseCallback(JSONArray response, NoticeListener listener, String languageCode) {
        try {
            StringBuilder noticeBuilder = new StringBuilder();

            // The response contains an array of notices, we display the ones marked as enabled
            for (int i = 0; i < response.length(); i++) {
                final JSONObject notice = response.getJSONObject(i);
                boolean enabled = notice.getBoolean(NoticeContract.ENABLED);
                boolean debugEnabled = notice.getBoolean(NoticeContract.ENABLED_DEBUG);

                // Only display notice if it's marked as enabled OR marked as enabled for debug mode
                // and debug mode is actually turned on:
                if (enabled || (debugEnabled && isDebugActivated())) {
                    String noticeText;
                    if (languageCode.equals("hu")) {
                        noticeText = notice.getString(NoticeContract.TEXT_HU);
                    } else {
                        noticeText = notice.getString(NoticeContract.TEXT_EN);
                    }
                    noticeBuilder.append(noticeText);
                    noticeBuilder.append("<br /><br />");
                }
            }

            if (noticeBuilder.length() > 0) {
                listener.onNoticeResponse(noticeBuilder.toString());
            } else {
                listener.onNoNotice();
            }
        } catch (Exception ex) {
            // We don't display anything on the UI because this feature is meant to be silent
            Timber.e(ex.toString());
            Crashlytics.logException(ex);
        }
    }

    private boolean isDebugActivated() {
        return mSharedPreferences.getBoolean(
                mContext.getString(R.string.pref_key_debug_mode), false);
    }
}
