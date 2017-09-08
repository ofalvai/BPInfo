package com.ofalvai.bpinfo.ui.notifications.routelist

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ofalvai.bpinfo.R

class RouteListFragment : Fragment(), RouteListContract.View {

    lateinit var presenter: RouteListContract.Presenter

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

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }
}