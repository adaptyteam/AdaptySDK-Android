package com.adapty.example

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.AdaptyUI
import com.adapty.ui.listeners.AdaptyUiDefaultEventListener

class PaywallUiFragment : Fragment(R.layout.fragment_paywall_ui) {

    companion object {
        fun newInstance(
            viewConfig: AdaptyUI.LocalizedViewConfiguration,
            products: List<AdaptyPaywallProduct>,
        ) =
            PaywallUiFragment().apply {
                this.products = products
                this.viewConfiguration = viewConfig
            }
    }

    private var viewConfiguration: AdaptyUI.LocalizedViewConfiguration? = null
    private var products = listOf<AdaptyPaywallProduct>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val paywallView = view as? AdaptyPaywallView ?: return
        val viewConfig = viewConfiguration ?: return

        val eventListener = object: AdaptyUiDefaultEventListener() {

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
        paywallView.showPaywall(
            viewConfig,
            products,
            eventListener,
            tagResolver = { tag -> customTags[tag] }
        )

        /**
         * Also you can get the `AdaptyPaywallView` and set paywall right away
         * by calling `AdaptyUi.getPaywallView()`
         */
    }
}