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

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.NoConnectionError;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.ofalvai.bpinfo.BpInfoApplication;
import com.ofalvai.bpinfo.Config;
import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.api.AlertApiClient;
import com.ofalvai.bpinfo.api.AlertListErrorMessage;
import com.ofalvai.bpinfo.api.AlertListMessage;
import com.ofalvai.bpinfo.api.AlertRequestParams;
import com.ofalvai.bpinfo.api.NoticeClient;
import com.ofalvai.bpinfo.api.bkkfutar.AlertSearchContract;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.Route;
import com.ofalvai.bpinfo.model.RouteType;
import com.ofalvai.bpinfo.ui.base.BasePresenter;
import com.ofalvai.bpinfo.util.UtilsKt;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

public class AlertListPresenter extends BasePresenter<AlertListContract.View>
        implements NoticeClient.NoticeListener, AlertListContract.Presenter {

    @Inject
    AlertApiClient mAlertApiClient;

    @Inject NoticeClient mNoticeClient;

    @Inject
    SharedPreferences mSharedPreferences;

    @Inject
    Context mContext;

    private final AlertListType mAlertListType;

    /**
     * List of alerts returned by the client, before filtering by RouteTypes
     */
    @Nullable
    private List<Alert> mUnfilteredAlerts;

    @Nullable
    private DateTime mLastUpdate;

    @NonNull
    private Set<RouteType> mActiveFilter = new HashSet<>();

    AlertListPresenter(@NonNull AlertListType alertListType) {
        mAlertListType = alertListType;

        BpInfoApplication.getInjector().inject(this);
    }

    @Override
    public void attachView(@NonNull AlertListContract.View view) {
        super.attachView(view);
        EventBus.getDefault().register(this);
    }

    @Override
    public void detachView() {
        super.detachView();
        EventBus.getDefault().unregister(this);
    }

    /**
     * Initiates a network refresh if possible, and returns the alert list to the listener, or
     * calls the appropriate callback.
     */
    @Override
    public void fetchAlertList() {
        if (UtilsKt.hasNetworkConnection(mContext)) {
            mAlertApiClient.fetchAlertList(getAlertRequestParams());
        } else if (mUnfilteredAlerts == null) {
            // Nothing was displayed previously, showing a full error view
            if (getView() != null) {
                getView().displayNetworkError(new NoConnectionError());
            }
        } else {
            // A list was loaded previously, we don't clear that, only display a warning.
            if (getView() != null) {
                getView().displayNoNetworkWarning();
            }
        }
    }

    /**
     * If possible, returns the local, filtered state of the alert list to the listener,
     * otherwise calls fetchAlertList() to get data from the API.
     */
    @Override
    public void getAlertList() {
        if (mUnfilteredAlerts != null) {
            List<Alert> processedAlerts = filterAndSort(mActiveFilter, mUnfilteredAlerts, mAlertListType);
            if (getView() != null) {
                getView().displayAlerts(processedAlerts);
            }
        } else {
            fetchAlertList();
        }
    }

    @Override
    public void fetchAlert(String alertId) {
        mAlertApiClient.fetchAlert(
                alertId,
                new AlertApiClient.AlertDetailListener() {
                    @Override
                    public void onAlertResponse(Alert alert) {
                        if (getView() != null) {
                            getView().updateAlertDetail(alert);
                        }
                    }

                    @Override
                    public void onError(Exception ex) {
                        if (getView() != null) {
                            getView().displayAlertDetailError();
                        }
                        Timber.e(ex.toString());
                        Crashlytics.logException(ex);
                    }
                },
                getAlertRequestParams()
        );
    }

    @Override
    public void setLastUpdate() {
        mLastUpdate = new DateTime();
    }

    /**
     * Initiates a list update if enough time has passed since the last update
     */
    @Override
    public boolean updateIfNeeded() {
        Period updatePeriod = new Period().withSeconds(Config.REFRESH_THRESHOLD_SEC);
        if (mLastUpdate != null && mLastUpdate.plus(updatePeriod).isBeforeNow()) {
            fetchAlertList();
            fetchNotice();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the RouteType filter to be applied to the returned alert list.
     * If an empty Set or null is passed, the list is not filtered.
     */
    @Override
    public void setFilter(@Nullable Set<RouteType> routeTypes) {
        if (routeTypes == null) {
            mActiveFilter.clear();
        } else {
            mActiveFilter = routeTypes;
        }
    }

    @Nullable
    @Override
    public Set<RouteType> getFilter() {
        return mActiveFilter;
    }

    /**
     * Transforms the list of returned alerts in the following order:
     * 1. Sort the list by the alerts' start time
     * 2. Filter the list by the currently active filter
     */
    @Subscribe
    public void onAlertListEvent(AlertListMessage message) {
        if (mAlertListType.equals(AlertListType.ALERTS_TODAY)) {
            mUnfilteredAlerts = message.todayAlerts;
        } else if (mAlertListType.equals(AlertListType.ALERTS_FUTURE)){
            mUnfilteredAlerts = message.futureAlerts;
        }

        if (mUnfilteredAlerts != null) {
            List<Alert> processedAlerts = filterAndSort(mActiveFilter, mUnfilteredAlerts, mAlertListType);
            if (getView() != null) {
                getView().displayAlerts(processedAlerts);
            }
        } else {
            Timber.e("Unfiltered alerts is null");
        }
    }

    @Subscribe
    public void onAlertListErrorEvent(AlertListErrorMessage message) {
        final Exception ex = message.mException;
        Timber.e(ex.toString());

        if (mUnfilteredAlerts != null) {
            mUnfilteredAlerts.clear();
        }

        if (getView() != null) {
            if (ex instanceof VolleyError) {
                VolleyError error = (VolleyError) ex;
                getView().displayNetworkError(error);
            } else if (ex instanceof JSONException) {
                getView().displayDataError();
                Crashlytics.logException(ex);
            } else {
                getView().displayGeneralError();
                Crashlytics.logException(ex);
            }
        }
    }

    /**
     * Returns a new filtered list of Alerts matching the provided set of RouteTypes, and sorted
     * according to the alert list type (descending/ascending)
     */
    @NonNull
    private List<Alert> filterAndSort(@Nullable Set<RouteType> types,
                                             @NonNull List<Alert> alerts,
                                             @NonNull AlertListType type) {
        List<Alert> sorted = new ArrayList<>(alerts);

        // Sort: descending by alert start time
        Collections.sort(sorted, UtilsKt.getAlertStartComparator());
        if (type == AlertListType.ALERTS_TODAY) {
            Collections.reverse(sorted);
        }

        if (types == null || types.isEmpty()) {
            return sorted;
        }

        List<Alert> filtered = new ArrayList<>();

        for (Alert alert : sorted) {
            for (Route route : alert.getAffectedRoutes()) {
                if (types.contains(route.getType())) {
                    filtered.add(alert);
                    break;
                }
            }
        }

        return filtered;
    }

    /**
     * Gets the current language's language code.
     * If a language has been set in the preferences, it reads the value from SharedPreferences.
     * If it has been set to "auto" or unset, it decides based on the current locale, using "en" for
     * any other language than Hungarian ("hu")
     * @return The app's current language's code.
     */
    @NonNull
    private String getCurrentLanguageCode() {
        String languageCode = mSharedPreferences.getString(
                mContext.getString(R.string.pref_key_language),
                mContext.getString(R.string.pref_key_language_auto)
        );

        if (languageCode.equals(mContext.getString(R.string.pref_key_language_auto))) {
            Locale locale = Locale.getDefault();

            if (locale.getLanguage().equals(AlertSearchContract.LANG_HU)) {
                languageCode = AlertSearchContract.LANG_HU;
            } else {
                languageCode = AlertSearchContract.LANG_EN;
            }
        }
        
        return languageCode;
    }

    @Override
    public void fetchNotice() {
        // We only need to display one dialog per activity
        if (mAlertListType.equals(AlertListType.ALERTS_TODAY)) {
            mNoticeClient.fetchNotice(this, getCurrentLanguageCode());
        }
    }

    @Override
    public void onNoticeResponse(String noticeText) {
        if (getView() != null) {
            getView().displayNotice(noticeText);
        }
    }

    @Override
    public void onNoNotice() {
        if (getView() != null) {
            getView().removeNotice();
        }
    }

    private AlertRequestParams getAlertRequestParams() {
        return new AlertRequestParams(mAlertListType, getCurrentLanguageCode());
    }
}
