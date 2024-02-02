package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.AnalyticsEvent
import com.adapty.internal.utils.*
import com.adapty.utils.ErrorCallback
import kotlinx.coroutines.flow.*
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AnalyticsManager(
    private val eventRecorder: AnalyticsTracker,
    private val eventQueueDispatcher: AnalyticsEventQueueDispatcher,
): AnalyticsTracker {

     override fun trackSystemEvent(
         customData: AnalyticsEvent.CustomData,
         onEventRegistered: suspend (AnalyticsEvent) -> Unit,
     ) {
        eventRecorder.trackSystemEvent(customData) { event ->
            eventQueueDispatcher.addToQueue(event)
        }
    }

    override fun trackEvent(
        eventName: String,
        subMap: Map<String, Any>?,
        onEventRegistered: suspend (AnalyticsEvent) -> Unit,
        completion: ErrorCallback?,
    ) {
        eventRecorder.trackEvent(
            eventName,
            subMap,
            { event -> eventQueueDispatcher.addToQueue(event) },
            completion,
        )
    }
}