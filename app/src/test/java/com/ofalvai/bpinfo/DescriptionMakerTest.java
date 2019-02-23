package com.ofalvai.bpinfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

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
    private static Context context;

    private Map<String, String> routeMap;

    private static final String DATA_KEY_ROUTE_BUS = "route_bus";
    private static final String DATA_KEY_ROUTE_FERRY = "route_ferry";
    private static final String DATA_KEY_ROUTE_RAIL = "route_rail";
    private static final String DATA_KEY_ROUTE_TRAM = "route_tram";
    private static final String DATA_KEY_ROUTE_TROLLEYBUS = "route_trolleybus";
    private static final String DATA_KEY_ROUTE_SUBWAY = "route_subway";
    private static final String DATA_KEY_ROUTE_OTHER = "route_other";

    @BeforeClass
    public static void setUpMocks() {
        context = mock(Context.class);
        Resources resources = mock(Resources.class);
        Configuration configuration = mock(Configuration.class);
        Locale locale = new Locale.Builder().setLanguage("hu").build();

        when(context.getResources()).thenReturn(resources);
        when(resources.getConfiguration()).thenReturn(configuration);
        configuration.locale = locale;

        when(context.getString(R.string.route_bus_alt)).thenReturn("busz");
        when(context.getString(R.string.route_ferry_alt)).thenReturn("hajó");
        when(context.getString(R.string.route_rail_alt)).thenReturn("HÉV");
        when(context.getString(R.string.route_tram_alt)).thenReturn("villamos");
        when(context.getString(R.string.route_trolleybus_alt)).thenReturn("trolibusz");
        when(context.getString(R.string.route_subway_alt)).thenReturn("metró");
        when(context.getString(R.string.route_other)).thenReturn("Egyéb");
    }

    @Before
    public void setUpRouteMap() {
        routeMap = new HashMap<>();
        routeMap.put(DATA_KEY_ROUTE_BUS, "");
        routeMap.put(DATA_KEY_ROUTE_FERRY, "");
        routeMap.put(DATA_KEY_ROUTE_RAIL, "");
        routeMap.put(DATA_KEY_ROUTE_TRAM, "");
        routeMap.put(DATA_KEY_ROUTE_TROLLEYBUS, "");
        routeMap.put(DATA_KEY_ROUTE_SUBWAY, "");
        routeMap.put(DATA_KEY_ROUTE_OTHER, "");
    }

    @Test
    public void single_bus() {
        routeMap.put(DATA_KEY_ROUTE_BUS, "7");

        String result = DescriptionMaker.makeDescription(routeMap, context);

        assertEquals("Single bus route is correct", "7-es busz", result);
    }

    @Test
    public void multiple_trams() {
        routeMap.put(DATA_KEY_ROUTE_TRAM, "47|49|56");

        String result = DescriptionMaker.makeDescription(routeMap, context);

        String expected = "47-es, 49-es, 56-os villamos";
        assertEquals("Multiple tram routes are concatenated", expected, result);
    }

    @Test
    public void multiple_routeNumbersWithLetterEndings() {
        routeMap.put(DATA_KEY_ROUTE_TRAM, "12M|14M");
        routeMap.put(DATA_KEY_ROUTE_BUS, "30A|84E");

        String result = DescriptionMaker.makeDescription(routeMap, context);

        String expected = "30A, 84E busz\n12M, 14M villamos";
        assertEquals("Numbered routes with letter endings don't have postfixes", expected, result);
    }

    @Test
    public void multiple_SubwaysAndRails() {
        routeMap.put(DATA_KEY_ROUTE_SUBWAY, "M2|M4");
        routeMap.put(DATA_KEY_ROUTE_RAIL, "H8|H9");

        String result = DescriptionMaker.makeDescription(routeMap, context);

        String expected = "M2-es, M4-es metró\nH8-as, H9-es HÉV";
        assertEquals("Multiple tram and rail routes are concatenated, then broken into lines", expected, result);
    }

    @Test
    public void multiple_duplicates() {
        routeMap.put(DATA_KEY_ROUTE_RAIL, "H8|H8|H9");

        String result = DescriptionMaker.makeDescription(routeMap, context);

        String expected = "H8-as, H9-es HÉV";
        assertEquals("Duplicate routes are omitted", expected, result);
    }

    @Test
    public void error_noRouteKeyInMap() {
        routeMap.remove(DATA_KEY_ROUTE_SUBWAY);
        routeMap.put(DATA_KEY_ROUTE_BUS, "32");

        String result = DescriptionMaker.makeDescription(routeMap, context);

        String expected = "32-es busz";
        assertEquals("No exception shown when a route type is missing, other routes are correct", expected, result);
    }

    @Test
    public void error_invalidSeparatorInRouteList() {
        routeMap.put(DATA_KEY_ROUTE_BUS, "32|");

        String result = DescriptionMaker.makeDescription(routeMap, context);

        String expected = "32-es busz";
        assertEquals("No exception shown when route list ends with separator", expected, result);
    }

    @Test
    public void error_invalidSeparatorInRouteList2() {
        routeMap.put(DATA_KEY_ROUTE_BUS, "|32");

        String result = DescriptionMaker.makeDescription(routeMap, context);

        String expected = "32-es busz";
        assertEquals("No exception shown when route list starts with separator", expected, result);
    }

    @Test
    public void otherTypes() {
        routeMap.put(DATA_KEY_ROUTE_OTHER, "Libegő|Sikló|BP100|N17|Sétajárat");

        String result = DescriptionMaker.makeDescription(routeMap, context);

        String expected = "Libegő, Sikló, BP100-as, N17-es, Sétajárat";
        assertEquals("Routes of type OTHER are listed without the route type", expected, result);
    }

    @Test
    public void otherTypes_wildShortNames() {
        routeMap.put(DATA_KEY_ROUTE_OTHER, "❄");
        routeMap.put(DATA_KEY_ROUTE_BUS, " 7 ");

        String result = DescriptionMaker.makeDescription(routeMap, context);

        String expected = "7-es busz\n❄";
        assertEquals("Routes of type OTHER are listed without the route type", expected, result);
    }
}
