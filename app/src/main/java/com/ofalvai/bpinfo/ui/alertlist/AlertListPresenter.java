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
import com.ofalvai.bpinfo.BkkInfoApplication;
import com.ofalvai.bpinfo.Config;
import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.api.AlertSearchContract;
import com.ofalvai.bpinfo.api.FutarApiClient;
import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.Route;
import com.ofalvai.bpinfo.model.RouteType;
import com.ofalvai.bpinfo.util.Utils;

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

public class AlertListPresenter implements FutarApiClient.FutarApiCallback {

    @Inject FutarApiClient mFutarApiClient;

    @Inject SharedPreferences mSharedPreferences;

    @Inject Context mContext;

    @NonNull
    private AlertInteractionListener mInteractionListener;

    /**
     * List of alerts returned by the client, before filtering by RouteTypes
     */
    @Nullable
    private List<Alert> mUnfilteredAlerts;

    @Nullable
    private DateTime mLastUpdate;

    @NonNull
    private Set<RouteType> mActiveFilter = new HashSet<>();

    public AlertListPresenter(@NonNull AlertInteractionListener interactionListener) {
        mInteractionListener = interactionListener;

        BkkInfoApplication.injector.inject(this);
    }

    public interface AlertInteractionListener {
        void displayAlerts(@NonNull List<Alert> alerts);

        void displayNetworkError(@NonNull VolleyError error);

        void displayDataError();

        void displayGeneralError();

        void setUpdating(boolean state);

        void displayNoNetworkWarning();
    }

    /**
     * Initiates a network refresh if possible, and returns the alert list to the listener, or
     * calls the appropriate callback.
     */
    public void fetchAlertList() {
        if (Utils.hasNetworkConnection(mContext)) {
            mFutarApiClient.fetchAlertList(this, getCurrentLanguageCode());
        } else if (mUnfilteredAlerts == null) {
            // Nothing was displayed previously, showing a full error view
            mInteractionListener.displayNetworkError(new NoConnectionError());
        } else {
            // A list was loaded previously, we don't clear that, only display a warning.
            mInteractionListener.displayNoNetworkWarning();
        }
    }

    /**
     * If possible, returns the local, filtered state of the alert list to the listener,
     * otherwise calls fetchAlertList() to get data from the API.
     */
    public void getAlertList() {
        if (mUnfilteredAlerts != null) {
            // Filter by route type
            List<Alert> filteredAlerts = filter(mActiveFilter, mUnfilteredAlerts);

            mInteractionListener.displayAlerts(filteredAlerts);
        } else {
            fetchAlertList();
        }

    }

    public void setLastUpdate() {
        mLastUpdate = new DateTime();
    }

    /**
     * Initiates a list update if enough time has passed since the last update
     */
    public void updateIfNeeded() {
        Period updatePeriod = new Period().withSeconds(Config.REFRESH_THRESHOLD_SEC);
        if (mLastUpdate != null && mLastUpdate.plus(updatePeriod).isBeforeNow()) {
            fetchAlertList();
        }
    }

    /**
     * Sets the RouteType filter to be applied to the returned alert list.
     * If an empty Set or null is passed, the list is not filtered.
     */
    public void setFilter(@Nullable Set<RouteType> routeTypes) {
        if (routeTypes == null) {
            mActiveFilter.clear();
        } else {
            mActiveFilter = routeTypes;
        }
    }

    @Nullable
    public Set<RouteType> getFilter() {
        return mActiveFilter;
    }

    /**
     * Transforms the list of returned alert in the following order:
     * 1. Attach Route objects to each Alert based on the alert's route IDs
     * 2. Sort the list by the alerts' start time in descending order
     * 3. Filter the list by the currently active filter
     */
    @Override
    public void onAlertResponse(List<Alert> alerts) {
        mUnfilteredAlerts = alerts;

        attachAffectedRoutesToAlerts(mUnfilteredAlerts);

        // Sort: descending by alert start time
        Collections.sort(mUnfilteredAlerts, new Utils.AlertStartTimestampComparator());
        Collections.reverse(mUnfilteredAlerts);

        // Filter by route type
        List<Alert> filteredAlerts = filter(mActiveFilter, mUnfilteredAlerts);

        mInteractionListener.displayAlerts(filteredAlerts);
    }

    @Override
    public void onError(@NonNull Exception ex) {
        if (mUnfilteredAlerts != null) {
            mUnfilteredAlerts.clear();
        }

        if (ex instanceof VolleyError) {
            VolleyError error = (VolleyError) ex;
            mInteractionListener.displayNetworkError(error);
        } else if (ex instanceof JSONException) {
            mInteractionListener.displayDataError();
        } else {
            mInteractionListener.displayGeneralError();
        }

        Crashlytics.logException(ex);
    }

    /**
     * Alerts returned by the API list affected routes only by their IDs,
     * but this method adds parsed Route object to the Alert objects
     * @param alerts    List of Alerts to apply adding Route objects
     */
    private void attachAffectedRoutesToAlerts(@NonNull List<Alert> alerts) {
        for (Alert alert : alerts) {
            List<Route> affectedRoutes = mFutarApiClient.getAffectedRoutesForAlert(alert);
            alert.setAffectedRoutes(affectedRoutes);
        }
    }

    /**
     * Filters a list of Alerts matching the provided set of RouteTypes
     * @param types RouteTypes to match
     * @param alerts List of Alerts to filter
     * @return Filtered list of Alerts
     */
    @NonNull
    private List<Alert> filter(@Nullable Set<RouteType> types, @NonNull List<Alert> alerts) {
        if (types == null || types.isEmpty()) {
            return alerts;
        }

        List<Alert> filtered = new ArrayList<>();

        for (Alert alert : alerts) {
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
}
