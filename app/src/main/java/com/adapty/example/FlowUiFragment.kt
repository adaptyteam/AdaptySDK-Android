package com.adapty.example

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.ui.AdaptyFlowView
import com.adapty.ui.AdaptyUI
import com.adapty.ui.listeners.AdaptyFlowDefaultEventListener

class FlowUiFragment : Fragment(R.layout.fragment_flow_ui) {

    companion object {
        fun newInstance(
            viewConfig: AdaptyUI.FlowConfiguration,
            products: List<AdaptyPaywallProduct>,
        ) =
            FlowUiFragment().apply {
                this.products = products
                this.viewConfiguration = viewConfig
            }
    }

    private var viewConfiguration: AdaptyUI.FlowConfiguration? = null
    private var products = listOf<AdaptyPaywallProduct>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val flowView = view as? AdaptyFlowView ?: return
        val viewConfig = viewConfiguration ?: return

        val eventListener = object: AdaptyFlowDefaultEventListener() {

            /**
             * You can override more methods if needed
             */

            override fun onRestoreSuccess(
                profile: AdaptyProfile,
                context: Context,
            ) {
                if (profile.accessLevels["premium"]?.isActive == true) {
                    parentFragmentManager.popBackStack()
                }
            }
        }

        val customTags = mapOf("USERNAME" to "Bruce", "CITY" to "Philadelphia")
        flowView.showFlow(
            viewConfig,
            products,
            eventListener,
            tagResolver = { tag -> customTags[tag] }
        )

        /**
         * Also you can get the `AdaptyFlowView` and set flow right away
         * by calling `AdaptyUi.getFlowView()`
         */
    }
}