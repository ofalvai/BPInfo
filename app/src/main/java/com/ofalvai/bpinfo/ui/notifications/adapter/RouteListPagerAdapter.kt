package com.ofalvai.bpinfo.ui.notifications.adapter

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.util.SparseArray
import android.view.ViewGroup
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.notifications.routelist.RouteListContract
import com.ofalvai.bpinfo.ui.notifications.routelist.RouteListFragment

class RouteListPagerAdapter(fm: FragmentManager,
                            private val context: Context
) : FragmentStatePagerAdapter(fm) {

    // TODO: exctract position-fragment-title association

    private val registeredFragments = SparseArray<Fragment>()

    companion object {
        const val FRAGMENT_COUNT = 7
    }

    fun getView(routeType: RouteType): RouteListContract.View? {
        return when(routeType) {
            RouteType.BUS -> registeredFragments[0] as RouteListContract.View
            RouteType.SUBWAY -> registeredFragments[1] as RouteListContract.View
            RouteType.TRAM -> registeredFragments[2] as RouteListContract.View
            RouteType.TROLLEYBUS -> registeredFragments[3] as RouteListContract.View
            RouteType.RAIL -> registeredFragments[4] as RouteListContract.View
            RouteType.FERRY -> registeredFragments[5] as RouteListContract.View
            RouteType._OTHER_ -> registeredFragments[6] as RouteListContract.View
            else -> null
        }
    }

    override fun getItem(position: Int): Fragment {
        return RouteListFragment.newInstance(
                when (position) {
                    0 -> RouteType.BUS
                    1 -> RouteType.SUBWAY
                    2 -> RouteType.TRAM
                    3 -> RouteType.TROLLEYBUS
                    4 -> RouteType.RAIL
                    5 -> RouteType.FERRY
                    6 -> RouteType._OTHER_
                    else -> RouteType._OTHER_
                }
        )
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

    override fun instantiateItem(container: ViewGroup?, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        registeredFragments.put(position, fragment)
        return fragment
    }

    override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }
}