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
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.ActionBarContainer;
import android.text.format.DateUtils;

import com.ofalvai.bpinfo.ui.alertlist.AlertListActivity;
import com.ofalvai.bpinfo.util.AlertListTestHelper;
import com.ofalvai.bpinfo.util.ElapsedTimeIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.ofalvai.bpinfo.util.AlertListTestHelper.withRecyclerView;
import static com.ofalvai.bpinfo.util.OrientationChangeAction.orientationLandscape;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

@RunWith(AndroidJUnit4.class)
public class AlertListTest {

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

    /**
     * Checks whether the list has items (rows)
     */
    @Test
    public void alertListHasItemTest() {
        onView(withRecyclerView(AlertListTestHelper.RECYCLER_VIEW_ID).atPosition(0))
                .check(matches(isCompletelyDisplayed()));
    }

    /**
     * Checks whether the first item in the list has a title
     */
    @Test
    public void alertListItemTitleTest() {
        onView(withRecyclerView(AlertListTestHelper.RECYCLER_VIEW_ID).atPosition(0))
                .check(matches(hasDescendant(allOf(
                        withId(R.id.list_item_alert_description),
                        isCompletelyDisplayed()
                ))));
    }

    /**
     * Checks whether the first item in the list has a date view
     */
    @Test
    public void alertListItemDateTest() {
        onView(withRecyclerView(AlertListTestHelper.RECYCLER_VIEW_ID).atPosition(0))
                .check(matches(hasDescendant(allOf(
                        withId(R.id.list_item_alert_date),
                        isCompletelyDisplayed()
                ))));
    }

    /**
     * Checks whether the ActionBar's subtitle displays the number of alerts
     */
    @Test
    public void actionBarSubtitleTest() {
        onView(allOf(
                isDescendantOfA(isAssignableFrom(ActionBarContainer.class)),
                withText(endsWith("forgalmi változás van ma")) //TODO: String resource
        )).check(matches(isDisplayed()));
    }

    /**
     * Tries to click on an item in the list
     */
    @Test
    public void itemClickTest() {
        onView(withRecyclerView(AlertListTestHelper.RECYCLER_VIEW_ID).atPosition(0)).perform(click());
    }

    //TODO: doesn't work yet
    @Test
    public void alertListRotationTest() {
        onView(isRoot()).perform(orientationLandscape()).check(matches(isCompletelyDisplayed()));

        onView(withId(AlertListTestHelper.RECYCLER_VIEW_ID)).check(matches(isCompletelyDisplayed()));
    }

}
