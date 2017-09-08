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
import com.ofalvai.bpinfo.ui.notifications.routelist.adapter.RouteAdapter
import com.ofalvai.bpinfo.util.EmptyRecyclerView
import com.ofalvai.bpinfo.util.bindView

class RouteListFragment : Fragment(), RouteListContract.View {

    lateinit var presenter: RouteListContract.Presenter

    private val recyclerView: EmptyRecyclerView by bindView(R.id.fragment_route_list__recyclerview)

    private val progressBar: ProgressBar by bindView(R.id.fragment_route_list__progressbar)

    private lateinit var adapter: RouteAdapter

    companion object {
        fun newInstance(): RouteListFragment {
            return RouteListFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.fragment_route_list, container, false)

        presenter = RouteListPresenter()
        presenter.attachView(this)

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()

        presenter.fetchRoutes()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun displayRoutes(routes: List<Route>) {
        adapter.routeList = routes
        progressBar.visibility = View.GONE
    }

    private fun initRecyclerView() {
        adapter = RouteAdapter(context)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
        // TODO: recyclerView.setEmptyView()
    }
}