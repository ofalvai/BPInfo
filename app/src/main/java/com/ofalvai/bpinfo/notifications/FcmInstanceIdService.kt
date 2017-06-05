package com.ofalvai.bpinfo.notifications

import com.google.firebase.iid.FirebaseInstanceIdService

class FcmInstanceIdService: FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        super.onTokenRefresh()
    }
}