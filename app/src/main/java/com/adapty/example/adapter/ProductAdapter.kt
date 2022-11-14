package com.adapty.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adapty.example.R
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.android.billingclient.api.BillingClient
import kotlinx.android.synthetic.main.product_item.view.*

typealias PurchaseClickCallback = (AdaptyPaywallProduct) -> Unit
typealias SubscriptionChangeClickCallback = (AdaptyPaywallProduct, AdaptySubscriptionUpdateParameters.ProrationMode) -> Unit

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

private val prorationModeList = listOf(
    "IMMEDIATE_WITH_TIME_PRORATION",
    "IMMEDIATE_AND_CHARGE_PRORATED_PRICE",
    "IMMEDIATE_WITHOUT_PRORATION",
    "DEFERRED",
    "IMMEDIATE_AND_CHARGE_FULL_PRICE",
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
            original_price.text = product.localizedPrice
            price_currency.text = product.currencyCode
            product_type.text = product.skuDetails.type
            make_purchase.setOnClickListener {
                onPurchaseClick(product)
            }

            if (product.skuDetails.type == BillingClient.SkuType.SUBS) {
                group_change_subscription.visibility = View.VISIBLE
                val prorationModeAdapter =
                    ArrayAdapter(context, android.R.layout.simple_list_item_1, prorationModeList)
                proration_mode_selector.adapter = prorationModeAdapter
                change_subscription.setOnClickListener {
                    onSubscriptionChangeClick(
                        product,
                        AdaptySubscriptionUpdateParameters.ProrationMode.valueOf(proration_mode_selector.selectedItem.toString())
                    )
                }
            } else {
                group_change_subscription.visibility = View.GONE
            }
        }
    }
}