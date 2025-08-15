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

        fun createFallback() = AnalyticsConfig(
            listOf("system_log"),
            System.currentTimeMillis() + FALLBACK_MILLIS,
        )

        private const val FALLBACK_MILLIS = 24 * 60 * 60 * 1000
    }

    operator fun component1() = disabledEventTypes
    operator fun component2() = expiresAt
}