package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class CrossPlacementInfo(
    @SerializedName("placement_with_variation_map")
    val placementWithVariationMap: Map<String, String>,
    val version: Long,
) {
    companion object {
        fun forNewProfile() = CrossPlacementInfo(mapOf(), 0L)
    }
}
