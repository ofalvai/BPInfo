package com.example.bkkinfoplus;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.ActionBarContainer;
import android.text.format.DateUtils;

import com.example.bkkinfoplus.ui.alertlist.AlertListActivity;
import com.example.bkkinfoplus.util.AlertListTestHelper;
import com.example.bkkinfoplus.util.ElapsedTimeIdlingResource;

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
import static com.example.bkkinfoplus.util.AlertListTestHelper.withRecyclerView;
import static com.example.bkkinfoplus.util.OrientationChangeAction.orientationLandscape;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

/**
 * Created by oli on 2016. 06. 26..
 */

@RunWith(AndroidJUnit4.class)
public class AlertListTest {

    private IdlingResource mIdlingResource;

    @Rule
    public ActivityTestRule<AlertListActivity> mActivityRule =
            new ActivityTestRule<AlertListActivity>(AlertListActivity.class);

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
