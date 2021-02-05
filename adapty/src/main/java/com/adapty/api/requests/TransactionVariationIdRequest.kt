package com.adapty.api.requests

import com.google.gson.annotations.SerializedName

class TransactionVariationIdRequest(
    @SerializedName("data")
    val data: Data
) {
    class Data(
        val type: String = "adapty_analytics_transaction_variation_id",
        @SerializedName("attributes")
        val attributes: Attributes
    ) {

        class Attributes(
            @SerializedName("transaction_id")
            val transactionId: String,
            @SerializedName("variation_id")
            val variationId: String,
            @SerializedName("profile_id")
            val profileId: String
        )
    }

    companion object {
        fun create(transactionId: String, variationId: String, profileId: String) =
            TransactionVariationIdRequest(
                Data(
                    attributes = Data.Attributes(
                        transactionId, variationId, profileId
                    )
                )
            )
    }
}