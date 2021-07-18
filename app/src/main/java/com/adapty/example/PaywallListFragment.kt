package com.adapty.example

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adapty.Adapty
import com.adapty.errors.AdaptyError
import com.adapty.example.adapter.PaywallAdapter
import com.adapty.listeners.VisualPaywallListener
import com.adapty.models.GoogleValidationResult
import com.adapty.models.PaywallModel
import com.adapty.models.ProductModel
import com.adapty.models.PurchaserInfoModel
import com.adapty.visual.VisualPaywallActivity
import kotlinx.android.synthetic.main.fragment_list.*

class PaywallListFragment : Fragment(R.layout.fragment_list) {

    companion object {
        fun newInstance(paywalls: List<PaywallModel>) = PaywallListFragment().apply {
            paywallList = paywalls
        }
    }

    private var paywallList = listOf<PaywallModel>()

    private val paywallAdapter: PaywallAdapter by lazy {
        PaywallAdapter(
            paywalls = paywallList,
            onPaywallClick = ::onPaywallClick,
            onVisualPaywallClick = ::onShowVisualPaywallClick
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.adapter = paywallAdapter

        Adapty.setVisualPaywallListener(object : VisualPaywallListener {
            override fun onPurchased(
                purchaserInfo: PurchaserInfoModel?,
                purchaseToken: String?,
                googleValidationResult: GoogleValidationResult?,
                product: ProductModel,
                modalActivity: VisualPaywallActivity?
            ) {
                showToast("Purchased: ${product.vendorProductId}")
            }

            override fun onPurchaseFailure(
                product: ProductModel,
                error: AdaptyError,
                modalActivity: VisualPaywallActivity?
            ) {
                showToast("Purchase failed: ${error.message}")
            }

            override fun onRestorePurchases(
                purchaserInfo: PurchaserInfoModel?,
                googleValidationResultList: List<GoogleValidationResult>?,
                error: AdaptyError?,
                modalActivity: VisualPaywallActivity?
            ) {
                showToast("Restore: ${error?.message ?: "Success"}")
            }

            override fun onCancel(modalActivity: VisualPaywallActivity?) {
                showToast("Attempt to close visual paywall")
                modalActivity?.close()
            }
        })
    }

    private fun onPaywallClick(paywall: PaywallModel) {
        Adapty.logShowPaywall(paywall)

        (activity as? MainActivity)?.addFragment(
            ProductListFragment.newInstance(paywall.products),
            true
        )
    }

    private fun onShowVisualPaywallClick(paywall: PaywallModel) {
        activity?.let {
            Adapty.showVisualPaywall(it, paywall)
        }
    }
}