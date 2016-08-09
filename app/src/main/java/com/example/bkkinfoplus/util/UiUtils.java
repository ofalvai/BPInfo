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

package com.example.bkkinfoplus.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bkkinfoplus.Config;
import com.example.bkkinfoplus.R;
import com.example.bkkinfoplus.model.Route;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/**
 * Created by oli on 2016. 06. 18..
 */
public class UiUtils {



    /**
     * Transforms the start and end timestamps into a human-friendly readable string,
     * with special replacements for special dates, times, and the API's strange notations.
     * @param context
     * @param startTimestamp Start of the alert in seconds since the UNIX epoch
     * @param endTimestamp   End of the alert in seconds since the UNIX epoch
     * @return  A string in the format of [startdate] [starttime] [separator] [enddate] [endtime]
     */
    @NonNull
    public static String alertDateFormatter(Context context, long startTimestamp, long endTimestamp) {
        DateTime startDateTime = new DateTime(startTimestamp * 1000L);
        DateTime endDateTime = new DateTime(endTimestamp * 1000L);
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        DateTime today = new DateTime();
        LocalDate todayDate = new DateTime().toLocalDate();
        DateTime yesterday = today.minusDays(1);
        LocalDate yesterdayDate = yesterday.toLocalDate();
        DateTime tomorrow = today.plusDays(1);
        LocalDate tomorrowDate = tomorrow.toLocalDate();

        // Alert start, date part
        String startDateString;
        if (startDate.equals(todayDate)) {
            // Start day is today, replacing month and day with today string
            startDateString = context.getString(R.string.date_today) + " ";
        } else if (startDate.year().get() < today.year().get()) {
            // The start year is less than the current year, displaying the year too
            startDateString = Config.FORMATTER_DATE_YEAR.print(startDateTime);
        } else if (startDate.equals(yesterdayDate)) {
            startDateString = context.getString(R.string.date_yesterday) + " ";
        } else if (startDate.equals(tomorrowDate)) {
            startDateString = context.getString(R.string.date_tomorrow) + " ";
        } else {
            startDateString = Config.FORMATTER_DATE.print(startDateTime);
        }

        // Alert start, time part
        String startTimeString;
        if (startDateTime.hourOfDay().get() == 0 && startDateTime.minuteOfHour().get() == 0) {
            // The API marks "first departure" as 00:00
            startTimeString = context.getString(R.string.date_first_departure);
        } else {
            startTimeString = Config.FORMATTER_TIME.print(startDateTime);
        }

        // Alert end, date part
        String endDateString;
        if (endTimestamp == 0) {
            // The API marks "until further notice" as 0 (in UNIX epoch), no need to display date
            // (the replacement string is displayed as the time part, not the date)
            endDateString = " ";
        } else if (endDate.year().get() > today.year().get()) {
            // The end year is greater than the current year, displaying the year too
            endDateString = Config.FORMATTER_DATE_YEAR.print(endDateTime);
        } else if (endDate.equals(todayDate)) {
            // End  day is today, replacing month and day with today string
            endDateString = context.getString(R.string.date_today) + " ";
        } else if (endDate.equals(yesterdayDate)) {
            endDateString = context.getString(R.string.date_yesterday) + " ";
        } else if (endDate.equals(tomorrowDate)) {
            endDateString = context.getString(R.string.date_tomorrow) + " ";
        } else {
            endDateString = Config.FORMATTER_DATE.print(endDateTime);
        }

        // Alert end, time part
        String endTimeString;
        if (endTimestamp == 0) {
            // The API marks "until further notice" as 0 (in UNIX epoch)
            endTimeString = context.getString(R.string.date_until_revoke);
        } else if (endDateTime.hourOfDay().get() == 23 && endDateTime.minuteOfHour().get() == 59) {
            // The API marks "last departure" as 23:59
            endTimeString = context.getString(R.string.date_last_departure);
        } else {
            endTimeString = Config.FORMATTER_TIME.print(endDateTime);
        }

        return startDateString + startTimeString + Config.DATE_SEPARATOR + endDateString + endTimeString;
    }

    /**
     * Adds a rectangular icon for the affected route.
     *
     * First it inflates the view, then sets the text and color properties of the view.
     * The custom colored rounded background is achieved by a Drawable and a ColorFilter on top of that.
     * @param context   Needed for inflating
     * @param root      This is where the view will be added
     * @param route     Route object containing the color and text attributes
     */
    public static void addRouteIcon(Context context, @NonNull ViewGroup root, @NonNull Route route) {
        View.inflate(
                context, R.layout.list_item_route_icon, root
        );

        // Finding the inflated view, because inflate() returns its root, not the inflated view
        TextView iconView = (TextView) root.getChildAt(root.getChildCount() - 1);

        iconView.setText(route.getShortName());
        iconView.setTextColor(Color.parseColor("#" + route.getTextColor()));

        // Setting a custom colored rounded background drawable as background
        Drawable iconBackground = context.getResources().getDrawable(R.drawable.rounded_corner_5dp);
        if (iconBackground != null) {
            ColorFilter colorFilter = new LightingColorFilter(Color.rgb(1, 1, 1),
                    Color.parseColor("#" + route.getColor()));
            iconBackground.mutate().setColorFilter(colorFilter);
            iconView.setBackground(iconBackground);
        }
    }

    /**
     * Opens a Chrome custom tab styled to the application's theme
     * @param activity  Used for context and launching fallback intent
     * @param url   URL to open
     */
    public static void openCustomTab(Activity activity, Uri url) {
        String packageName = CustomTabsHelper.getPackageNameToUse(activity);

        if (packageName == null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(url);
            activity.startActivity(intent);
        } else {
            CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
            int color = ContextCompat.getColor(activity, R.color.colorPrimary);
            intentBuilder.setToolbarColor(color);

            CustomTabsIntent customTabsIntent = intentBuilder.build();
            customTabsIntent.intent.setPackage(packageName);
            customTabsIntent.launchUrl(activity, url);
        }
    }
}
