package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ValidationResult(
    @SerializedName("profile")
    val profile: ProfileDto,
    @SerializedName("errors")
    val errors: ArrayList<SideError>,
) {
    class SideError(
        @SerializedName("purchase_token")
        val purchaseToken: String?,
        @SerializedName("message")
        val message: String?,
    )
}