package com.example.bkkinfoplus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.ofalvai.bpinfo.R;
import com.ofalvai.bpinfo.notifications.DescriptionMaker;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DescriptionMakerTest {

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    private Map<String, String> mRouteMap;

    private static final String DATA_KEY_ROUTE_BUS = "route_bus";
    private static final String DATA_KEY_ROUTE_FERRY = "route_ferry";
    private static final String DATA_KEY_ROUTE_RAIL = "route_rail";
    private static final String DATA_KEY_ROUTE_TRAM = "route_tram";
    private static final String DATA_KEY_ROUTE_TROLLEYBUS = "route_trolleybus";
    private static final String DATA_KEY_ROUTE_SUBWAY = "route_subway";
    private static final String DATA_KEY_ROUTE_OTHER = "route_other";

    @BeforeClass
    public static void setUpMocks() {
        mContext = mock(Context.class);
        Resources resources = mock(Resources.class);
        Configuration configuration = mock(Configuration.class);
        Locale locale = new Locale.Builder().setLanguage("hu").build();

        when(mContext.getResources()).thenReturn(resources);
        when(resources.getConfiguration()).thenReturn(configuration);
        configuration.locale = locale;

        when(mContext.getString(R.string.route_bus)).thenReturn("Busz");
        when(mContext.getString(R.string.route_ferry)).thenReturn("Hajó");
        when(mContext.getString(R.string.route_rail)).thenReturn("HÉV");
        when(mContext.getString(R.string.route_tram)).thenReturn("Villamos");
        when(mContext.getString(R.string.route_trolleybus)).thenReturn("Trolibusz");
        when(mContext.getString(R.string.route_subway)).thenReturn("Metró");
        when(mContext.getString(R.string.route_other)).thenReturn("Egyéb");

    }

    @Before
    public void setUpRouteMap() {
        mRouteMap = new HashMap<>();
        mRouteMap.put(DATA_KEY_ROUTE_BUS, "");
        mRouteMap.put(DATA_KEY_ROUTE_FERRY, "");
        mRouteMap.put(DATA_KEY_ROUTE_RAIL, "");
        mRouteMap.put(DATA_KEY_ROUTE_TRAM, "");
        mRouteMap.put(DATA_KEY_ROUTE_TROLLEYBUS, "");
        mRouteMap.put(DATA_KEY_ROUTE_SUBWAY, "");
        mRouteMap.put(DATA_KEY_ROUTE_OTHER, "");
    }

    @Test
    public void single_Bus() {
        mRouteMap.put(DATA_KEY_ROUTE_BUS, "7");

        String result = DescriptionMaker.Companion.makeDescription(mRouteMap, mContext);

        assertEquals("Single bus route is correct", "7-es Busz", result);
    }

    @Test
    public void multiple_trams() {
        mRouteMap.put(DATA_KEY_ROUTE_TRAM, "47|49|56");

        String result = DescriptionMaker.Companion.makeDescription(mRouteMap, mContext);

        String expected = "47-es, 49-es, 56-os Villamos";
        assertEquals("Multiple tram routes are concatenated", expected, result);
    }

    @Test
    public void multiple_SubwaysAndRails() {
        mRouteMap.put(DATA_KEY_ROUTE_SUBWAY, "M2|M4");
        mRouteMap.put(DATA_KEY_ROUTE_RAIL, "H8|H9");

        String result = DescriptionMaker.Companion.makeDescription(mRouteMap, mContext);

        String expected = "M2-es, M4-es Metró\nH8-as, H9-es HÉV";
        assertEquals("Multiple tram and rail routes are concatenated, then broken into lines", expected, result);
    }

    @Test
    public void error_noRouteKeyInMap() {
        mRouteMap.remove(DATA_KEY_ROUTE_SUBWAY);
        mRouteMap.put(DATA_KEY_ROUTE_BUS, "32");

        String result = DescriptionMaker.Companion.makeDescription(mRouteMap, mContext);

        String expected = "32-es Busz";
        assertEquals("No exception shown when a route type is missing, other routes are correct", expected, result);
    }

}
