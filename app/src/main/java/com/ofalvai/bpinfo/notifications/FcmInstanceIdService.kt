package com.ofalvai.bpinfo.notifications

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.ofalvai.bpinfo.util.LogUtils.LOGD

class FcmInstanceIdService: FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        super.onTokenRefresh()

        val token = FirebaseInstanceId.getInstance().token
        LOGD("FcmInstanceIdService", "Token: $token")
    }
}