package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AttributionData(
    @SerializedName("source")
    internal val source: String,
    @SerializedName("attribution")
    private val attribution: Any,
    @SerializedName("network_user_id")
    private val networkUserId: String?
)