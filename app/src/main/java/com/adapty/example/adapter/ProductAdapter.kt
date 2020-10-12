package com.adapty.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adapty.api.entity.containers.Product
import com.adapty.example.R
import kotlinx.android.synthetic.main.product_item.view.*

typealias PurchaseClickCallback = (Product) -> Unit

class ProductAdapter(
    private val products: List<Product>,
    private val onPurchaseClick: PurchaseClickCallback
) :
    RecyclerView.Adapter<ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position], onPurchaseClick)
    }

    override fun getItemCount(): Int = products.size
}

class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(product: Product, onPurchaseClick: PurchaseClickCallback) {
        with(itemView) {
            product_title.text = product.localizedTitle
            product_id.text = product.vendorProductId
            original_price.text = product.price
            price_currency.text = product.currencyCode
            product_type.text = product.skuDetails?.type
            make_purchase.setOnClickListener {
                onPurchaseClick(product)
            }



            if (product.skuDetails?.type == null) {
                /**
                 * You might not have changed your applicationId.
                 *
                 * In order to receive full info about the products and make purchases,
                 * please change sample's applicationId in app/build.gradle to yours
                 */
                make_purchase.isEnabled = false
            }
        }
    }
}