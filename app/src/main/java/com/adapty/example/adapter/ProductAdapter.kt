package com.adapty.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adapty.example.R
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProductSubscriptionDetails
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import kotlinx.android.synthetic.main.product_item.view.*

typealias PurchaseClickCallback = (AdaptyPaywallProduct) -> Unit
typealias SubscriptionChangeClickCallback = (AdaptyPaywallProduct, AdaptySubscriptionUpdateParameters.ReplacementMode) -> Unit

class ProductAdapter(
    private val products: List<AdaptyPaywallProduct>,
    private val onPurchaseClick: PurchaseClickCallback,
    private val onSubscriptionChangeClick: SubscriptionChangeClickCallback,
) :
    RecyclerView.Adapter<ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position], onPurchaseClick, onSubscriptionChangeClick)
    }

    override fun getItemCount(): Int = products.size
}

private val replacementModeList = listOf(
    "WITH_TIME_PRORATION",
    "CHARGE_PRORATED_PRICE",
    "WITHOUT_PRORATION",
    "DEFERRED",
    "CHARGE_FULL_PRICE",
)

class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(
        product: AdaptyPaywallProduct,
        onPurchaseClick: PurchaseClickCallback,
        onSubscriptionChangeClick: SubscriptionChangeClickCallback
    ) {
        with(itemView) {
            product_title.text = product.localizedTitle
            product_id.text = product.vendorProductId
            product.subscriptionDetails?.basePlanId?.let { basePlanId ->
                group_sub_base_plan.visibility = View.VISIBLE
                base_plan_id.text = basePlanId
            } ?: kotlin.run { group_sub_base_plan.visibility = View.GONE }
            product.subscriptionDetails?.offerId?.let { offerId ->
                group_sub_offer.visibility = View.VISIBLE
                offer_id.text = offerId
            } ?: kotlin.run { group_sub_offer.visibility = View.GONE }
            original_price.text = product.price.localizedString
            price_currency.text = product.price.currencyCode
            product_type.text = when {
                product.productDetails.productType == ProductType.SUBS && product.subscriptionDetails?.renewalType == AdaptyProductSubscriptionDetails.RenewalType.PREPAID -> "${ProductType.SUBS}:prepaid"
                else -> product.productDetails.productType
            }
            make_purchase.setOnClickListener {
                onPurchaseClick(product)
            }

            if (product.productDetails.productType == BillingClient.ProductType.SUBS) {
                group_change_subscription.visibility = View.VISIBLE
                val replacementModeAdapter =
                    ArrayAdapter(context, android.R.layout.simple_list_item_1, replacementModeList)
                replacement_mode_selector.adapter = replacementModeAdapter
                change_subscription.setOnClickListener {
                    onSubscriptionChangeClick(
                        product,
                        AdaptySubscriptionUpdateParameters.ReplacementMode.valueOf(replacement_mode_selector.selectedItem.toString())
                    )
                }
            } else {
                group_change_subscription.visibility = View.GONE
            }
        }
    }
}