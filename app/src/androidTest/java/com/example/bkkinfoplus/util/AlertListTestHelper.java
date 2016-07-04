package com.example.bkkinfoplus.util;

import com.example.bkkinfoplus.R;

/**
 * Created by oli on 2016. 07. 04..
 */
public class AlertListTestHelper {

    public static final int RECYCLER_VIEW_ID = R.id.alerts_recycler_view;

    // Convenience helper
    public static RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {
        return new RecyclerViewMatcher(recyclerViewId);
    }

}
