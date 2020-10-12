package com.adapty.example

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adapty.api.entity.containers.Paywall
import com.adapty.example.adapter.PaywallAdapter
import kotlinx.android.synthetic.main.fragment_list.*

class PaywallListFragment : Fragment(R.layout.fragment_list) {

    companion object {
        fun newInstance(paywalls: List<Paywall>) = PaywallListFragment().apply {
            paywallList = paywalls
        }
    }

    private var paywallList = listOf<Paywall>()

    private val paywallAdapter: PaywallAdapter by lazy {
        PaywallAdapter(paywalls = paywallList, onPaywallClick = { paywall ->
            paywall.products?.let { products ->
                (activity as? MainActivity)?.addFragment(
                    ProductListFragment.newInstance(products),
                    true
                )
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.adapter = paywallAdapter
    }
}