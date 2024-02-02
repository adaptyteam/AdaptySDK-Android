package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AnalyticsConfig(
    @SerializedName("blacklist")
    val disabledEventTypes: List<String>,
    @SerializedName("expires_at")
    val expiresAt: Long,
) {
    companion object {
        val DEFAULT = AnalyticsConfig(listOf(), 0)
    }

    operator fun component1() = disabledEventTypes
    operator fun component2() = expiresAt
}