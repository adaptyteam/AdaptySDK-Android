package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.RestoreProductInfo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class RestoreReceiptRequest(
    @SerializedName("data")
    private val data: Data
) {
    internal class Data(
        @SerializedName("type")
        val type: String = "google_receipt_validation_result",
        @SerializedName("attributes")
        val attributes: Attributes
    ) {

        internal class Attributes(
            @SerializedName("profile_id")
            private val profileId: String,
            @SerializedName("restore_items")
            private val restoreItems: List<RestoreProductInfo>
        )
    }

    internal companion object {
        fun create(profileId: String, restoreItems: List<RestoreProductInfo>) =
            RestoreReceiptRequest(
                Data(attributes = Data.Attributes(profileId, restoreItems))
            )
    }
}