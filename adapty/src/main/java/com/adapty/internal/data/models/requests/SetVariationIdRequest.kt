package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SetVariationIdRequest(
    @SerializedName("data")
    val data: Data
) {
    internal class Data(
        @SerializedName("type")
        val type: String = "adapty_analytics_transaction_variation_id",
        @SerializedName("attributes")
        val attributes: Attributes
    ) {

        internal class Attributes(
            @SerializedName("transaction_id")
            val transactionId: String,
            @SerializedName("variation_id")
            val variationId: String,
            @SerializedName("profile_id")
            val profileId: String
        )
    }

    internal companion object {
        fun create(transactionId: String, variationId: String, profileId: String) =
            SetVariationIdRequest(
                Data(
                    attributes = Data.Attributes(
                        transactionId, variationId, profileId
                    )
                )
            )
    }
}