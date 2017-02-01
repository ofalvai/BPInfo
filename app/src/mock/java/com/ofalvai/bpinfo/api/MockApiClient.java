package com.ofalvai.bpinfo.api;

import android.graphics.Color;
import android.support.annotation.NonNull;

import com.ofalvai.bpinfo.model.Alert;
import com.ofalvai.bpinfo.model.Route;
import com.ofalvai.bpinfo.model.RouteType;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class MockApiClient implements AlertApiClient {

    private static final List<Alert> TEST_ALERTS = new ArrayList<>();

    public MockApiClient() {
        makeTestAlerts();
    }

    @Override
    public void fetchAlertList(@NonNull AlertRequestParams params) {
        EventBus.getDefault().post(new AlertListMessage(TEST_ALERTS, TEST_ALERTS));
    }

    @Override
    public void fetchAlert(@NonNull String id, @NonNull AlertDetailListener listener, @NonNull AlertRequestParams params) {
        listener.onAlertResponse(TEST_ALERTS.get(0));
    }

    private void makeTestAlerts() {
        Route route1 = new Route(
                "123",
                "HA-HA",
                "HA-HA-HA-HA",
                "Ez egy mock járat",
                RouteType.BUS,
                Color.parseColor("#009FE3"),
                Color.parseColor("#FFFFFF")
        );

        List<Route> routes = new ArrayList<>();
        routes.add(route1);


        TEST_ALERTS.add(new Alert(
                "123",
                0,
                0,
                0,
                null,
                "Mock alert",
                "Ez egy mock alert, meglátjuk megy-e",
                routes
        ));
    }
}