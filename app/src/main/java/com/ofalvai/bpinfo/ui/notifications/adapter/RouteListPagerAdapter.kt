package com.ofalvai.bpinfo.ui.notifications.adapter

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.ui.notifications.routelist.RouteListFragment

class RouteListPagerAdapter(fm: FragmentManager,
                            private val context: Context
) : FragmentStatePagerAdapter(fm) {

    companion object {
        const val FRAGMENT_COUNT = 7
    }

    override fun getItem(position: Int): Fragment {
        return RouteListFragment.newInstance()
    }

    override fun getCount(): Int {
        return FRAGMENT_COUNT
    }

    override fun getPageTitle(position: Int): CharSequence {
        return context.getString(when(position) {
            0 -> R.string.route_bus
            1 -> R.string.route_subway
            2 -> R.string.route_tram
            3 -> R.string.route_trolleybus
            4 -> R.string.route_rail
            5 -> R.string.route_ferry
            6 -> R.string.route_other
            else -> R.string.route_other
        })
    }
}