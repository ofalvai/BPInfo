package com.example.bkkinfoplus.ui.alertlist;

import com.example.bkkinfoplus.model.Alert;
import com.example.bkkinfoplus.BkkInfoApplication;
import com.example.bkkinfoplus.FutarApiClient;
import com.example.bkkinfoplus.model.Route;

import java.util.GregorianCalendar;
import java.util.List;
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

    private long mLastUpdate;

    public AlertListPresenter(AlertInteractionListener interactionListener) {
        // TODO: injection
        mInteractionListener = interactionListener;
    }

    public interface AlertInteractionListener {
        void displayAlerts(List<Alert> alerts);

        void displayNetworkError();

        void displayGeneralError();

        void setUpdating(boolean state);
    }

    public void getAlertList() {
        // TODO: temporary
        BkkInfoApplication.injector.inject(this);
        mFutarApiClient.fetchAlertList(this);
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
            getAlertList();
        }
    }

    @Override
    public void onAlertResponse(List<Alert> alerts) {
        attachAffectedRoutesToAlerts(alerts);

        mInteractionListener.displayAlerts(alerts);
    }

    @Override
    public void onError(Exception ex) {
        // TODO: separate error causes
        mInteractionListener.displayGeneralError();
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
}
