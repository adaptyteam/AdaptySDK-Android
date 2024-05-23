package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FallbackVariations(
    @SerializedName("placement_id")
    val placementId: String,
    val data: List<PaywallDto>,
)