package com.adapty.example

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.adapty.Adapty
import com.adapty.example.adapter.ProductAdapter
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.ui.AdaptyUI
import com.adapty.utils.AdaptyResult

class ProductListFragment : Fragment(R.layout.fragment_list) {

    companion object {
        fun newInstance(paywall: AdaptyPaywall, products: List<AdaptyPaywallProduct>) =
            ProductListFragment().apply {
                this.paywall = paywall
                this.products = products
            }
    }

    private var paywall: AdaptyPaywall? = null
    private var products = listOf<AdaptyPaywallProduct>()

    private val progressDialog: ProgressDialog by lazy {
        ProgressDialog(context)
    }

    private val productAdapter: ProductAdapter by lazy {
        ProductAdapter(products = products, onPurchaseClick = { product ->
            activity?.let { activity ->
                progressDialog.show()
                Adapty.makePurchase(
                    activity,
                    product
                ) { result ->
                    progressDialog.cancel()
                    showToast(
                        when (result) {
                            is AdaptyResult.Success -> "Success"
                            is AdaptyResult.Error -> result.error.message.orEmpty()
                        }
                    )
                }
            }
        }, onSubscriptionChangeClick = { product, replacementMode ->
            activity?.let { activity ->
                Adapty.getProfile { result ->
                    when (result) {
                        is AdaptyResult.Success -> {
                            val profile = result.value
                            profile.accessLevels.values.find { it.isActive && !it.isLifetime }
                                ?.let { currentSubscription ->
                                    val vendorProductId: String
                                    val basePlanId: String?
                                    currentSubscription.vendorProductId.split(":").let { parts ->
                                        vendorProductId = parts[0]
                                        basePlanId = parts.getOrNull(1)
                                    }

                                    if (vendorProductId == product.vendorProductId && basePlanId == product.subscriptionDetails?.basePlanId) {
                                        showToast("Can't change to same product")
                                        return@let
                                    }

                                    if (currentSubscription.store != "play_store") {
                                        showToast("Can't change subscription from different store")
                                        return@let
                                    }

                                    progressDialog.show()
                                    Adapty.makePurchase(
                                        activity,
                                        product,
                                        AdaptyPurchaseParameters.Builder()
                                            .withSubscriptionUpdateParams(
                                                AdaptySubscriptionUpdateParameters(
                                                    vendorProductId,
                                                    replacementMode,
                                                )
                                            )
                                            .build(),
                                    ) { result ->
                                        progressDialog.cancel()
                                        showToast(
                                            when (result) {
                                                is AdaptyResult.Success -> "Success"
                                                is AdaptyResult.Error -> result.error.message.orEmpty()
                                            }
                                        )
                                    }
                                } ?: showToast("Nothing to change. First buy any subscription")
                        }

                        is AdaptyResult.Error -> {
                            showToast("error: ${result.error.message}")
                        }
                    }
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.onReceiveSystemBarsInsets { insets ->
            view.setPadding(
                view.paddingStart,
                view.paddingTop + insets.top,
                view.paddingEnd,
                view.paddingBottom + insets.bottom,
            )
        }

        view.findViewById<RecyclerView>(R.id.list).adapter = productAdapter

        val hasViewConfiguration = paywall?.hasViewConfiguration ?: false

        view.findViewById<View>(R.id.presentPaywall).apply {
            val paywall = paywall ?: run {
                isVisible = false
                return@apply
            }
            isVisible = hasViewConfiguration

            if (hasViewConfiguration) {
                setOnClickListener {
                    AdaptyUI.getViewConfiguration(paywall) { configResult ->
                        progressDialog.cancel()
                        when (configResult) {
                            is AdaptyResult.Success -> {
                                presentPaywall(configResult.value, products)
                            }
                            is AdaptyResult.Error -> {
                                showToast("error:\n${configResult.error.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun presentPaywall(
        viewConfiguration: AdaptyUI.LocalizedViewConfiguration,
        products: List<AdaptyPaywallProduct>
    ) {
        val paywallFragment =
            PaywallUiFragment.newInstance(
                viewConfiguration,
                products,
            )

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_up,
                R.anim.slide_down,
                R.anim.slide_up,
                R.anim.slide_down,
            )
            .addToBackStack(null)
            .add(android.R.id.content, paywallFragment)
            .commit()
    }
}