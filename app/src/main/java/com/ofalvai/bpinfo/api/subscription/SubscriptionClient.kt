package com.ofalvai.bpinfo.api.subscription

import android.net.Uri
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.google.firebase.iid.FirebaseInstanceId
import com.ofalvai.bpinfo.Config
import com.ofalvai.bpinfo.model.RouteSubscription
import com.ofalvai.bpinfo.util.addTo
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

class SubscriptionClient @Inject constructor(private val requestQueue: RequestQueue) {

    interface Callback {
        fun onSubscriptionError(error: VolleyError)
        fun onPostSubscriptionResponse(subscription: RouteSubscription)
        fun onGetSubscriptionResponse(routeIDList: List<String>)
        fun onDeleteSubscriptionResponse(subscription: RouteSubscription)
    }

    interface TokenReplaceCallback {
        fun onTokenReplaceSuccess()
        fun onTokenReplaceError(error: VolleyError)
    }

    companion object {
        private const val SUBSCRIPTION_URL = Config.Url.BACKEND + "subscription"

        private const val PATH_REPLACE_TOKEN = "replaceToken"

        private const val KEY_ROUTE_ID = "routeId"
        private const val KEY_TOKEN = "token"
        private const val KEY_OLD_TOKEN = "old"
        private const val KEY_NEW_TOKEN = "new"
    }

    private val token get() = FirebaseInstanceId.getInstance().token

    fun postSubscription(routeID: String, callback: Callback) {
        val url = SUBSCRIPTION_URL

        val body = JSONObject().apply {
            put("routeId", routeID)
            put("token", token)
        }

        JsonObjectRequest(Request.Method.POST, url, body,
            Response.Listener {
                val subscription = parseSubscription(it)
                callback.onPostSubscriptionResponse(subscription)
            },
            Response.ErrorListener {
                Timber.e(it.toString())
                callback.onSubscriptionError(it)
            }
        ).addTo(requestQueue)
    }

    fun getSubscriptions(callback: Callback) {
        val url = Uri.parse(SUBSCRIPTION_URL)
            .buildUpon()
            .appendPath(token)
            .toString()

        JsonArrayRequest(Request.Method.GET, url, null,
            Response.Listener {
                callback.onGetSubscriptionResponse(parseSubscriptionList(it))
            },
            Response.ErrorListener {
                Timber.e(it.toString())
                callback.onSubscriptionError(it)
            }
        ).addTo(requestQueue)
    }

    fun deleteSubscription(routeID: String, callback: Callback) {
        val url = Uri.parse(SUBSCRIPTION_URL)
            .buildUpon()
            .appendPath(token)
            .appendPath(routeID)
            .toString()

        JsonObjectRequest(Request.Method.DELETE, url, null,
            Response.Listener {
                val subscription = parseSubscription(it)
                callback.onDeleteSubscriptionResponse(subscription)
            },
            Response.ErrorListener {
                Timber.e(it.toString())
                callback.onSubscriptionError(it)
            }
        ).addTo(requestQueue)
    }

    fun replaceToken(old: String, new: String, callback: TokenReplaceCallback) {
        val url = Uri.parse(SUBSCRIPTION_URL)
            .buildUpon()
            .appendPath(PATH_REPLACE_TOKEN)
            .toString()

        val body = JSONObject().apply {
            put(KEY_OLD_TOKEN, old)
            put(KEY_NEW_TOKEN, new)
        }

        JsonObjectRequest(Request.Method.POST, url, body,
            Response.Listener {
                Timber.i("Token replace success: %s", it.toString())
                callback.onTokenReplaceSuccess()
            },
            Response.ErrorListener {
                Timber.e(it)
                callback.onTokenReplaceError(it)
            }
        ).addTo(requestQueue)
    }

    private fun parseSubscriptionList(array: JSONArray): List<String> {
        val routeIDList = mutableListOf<String>()

        if (array.length() == 0) return routeIDList

        for (i in 0 until array.length()) {
            routeIDList.add(array.getString(i))
        }
        return routeIDList
    }

    private fun parseSubscription(jsonObject: JSONObject): RouteSubscription {
        val routeID = jsonObject.getString(KEY_ROUTE_ID)
        val token = jsonObject.getString(KEY_TOKEN)
        return RouteSubscription(token, routeID)
    }

}
