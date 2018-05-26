package com.ofalvai.bpinfo.ui.notifications.adapter

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.util.SparseArray
import android.view.ViewGroup
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.notifications.routelist.RouteListContract
import com.ofalvai.bpinfo.ui.notifications.routelist.RouteListFragment

class RouteListPagerAdapter(fm: FragmentManager,
                            private val context: Context
) : FragmentPagerAdapter(fm) {

    private val registeredFragments = SparseArray<Fragment>()

    companion object {
        private const val FRAGMENT_COUNT = 7

        /**
         * In this case, it's better to load these few tabs at once, rather than creating and
         * destroying them during swiping
         */
        const val OFFSCREEN_PAGE_LIMIT = 99
    }

    fun getView(routeType: RouteType): RouteListContract.View? {
        return when(routeType) {
            RouteType.BUS -> registeredFragments[0]
            RouteType.SUBWAY -> registeredFragments[1]
            RouteType.TRAM -> registeredFragments[2]
            RouteType.TROLLEYBUS -> registeredFragments[3]
            RouteType.RAIL -> registeredFragments[4]
            RouteType.FERRY -> registeredFragments[5]
            RouteType._OTHER_ -> registeredFragments[6]
            else -> null
        } as? RouteListContract.View
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

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        registeredFragments.put(position, fragment)
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }
}
