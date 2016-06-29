package com.example.bkkinfoplus;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.format.DateUtils;

import com.example.bkkinfoplus.ui.alertlist.AlertListActivity;
import com.example.bkkinfoplus.util.ElapsedTimeIdlingResource;
import com.example.bkkinfoplus.util.RecyclerViewMatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;

/**
 * Created by oli on 2016. 06. 27..
 */

@RunWith(AndroidJUnit4.class)
public class AlertDetailTest {

    private static final int RECYCLER_VIEW_ID = R.id.alerts_recycler_view;

    private IdlingResource mIdlingResource;

    // Convenience helper
    private static RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {
        return new RecyclerViewMatcher(recyclerViewId);
    }

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

    @Before
    public void clickOnAlertInList() {
        onView(withRecyclerView(RECYCLER_VIEW_ID).atPosition(0)).perform(click());
    }

    @After
    public void tearDown() {
        Espresso.unregisterIdlingResources(mIdlingResource);
        mIdlingResource = null;

    }

    @Test
    public void alertDetailHasTitleTest() {
        onView(withId(R.id.alert_detail_title)).check(
                matches(allOf(
                    isCompletelyDisplayed(),
                    not(withText(""))
                ))
        );
    }

    @Test
    public void alertDetailHasDateTest() {
        onView(withId(R.id.alert_detail_date)).check(
                matches(allOf(
                        isCompletelyDisplayed(),
                        not(withText(""))
                ))
        );
    }

    @Test
    public void alertDetailHasDescriptionTest() {
        onView(withId(R.id.alert_detail_description)).check(
                matches(allOf(
                        isDisplayed(), // the whole view not necessarily fit the screen
                        not(withText(""))
                ))
        );
    }

    // TODO: doesn't stop
    //@Test
    //public void alertDetailLinkClickTest() {
    //    // Can't use scrollTo() here, because BottomSheet is not a ScrollView
    //    onView(withId(R.id.alert_detail_title)).perform(swipeUp(), swipeUp());
    //
    //    onView(withId(R.id.alert_detail_url)).perform(click());
    //
    //    onView(isAssignableFrom(ImageButton.class)).check(matches(isCompletelyDisplayed()));
    //}

    @Test
    public void alertDetailBackButtonTest() {
        onView(withId(R.id.alert_detail_title)).perform(pressBack());

        onView(withId(RECYCLER_VIEW_ID)).check(matches(isCompletelyDisplayed()));
    }
}
