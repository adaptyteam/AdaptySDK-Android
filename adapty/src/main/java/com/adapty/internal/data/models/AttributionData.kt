package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AttributionData(
    @SerializedName("source")
    val source: String,
    @SerializedName("attribution")
    val attribution: Any,
    @SerializedName("network_user_id")
    val networkUserId: String?
)