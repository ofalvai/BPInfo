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
import android.text.format.DateUtils;

import com.ofalvai.bpinfo.util.ElapsedTimeIdlingResource;
import com.ofalvai.bpinfo.ui.alertlist.AlertListActivity;
import com.ofalvai.bpinfo.util.AlertListTestHelper;

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

@RunWith(AndroidJUnit4.class)
public class AlertDetailTest {

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

    @Before
    public void clickOnAlertInList() {
        onView(AlertListTestHelper.withRecyclerView(AlertListTestHelper.RECYCLER_VIEW_ID).atPosition(0)).perform(click());
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

        onView(withId(AlertListTestHelper.RECYCLER_VIEW_ID)).check(matches(isCompletelyDisplayed()));
    }
}
