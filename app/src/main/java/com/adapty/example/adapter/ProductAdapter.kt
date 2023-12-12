package com.adapty.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.RecyclerView
import com.adapty.example.R
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProductSubscriptionDetails
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType

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

    private val productTitle = itemView.findViewById<TextView>(R.id.product_title)
    private val productId = itemView.findViewById<TextView>(R.id.product_id)
    private val groupSubBasePlan = itemView.findViewById<Group>(R.id.group_sub_base_plan)
    private val basePlanIdLabel = itemView.findViewById<TextView>(R.id.base_plan_id)
    private val groupSubOffer = itemView.findViewById<Group>(R.id.group_sub_offer)
    private val offerIdLabel = itemView.findViewById<TextView>(R.id.offer_id)
    private val originalPrice = itemView.findViewById<TextView>(R.id.original_price)
    private val priceCurrency = itemView.findViewById<TextView>(R.id.price_currency)
    private val productType = itemView.findViewById<TextView>(R.id.product_type)
    private val makePurchase = itemView.findViewById<View>(R.id.make_purchase)
    private val groupChangeSubscription = itemView.findViewById<Group>(R.id.group_change_subscription)
    private val replacementModeSelector = itemView.findViewById<Spinner>(R.id.replacement_mode_selector)
    private val changeSubscription = itemView.findViewById<View>(R.id.change_subscription)

    fun bind(
        product: AdaptyPaywallProduct,
        onPurchaseClick: PurchaseClickCallback,
        onSubscriptionChangeClick: SubscriptionChangeClickCallback
    ) {
        with(itemView) {
            productTitle.text = product.localizedTitle
            productId.text = product.vendorProductId
            product.subscriptionDetails?.basePlanId?.let { basePlanId ->
                groupSubBasePlan.visibility = View.VISIBLE
                basePlanIdLabel.text = basePlanId
            } ?: kotlin.run { groupSubBasePlan.visibility = View.GONE }
            product.subscriptionDetails?.offerId?.let { offerId ->
                groupSubOffer.visibility = View.VISIBLE
                offerIdLabel.text = offerId
            } ?: kotlin.run { groupSubOffer.visibility = View.GONE }
            originalPrice.text = product.price.localizedString
            priceCurrency.text = product.price.currencyCode
            productType.text = when {
                product.productDetails.productType == ProductType.SUBS && product.subscriptionDetails?.renewalType == AdaptyProductSubscriptionDetails.RenewalType.PREPAID -> "${ProductType.SUBS}:prepaid"
                else -> product.productDetails.productType
            }
            makePurchase.setOnClickListener {
                onPurchaseClick(product)
            }

            if (product.productDetails.productType == BillingClient.ProductType.SUBS) {
                groupChangeSubscription.visibility = View.VISIBLE
                val replacementModeAdapter =
                    ArrayAdapter(context, android.R.layout.simple_list_item_1, replacementModeList)
                replacementModeSelector.adapter = replacementModeAdapter
                changeSubscription.setOnClickListener {
                    onSubscriptionChangeClick(
                        product,
                        AdaptySubscriptionUpdateParameters.ReplacementMode.valueOf(replacementModeSelector.selectedItem.toString())
                    )
                }
            } else {
                groupChangeSubscription.visibility = View.GONE
            }
        }
    }
}