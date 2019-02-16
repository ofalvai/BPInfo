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

package com.ofalvai.bpinfo.ui.notifications.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.notifications.routelist.RouteListFragment

class RouteListPagerAdapter(fm: FragmentManager,
                            private val context: Context
) : FragmentPagerAdapter(fm) {

    companion object {
        private const val FRAGMENT_COUNT = 7

        /**
         * In this case, it's better to load these few tabs at once, rather than creating and
         * destroying them during swiping
         */
        const val OFFSCREEN_PAGE_LIMIT = 99
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
                    6 -> RouteType.OTHER
                    else -> RouteType.OTHER
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
}
