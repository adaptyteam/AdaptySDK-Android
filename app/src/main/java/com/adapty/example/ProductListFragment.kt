package com.adapty.example

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adapty.Adapty
import com.adapty.example.adapter.ProductAdapter
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.utils.AdaptyResult
import kotlinx.android.synthetic.main.fragment_list.*

class ProductListFragment : Fragment(R.layout.fragment_list) {

    companion object {
        fun newInstance(products: List<AdaptyPaywallProduct>) = ProductListFragment().apply {
            productList = products
        }
    }

    private var productList = listOf<AdaptyPaywallProduct>()

    private val progressDialog: ProgressDialog by lazy {
        ProgressDialog(context)
    }

    private val productAdapter: ProductAdapter by lazy {
        ProductAdapter(products = productList, onPurchaseClick = { product ->
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
                                        AdaptySubscriptionUpdateParameters(
                                            vendorProductId,
                                            replacementMode
                                        )
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

        list.adapter = productAdapter
    }
}