package com.example.bkkinfoplus.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bkkinfoplus.R;
import com.example.bkkinfoplus.model.Route;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by oli on 2016. 06. 18..
 */
public class UiUtils {

    public static final String DATE_FORMAT = "MMM. d. E HH:mm";

    public static String createAlertDateString(Context context, long timestampStart, long timestampEnd) {
        Date start = new Date(timestampStart * 1000);
        DateFormat format = new SimpleDateFormat(DATE_FORMAT);
        String startText = format.format(start);
        String endText;
        if (timestampEnd == 0) {
            endText = context.getString(R.string.alert_label_until_revoke);
        } else {
            Date end = new Date(timestampEnd * 1000);
            endText = format.format(end);
        }
        return context.getResources().getString(R.string.alert_latel_date, startText, endText);
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
    public static void addRouteIcon(Context context, ViewGroup root, Route route) {
        View.inflate(
                context, R.layout.list_item_route_icon, root
        );

        // Finding the inflated view, because inflate() returns its root, not the inflated view
        TextView iconView = (TextView) root.getChildAt(root.getChildCount() - 1);

        iconView.setText(route.getShortName());
        iconView.setTextColor(Color.parseColor("#" + route.getTextColor()));

        // Setting a custom colored rounded background drawable as background
        Drawable iconBackground = context.getResources().getDrawable(R.drawable.rounded_corner_5dp);
        ColorFilter colorFilter = new LightingColorFilter(Color.rgb(1, 1, 1),
                Color.parseColor("#" + route.getColor()));
        iconBackground.mutate().setColorFilter(colorFilter);
        iconView.setBackground(iconBackground);

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
