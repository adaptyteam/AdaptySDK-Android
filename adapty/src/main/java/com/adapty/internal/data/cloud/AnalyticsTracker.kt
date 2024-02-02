package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.AnalyticsEvent
import com.adapty.utils.ErrorCallback

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface AnalyticsTracker {
    fun trackSystemEvent(
        customData: AnalyticsEvent.CustomData,
        onEventRegistered: suspend (AnalyticsEvent) -> Unit = {},
    )

    fun trackEvent(
        eventName: String,
        subMap: Map<String, Any>? = null,
        onEventRegistered: suspend (AnalyticsEvent) -> Unit = {},
        completion: ErrorCallback? = null,
    )
}