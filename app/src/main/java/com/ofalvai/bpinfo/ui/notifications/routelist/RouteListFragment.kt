package com.ofalvai.bpinfo.ui.notifications.routelist

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.ofalvai.bpinfo.R
import com.ofalvai.bpinfo.model.Route
import com.ofalvai.bpinfo.model.RouteType
import com.ofalvai.bpinfo.ui.notifications.NotificationsContract
import com.ofalvai.bpinfo.ui.notifications.routelist.adapter.RouteAdapter
import com.ofalvai.bpinfo.ui.notifications.routelist.adapter.RouteClickListener
import com.ofalvai.bpinfo.util.EmptyRecyclerView
import kotterknife.bindView

class RouteListFragment : Fragment(), RouteListContract.View, RouteClickListener {

    private lateinit var presenter: RouteListContract.Presenter

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

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(KEY_ROUTE_TYPE, routeType)
        super.onSaveInstanceState(outState)
    }

    override fun displayRoutes(routes: List<Route>) {
        adapter.routeList = routes
        progressBar.visibility = View.GONE
    }

    override fun onRouteClicked(route: Route) {
        (activity as? NotificationsContract.View)?.onRouteClicked(route)
    }

    private fun initRecyclerView() {
        adapter = RouteAdapter(requireContext(), this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
        // TODO: recyclerView.setEmptyView()
    }
}
