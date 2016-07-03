package com.example.bkkinfoplus.ui.alertlist;

import com.android.volley.VolleyError;
import com.example.bkkinfoplus.BkkInfoApplication;
import com.example.bkkinfoplus.FutarApiClient;
import com.example.bkkinfoplus.Utils;
import com.example.bkkinfoplus.model.Alert;
import com.example.bkkinfoplus.model.Route;
import com.example.bkkinfoplus.model.RouteType;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Created by oli on 2016. 06. 20..
 */
public class AlertListPresenter implements FutarApiClient.FutarApiCallback {

    private static final int REFRESH_THRESHOLD_SEC = 30;

    @Inject
    FutarApiClient mFutarApiClient;

    private AlertInteractionListener mInteractionListener;

    /**
     * List of alerts returned by the client, before filtering by RouteTypes
     */
    private List<Alert> mUnfilteredAlerts;

    private long mLastUpdate;

    private Set<RouteType> mActiveFilter = new HashSet<>();

    public AlertListPresenter(AlertInteractionListener interactionListener) {
        mInteractionListener = interactionListener;

        BkkInfoApplication.injector.inject(this);
    }

    public interface AlertInteractionListener {
        void displayAlerts(List<Alert> alerts);

        void displayNetworkError(VolleyError error);

        void displayDataError();

        void displayGeneralError();

        void setUpdating(boolean state);
    }

    /**
     * Initiates a network refresh and returns the alert list to the listener
     */
    public void fetchAlertList() {
        mFutarApiClient.fetchAlertList(this);
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
        mLastUpdate = new GregorianCalendar().getTimeInMillis();
    }

    public void checkIfUpdateNeeded() {
        long currentTimestamp = new GregorianCalendar().getTimeInMillis();
        long diff = currentTimestamp - mLastUpdate;
        TimeUnit secondUnit = TimeUnit.SECONDS;
        long diffInSeconds = secondUnit.convert(diff, TimeUnit.MILLISECONDS);

        if (diffInSeconds > REFRESH_THRESHOLD_SEC) {
            mInteractionListener.setUpdating(true);
            fetchAlertList();
        }
    }

    /**
     * Sets the RouteType filter to be applied to the returned alert list.
     * If an empty Set or null is passed, the list is not filtered.
     * @param routeTypes
     */
    public void setFilter(Set<RouteType> routeTypes) {
        mActiveFilter = routeTypes;
    }

    public Set<RouteType> getFilter() {
        return mActiveFilter;
    }

    /**
     * Transforms the list of returned alert in the following order:
     * 1. Attach Route objects to each Alert based on the alert's route IDs
     * 2. Sort the list by the alerts' start time in descending order
     * 3. Filter the list by the currently active filter
     * @param alerts
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
    public void onError(Exception ex) {
        mUnfilteredAlerts.clear();

        if (ex instanceof VolleyError) {
            VolleyError error = (VolleyError) ex;
            mInteractionListener.displayNetworkError(error);
        } else if (ex instanceof JSONException) {
            mInteractionListener.displayDataError();
        } else {
            mInteractionListener.displayGeneralError();
        }
    }

    /**
     * Alerts returned by the API list affected routes only by their IDs,
     * but this method adds parsed Route object to the Alert objects
     * @param alerts    List of Alerts to apply adding Route objects
     */
    private void attachAffectedRoutesToAlerts(List<Alert> alerts) {
        for (Alert alert : alerts) {
            List<Route> affectedRoutes = mFutarApiClient.getAffectedRoutesForAlert(alert);
            alert.setAffectedRoutes(affectedRoutes);
        }
    }

    private List<Alert> filter(Set<RouteType> types, List<Alert> alerts) {
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
}
