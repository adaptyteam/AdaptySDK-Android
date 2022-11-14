package com.adapty.internal.data.models.responses

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.PaywallDto
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallResponse(
    @SerializedName("data")
    val data: Data,
) {
    internal class Data(
        @SerializedName("id")
        val id: String,
        @SerializedName("type")
        val type: String,
        @SerializedName("attributes")
        val attributes: PaywallDto
    )
}