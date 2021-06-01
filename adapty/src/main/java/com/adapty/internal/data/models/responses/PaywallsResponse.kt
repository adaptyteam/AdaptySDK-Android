package com.adapty.internal.data.models.responses

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.PaywallDto
import com.adapty.internal.data.models.ProductDto
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallsResponse(
    @SerializedName("data")
    val data: ArrayList<Data>?,
    @SerializedName("meta")
    val meta: Meta?
) {
    internal class Data(
        @SerializedName("id")
        val id: String?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("attributes")
        val attributes: PaywallDto?
    )

    internal class Meta(
        @SerializedName("products")
        val products: ArrayList<ProductDto>?
    )
}