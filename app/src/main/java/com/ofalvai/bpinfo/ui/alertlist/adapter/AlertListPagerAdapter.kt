/*
 * Copyright 2018 Olivér Falvai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        private const val FRAGMENT_COUNT = 2
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
