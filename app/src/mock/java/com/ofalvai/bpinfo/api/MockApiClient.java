package com.ofalvai.bpinfo.api;

import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.Route;
import com.ofalvai.bpinfo.model.RouteType;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockApiClient implements AlertApiClient {

    private static final String TAG = "MockApiClient";

    private static final List<Alert> TEST_ALERTS = new ArrayList<>();

    private static final int SIMULATE_DELAY_MS = 500;

    /**
     * Simulating different data with each request. Test data is modified in changeState()
     */
    private static final boolean SIMULATE_CHANGES = true;

    /**
     * Current state index of test data, if SIMULATE_CHANGES is set to true.
     */
    private static int mTestDataState = 0;

    /**
     * As with the real API clients, this client gets called by both alert lists in parallel.
     * We need to return data only once.
     */
    private volatile boolean mRequestInProgress = false;

    public MockApiClient() {
        makeTestAlerts();
    }

    @Override
    public void fetchAlertList(@NonNull AlertRequestParams params) {
        if (mRequestInProgress) return;
        mRequestInProgress = true;

        if (SIMULATE_CHANGES) {
            changeState();
        }
        Log.i(TAG, "Mock API request");

        // Simulating delayed response. Without this, mRequestInProgress would be reset to false
        // before the second request calls this method.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                EventBus.getDefault().post(new AlertListMessage(TEST_ALERTS, TEST_ALERTS));
                mRequestInProgress = false;
            }
        }, SIMULATE_DELAY_MS);
    }

    @Override
    public void fetchAlert(@NonNull String id, @NonNull AlertDetailListener listener, @NonNull AlertRequestParams params) {
        for (Alert alert : TEST_ALERTS) {
            if (alert.getId().equals(id)) {
                listener.onAlertResponse(alert);
                break;
            }
        }
    }

    private void makeTestAlerts() {
        Route bus13 = new Route(
                "BKK_0130",
                "13",
                "13-as autóbusz",
                "Budatétény vasútállomás (Campona)<br/>Diósd, Búzavirág utca",
                RouteType.BUS,
                Color.parseColor("#009FE3"),
                Color.parseColor("#FFFFFF")
        );

        Route tram60 = new Route(
                "BKK_3600",
                "60",
                "60-as villamos",
                "Városmajor<br/>Széchenyi-hegy, Gyermekvasút",
                RouteType.TRAM,
                Color.parseColor("#FFD800"),
                Color.parseColor("#000000")
        );

        Route trolley79 = new Route(
                "BKK_4790",
                "79",
                "79-es trolibusz",
                "Keleti pályaudvar M<br/>Kárpát utca",
                RouteType.TROLLEYBUS,
                Color.parseColor("#FF1609"),
                Color.parseColor("#FFFFFF")
        );

        Route ferry11 = new Route(
                "BKK_8110",
                "D11",
                "D11-es hajó",
                "Újpest, Árpád út&nbsp;- Haller utca",
                RouteType.FERRY,
                Color.parseColor("#E50475"),
                Color.parseColor("#FFFFFF")
        );

        Route chairlift = new Route(
                "BKK_E095",
                "Libegő",
                "Libegő",
                "Zugliget&nbsp;- János-hegy",
                RouteType.CHAIRLIFT,
                Color.parseColor("#009155"),
                Color.parseColor("#000000")
        );

        Route funicular = new Route(
                "BKK_????",
                "Fogaskerekű",
                "Fogaskerekű",
                "???",
                RouteType.FUNICULAR,
                Color.parseColor("#884200"),
                Color.parseColor("#000000")
        );

        Route rail5 = new Route(
                "BKK_6470",
                "H5",
                "H5-ös HÉV",
                "Békásmegyer | Batthyány tér",
                RouteType.RAIL,
                Color.parseColor("#821066"),
                Color.parseColor("#FFFFFF")
        );

        Route rainbow1 = new Route(
                "BKK_????",
                "\uD83D\uDE03",
                "X",
                "???",
                RouteType._OTHER_,
                Color.parseColor("#EC1C5A"),
                Color.parseColor("#FFFFFF")
        );

        Route rainbow2 = new Route(
                "BKK_????",
                "\uD83D\uDE0A",
                "X",
                "???",
                RouteType._OTHER_,
                Color.parseColor("#F58023"),
                Color.parseColor("#FFFFFF")
        );

        Route rainbow3 = new Route(
                "BKK_????",
                "\uD83D\uDE42",
                "X",
                "???",
                RouteType._OTHER_,
                Color.parseColor("#FAE83E"),
                Color.parseColor("#FFFFFF")
        );

        Route rainbow4 = new Route(
                "BKK_????",
                "\uD83D\uDE10",
                "X",
                "???",
                RouteType._OTHER_,
                Color.parseColor("#9CC841"),
                Color.parseColor("#FFFFFF")
        );

        Route rainbow5 = new Route(
                "BKK_????",
                "\uD83D\uDE15",
                "X",
                "???",
                RouteType._OTHER_,
                Color.parseColor("#46BEA2"),
                Color.parseColor("#FFFFFF")
        );

        Route rainbow6 = new Route(
                "BKK_????",
                "\uD83D\uDE41",
                "X",
                "???",
                RouteType._OTHER_,
                Color.parseColor("#3D81C2"),
                Color.parseColor("#FFFFFF")
        );

        Route rainbow7 = new Route(
                "BKK_????",
                "\uD83D\uDE43",
                "X",
                "???",
                RouteType._OTHER_,
                Color.parseColor("#644B9E"),
                Color.parseColor("#FFFFFF")
        );

        TEST_ALERTS.add(new Alert(
                "test-xxx",
                1485467157,
                0,
                0,
                null,
                "Ez egy busz",
                "Ez egy mock alert, meglátjuk megy-e",
                new ArrayList<>(Arrays.asList(bus13))
        ));

        TEST_ALERTS.add(new Alert(
                "test-xxx",
                1485567157,
                0,
                0,
                null,
                "Ez egy villamos",
                "Ez egy mock alert, meglátjuk megy-e",
                new ArrayList<>(Arrays.asList(tram60))
        ));

        TEST_ALERTS.add(new Alert(
                "test-xxx",
                1485667157,
                0,
                0,
                null,
                "Ez egy troli",
                "Ez egy mock alert, meglátjuk megy-e",
                new ArrayList<>(Arrays.asList(trolley79))
        ));

        TEST_ALERTS.add(new Alert(
                "test-xxx",
                1485767157,
                0,
                0,
                null,
                "WAT.",
                "Ilyen is kell...",
                new ArrayList<>(Arrays.asList(rainbow1, rainbow2, rainbow3, rainbow4, rainbow5, rainbow6, rainbow7))
        ));

        TEST_ALERTS.add(new Alert(
                "test-xxx",
                1485867157,
                0,
                0,
                null,
                "Na most minden egzotikusat",
                "Ebben most minden benne van!",
                new ArrayList<>(Arrays.asList(ferry11, chairlift, funicular, rail5))
        ));

        for (int i = 0; i < 5; i++) {
            TEST_ALERTS.add(new Alert(
                    "test-xxx",
                    1485567157,
                    0,
                    0,
                    null,
                    "Ez egy villamos " + i,
                    "Ez egy mock alert, meglátjuk megy-e",
                    new ArrayList<>(Arrays.asList(tram60))
            ));
        }
    }

    private void changeState() {
        switch (mTestDataState) {
            case 0:
                mTestDataState++;
                break;
            case 1:
                List<Route> routes = new ArrayList<>();
                routes.addAll(TEST_ALERTS.get(0).getAffectedRoutes());
                routes.addAll(TEST_ALERTS.get(1).getAffectedRoutes());
                routes.addAll(TEST_ALERTS.get(2).getAffectedRoutes());

                Alert newAlert = new Alert(
                        "BKK_????",
                        System.currentTimeMillis(),
                        0,
                        System.currentTimeMillis(),
                        null,
                        "Új elem",
                        "Új elem",
                        routes
                );
                TEST_ALERTS.add(newAlert);

                mTestDataState++;
                break;
            case 2:
                TEST_ALERTS.remove(TEST_ALERTS.size() - 1);

                mTestDataState = 0;
        }
        Log.d(TAG, "New mock data state: " + mTestDataState);
    }

}