package com.ofalvai.bpinfo.ui.alertlist.adapter

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.util.SparseArray
import android.view.ViewGroup
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.ui.alertlist.AlertListContract
import com.ofalvai.bpinfo.ui.alertlist.AlertListFragment
import com.ofalvai.bpinfo.ui.alertlist.AlertListType

class AlertListPagerAdapter(fm: FragmentManager, private val context: Context)
    : FragmentPagerAdapter(fm), ViewPager.OnPageChangeListener {

    companion object {

        const val FRAGMENT_COUNT = 2
    }

    // http://stackoverflow.com/a/15261142/745637
    private val registeredFragments = SparseArray<Fragment>()

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> AlertListFragment.newInstance(AlertListType.ALERTS_TODAY)
            1 -> AlertListFragment.newInstance(AlertListType.ALERTS_FUTURE)
            else -> Fragment()
        }
    }

    // http://stackoverflow.com/a/15261142/745637
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        registeredFragments.put(position, fragment)
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

    override fun getCount() = FRAGMENT_COUNT

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> context.getString(R.string.tab_title_today)
            1 -> context.getString(R.string.tab_title_future)
            else -> "Default tab"
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageSelected(position: Int) {
        val view = registeredFragments.get(position) as? AlertListContract.View
        view?.updateSubtitle()
    }
}
