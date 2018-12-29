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

package com.ofalvai.bpinfo.ui.notifications.routelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.notifications.NotificationsActivity
import com.ofalvai.bpinfo.ui.notifications.NotificationsViewModel
import com.ofalvai.bpinfo.ui.notifications.routelist.adapter.RouteAdapter
import com.ofalvai.bpinfo.ui.notifications.routelist.adapter.RouteClickListener
import com.ofalvai.bpinfo.util.EmptyRecyclerView
import com.ofalvai.bpinfo.util.bindView
import com.ofalvai.bpinfo.util.observe
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class RouteListFragment : Fragment(), RouteListContract.View, RouteClickListener {

    private lateinit var presenter: RouteListContract.Presenter

    private val parentViewModel by sharedViewModel<NotificationsViewModel>()

    private val recyclerView: EmptyRecyclerView by bindView(R.id.fragment_route_list__recyclerview)

    private val progressBar: ProgressBar by bindView(R.id.fragment_route_list__progressbar)

    private lateinit var adapter: RouteAdapter

    private lateinit var routeType: RouteType

    companion object {

        const val KEY_ROUTE_TYPE = "ROUTE_TYPE"

        fun newInstance(routeType: RouteType): RouteListFragment {
            val fragment = RouteListFragment()
            fragment.routeType = routeType
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_route_list, container, false)

        savedInstanceState?.let {
            routeType = if (savedInstanceState.getSerializable(KEY_ROUTE_TYPE) != null) {
                savedInstanceState.getSerializable(KEY_ROUTE_TYPE) as RouteType
            } else {
                RouteType._OTHER_
            }
        }

        presenter = RouteListPresenter()
        presenter.attachView(this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        observe(parentViewModel.routeList, this::displayRoutes)
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(KEY_ROUTE_TYPE, routeType)
        super.onSaveInstanceState(outState)
    }

    override fun displayRoutes(routeList: List<Route>) {
        val groupedRoutes: Map<RouteType, List<Route>> = routeList.groupBy { it.type }

        val routeListByType: List<Route>? = groupedRoutes[routeType]?.sortedBy { it.id }

        adapter.routeList = routeListByType ?: emptyList()
        progressBar.visibility = View.GONE
    }

    override fun onRouteClicked(route: Route) {
        (activity as? NotificationsActivity)?.onRouteClicked(route)
    }

    private fun initRecyclerView() {
        adapter = RouteAdapter(requireContext(), this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
    }
}
