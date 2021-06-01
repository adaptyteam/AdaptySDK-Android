package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.ValidateProductInfo
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ValidateReceiptRequest(
    @SerializedName("data")
    private val data: Data
) {
    internal class Data(
        @SerializedName("id")
        val id: String,
        @SerializedName("type")
        val type: String = "google_receipt_validation_result",
        @SerializedName("attributes")
        val attributes: Attributes
    ) {

        internal class Attributes(
            @SerializedName("profile_id")
            private val profileId: String,
            @SerializedName("product_id")
            private val productId: String,
            @SerializedName("purchase_token")
            private val purchaseToken: String,
            @SerializedName("is_subscription")
            private val isSubscription: Boolean,
            @SerializedName("transaction_id")
            private val transactionId: String?,
            @SerializedName("variation_id")
            private val variationId: String?,
            @SerializedName("price_locale")
            private val priceLocale: String?,
            @SerializedName("original_price")
            private val originalPrice: String?
        )
    }

    internal companion object {
        fun create(
            id: String,
            purchase: Purchase,
            product: ValidateProductInfo?,
            purchaseType: String
        ) =
            ValidateReceiptRequest(
                Data(
                    id = id,
                    attributes = Data.Attributes(
                        profileId = id,
                        productId = purchase.skus.firstOrNull().orEmpty(),
                        purchaseToken = purchase.purchaseToken,
                        isSubscription = purchaseType == BillingClient.SkuType.SUBS,
                        transactionId = purchase.orderId,
                        variationId = product?.variationId,
                        priceLocale = product?.priceLocale,
                        originalPrice = product?.originalPrice,
                    )
                )
            )
    }
}