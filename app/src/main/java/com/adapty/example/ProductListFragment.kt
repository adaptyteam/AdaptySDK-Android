package com.adapty.example

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adapty.Adapty
import com.adapty.example.adapter.ProductAdapter
import com.adapty.models.ProductModel
import com.adapty.models.SubscriptionUpdateParamModel
import kotlinx.android.synthetic.main.fragment_list.*

class ProductListFragment : Fragment(R.layout.fragment_list) {

    companion object {
        fun newInstance(products: List<ProductModel>) = ProductListFragment().apply {
            productList = products
        }
    }

    private var productList = listOf<ProductModel>()

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
                ) { purchaserInfo, purchaseToken, googleValidationResult, product, error ->
                    progressDialog.dismiss()
                    showToast(error?.message ?: "Success")
                }
            }
        }, onSubscriptionChangeClick = { product, prorationMode ->
            activity?.let { activity ->
                Adapty.getPurchaserInfo { purchaserInfo, error ->
                    purchaserInfo?.accessLevels?.values?.find { it.isActive && !it.isLifetime }
                        ?.let { currentSubscription ->
                            if (currentSubscription.vendorProductId == product.vendorProductId) {
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
                                SubscriptionUpdateParamModel(
                                    currentSubscription.vendorProductId,
                                    prorationMode
                                )
                            ) { purchaserInfo, purchaseToken, googleValidationResult, product, error ->
                                progressDialog.dismiss()
                                showToast(error?.message ?: "Success")
                            }
                        } ?: showToast("Nothing to change. First buy any subscription")
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.adapter = productAdapter
    }
}