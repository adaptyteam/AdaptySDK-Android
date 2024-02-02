package com.adapty.internal.data.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AnalyticsData(
    val events: List<AnalyticsEvent>,
    val prevOrdinal: Long,
) {
    companion object {
        val DEFAULT = AnalyticsData(listOf(), 0)
    }

    operator fun component1() = events
    operator fun component2() = prevOrdinal
}