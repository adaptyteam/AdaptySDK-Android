package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AttributionData(
    @SerializedName("source")
    val source: String,
    @SerializedName("attribution_json")
    val attribution: String,
    @SerializedName("profile_id")
    val profileId: String?
)