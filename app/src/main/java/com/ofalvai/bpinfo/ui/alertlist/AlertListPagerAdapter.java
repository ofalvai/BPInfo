package com.ofalvai.bpinfo.ui.alertlist;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.ofalvai.bpinfo.R;

public class AlertListPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

    private static final int FRAGMENT_COUNT = 2;

    private Context mContext;

    // http://stackoverflow.com/a/15261142/745637
    private SparseArray<Fragment> mRegisteredFragments = new SparseArray<>();

    public AlertListPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = AlertListFragment.newInstance(AlertListType.ALERTS_TODAY);
                mRegisteredFragments.put(position, fragment);
                break;
            case 1:
                fragment = AlertListFragment.newInstance(AlertListType.ALERTS_FUTURE);
                mRegisteredFragments.put(position, fragment);
                break;
            default:
                fragment = new Fragment();
        }
        return fragment;
    }

    // http://stackoverflow.com/a/15261142/745637
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        mRegisteredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public int getCount() {
        return FRAGMENT_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title;
        switch (position) {
            case 0:
                title = mContext.getString(R.string.tab_title_today);
                break;
            case 1:
                title = mContext.getString(R.string.tab_title_future);
                break;
            default:
                title = "Default tab";
        }
        return title;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onPageSelected(int position) {
        AlertListFragment fragment = (AlertListFragment) mRegisteredFragments.get(position);
        fragment.updateSubtitle();

    }
}
