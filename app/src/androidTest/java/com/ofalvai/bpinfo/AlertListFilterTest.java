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

package com.ofalvai.bpinfo;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.text.format.DateUtils;

import com.ofalvai.bpinfo.ui.alertlist.AlertListActivity;
import com.ofalvai.bpinfo.util.AlertListTestHelper;
import com.ofalvai.bpinfo.util.ElapsedTimeIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.ofalvai.bpinfo.util.AlertListTestHelper.withRecyclerView;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsNot.not;

public class AlertListFilterTest {

    private IdlingResource mIdlingResource;

    @Rule
    public ActivityTestRule<AlertListActivity> mActivityRule =
            new ActivityTestRule<>(AlertListActivity.class);

    /**
     * Delays the test so that the RecyclerView has items
     */
    @Before
    public void waitForNetworkResponse() {
        // TODO: event based waiting

        long waitingTime = DateUtils.SECOND_IN_MILLIS * 1;

        mIdlingResource = new ElapsedTimeIdlingResource(waitingTime);
        Espresso.registerIdlingResources(mIdlingResource);
    }

    @After
    public void tearDown() {
        Espresso.unregisterIdlingResources(mIdlingResource);
        mIdlingResource = null;

    }

    private void performApplyFilter(int routeTypeResId) {
        // Click filter toolbar button
        onView(withId(R.id.menu_item_filter_alerts)).perform(click());
        // Select one route type
        onView(withText(routeTypeResId)).perform(click());
        // Click positive button of dialog, apply filter
        onView(withText(R.string.filter_positive_button)).perform(click());
    }

    @Test
    public void filterDialogDisplayTest() {
        onView(withId(R.id.menu_item_filter_alerts)).perform(click());

        onView(withText(R.string.filter_title)).inRoot(isDialog()).check(matches(isDisplayed()));
    }

    /**
     * Checks whether every route type is displayed as a line
     */
    @Test
    public void filterDialogHasAllItemsTest() {
        onView(withId(R.id.menu_item_filter_alerts)).perform(click());

        onView(withText(R.string.route_subway)).check(matches(isDisplayed()));
        onView(withText(R.string.route_tram)).check(matches(isDisplayed()));
        onView(withText(R.string.route_trolleybus)).check(matches(isDisplayed()));
        onView(withText(R.string.route_bus)).check(matches(isDisplayed()));
        onView(withText(R.string.route_rail)).check(matches(isDisplayed()));
        onView(withText(R.string.route_ferry)).check(matches(isDisplayed()));
    }

    /**
     * Checks the alert list after applying a filter. Either a list item or a placeholder message
     * should be displayed.
     */
    @Test
    public void filteredListHasItemsOrMessageTest() {
        performApplyFilter(R.string.route_bus);

        onView(isRoot()).check(matches(anyOf(
                // Empty view of the list
                hasDescendant(withText(R.string.alert_list_empty)),
                // One item in the list
                hasDescendant(withRecyclerView(AlertListTestHelper.RECYCLER_VIEW_ID).atPosition(0))
        )));
    }

    /**
     * Checks the appearance and disappearance of the filter warning bar below the toolbar.
     */
    @Test
    public void filterWarningMessageTest() {
        int routeTypeResId = R.string.route_ferry;
        performApplyFilter(routeTypeResId);

        onView(withId(R.id.alert_list_filter_active_message)).check(matches(allOf(
                isDisplayed(),
                withText(startsWith("Szűrés: "))
        )));

        // Disabling the filter
        performApplyFilter(routeTypeResId);
        onView(withId(R.id.alert_list_filter_active_message)).check(matches(not(isDisplayed())));
    }
}
