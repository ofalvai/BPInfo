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

package com.ofalvai.bpinfo.ui.alertlist;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;

import com.ofalvai.bpinfo.BpInfoApplication;
import com.ofalvai.bpinfo.R;

public class NoticeFragment extends DialogFragment {

    @Nullable
    private String mNoticeText;

    private static final String KEY_NOTICE_TEXT = "notice_text";

    public static NoticeFragment newInstance(String noticeText) {
        NoticeFragment noticeFragment = new NoticeFragment();
        noticeFragment.mNoticeText = noticeText;
        return noticeFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.getCharSequence(KEY_NOTICE_TEXT) != null) {
                mNoticeText = savedInstanceState.getCharSequence(KEY_NOTICE_TEXT, "").toString();
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_notice_title)
                .setMessage(Html.fromHtml(mNoticeText))
                .setPositiveButton(R.string.dialog_notice_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(KEY_NOTICE_TEXT, mNoticeText);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BpInfoApplication.Companion.getRefWatcher(getContext()).watch(this);
    }
}
