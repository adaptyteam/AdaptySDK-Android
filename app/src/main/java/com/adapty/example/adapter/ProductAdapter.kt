package com.adapty.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adapty.example.R
import com.adapty.models.ProductModel
import com.adapty.models.SubscriptionUpdateParamModel
import com.android.billingclient.api.BillingClient
import kotlinx.android.synthetic.main.product_item.view.*

typealias PurchaseClickCallback = (ProductModel) -> Unit
typealias SubscriptionChangeClickCallback = (ProductModel, SubscriptionUpdateParamModel.ProrationMode) -> Unit

class ProductAdapter(
    private val products: List<ProductModel>,
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
        product: ProductModel,
        onPurchaseClick: PurchaseClickCallback,
        onSubscriptionChangeClick: SubscriptionChangeClickCallback
    ) {
        with(itemView) {
            product_title.text = product.localizedTitle
            product_id.text = product.vendorProductId
            original_price.text = product.localizedPrice
            price_currency.text = product.currencyCode
            product_type.text = product.skuDetails?.type
            make_purchase.setOnClickListener {
                onPurchaseClick(product)
            }

            if (product.skuDetails?.type == BillingClient.SkuType.SUBS) {
                group_change_subscription.visibility = View.VISIBLE
                val prorationModeAdapter =
                    ArrayAdapter(context, android.R.layout.simple_list_item_1, prorationModeList)
                proration_mode_selector.adapter = prorationModeAdapter
                change_subscription.setOnClickListener {
                    onSubscriptionChangeClick(
                        product,
                        SubscriptionUpdateParamModel.ProrationMode.valueOf(proration_mode_selector.selectedItem.toString())
                    )
                }
            } else {
                group_change_subscription.visibility = View.GONE
            }



            if (product.skuDetails?.type == null) {
                /**
                 * You might not have changed your applicationId.
                 *
                 * In order to receive full info about the products and make purchases,
                 * please change sample's applicationId in app/build.gradle to yours
                 */
                make_purchase.isEnabled = false
                change_subscription.isEnabled = false
            }
        }
    }
}