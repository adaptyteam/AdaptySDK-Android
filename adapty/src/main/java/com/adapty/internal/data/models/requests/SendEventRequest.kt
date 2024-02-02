package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.AnalyticsEvent

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SendEventRequest(
    val events: List<AnalyticsEvent>
) {
    internal companion object {
        fun create(events: List<AnalyticsEvent>) = SendEventRequest(events)
    }
}