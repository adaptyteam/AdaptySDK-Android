package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.models.AnalyticsData
import com.adapty.internal.data.models.AnalyticsEvent
import com.adapty.internal.data.models.AnalyticsEvent.Companion.CUSTOM_DATA
import com.adapty.internal.data.models.AnalyticsEvent.Companion.RETAIN_LIMIT
import com.adapty.internal.data.models.AnalyticsEvent.Companion.SYSTEM_LOG
import com.adapty.internal.utils.*
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.ErrorCallback
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AnalyticsEventRecorder(
    private val cacheRepository: CacheRepository,
    private val gson: Gson,
    private val dataLocalSemaphore: Semaphore,
    private val metaInfoRetriever: MetaInfoRetriever,
): AnalyticsTracker {

    private val sessionId = generateUuid()

    override fun trackSystemEvent(
        customData: AnalyticsEvent.CustomData,
        onEventRegistered: suspend (AnalyticsEvent) -> Unit,
    ) {
        trackEvent(SYSTEM_LOG, mapOf(CUSTOM_DATA to customData), onEventRegistered)
    }

    override fun trackEvent(
        eventName: String,
        subMap: Map<String, Any>?,
        onEventRegistered: suspend (AnalyticsEvent) -> Unit,
        completion: ErrorCallback?,
    ) {
        execute {
            flow {
                if (eventName !in cacheRepository.analyticsConfig.disabledEventTypes) {
                    val event = createEvent(eventName, subMap)
                    retainEvent(event)
                    onEventRegistered(event)
                }
                emit(Unit)
            }
                .run {
                    if (completion != null) {
                        this
                            .catch { e ->
                                runOnMain { completion.onResult(e.asAdaptyError()) }
                            }
                            .onEach {
                                runOnMain {
                                    completion.onResult(null) //since the event has been saved and will be sent at least later
                                }
                            }
                    } else {
                        this.catch { }
                    }
                }
                .collect()
        }
    }

    private fun createEvent(eventName: String, subMap: Map<String, Any>?): AnalyticsEvent {
        val createdAt = metaInfoRetriever.formatDateTimeGMT()
        return AnalyticsEvent(
            eventId = generateUuid(),
            eventName = eventName,
            profileId = cacheRepository.getProfileId(),
            sessionId = sessionId,
            deviceId = cacheRepository.getInstallationMetaId(),
            createdAt = createdAt,
            platform = "Android",
            other = subMap.orEmpty().mapValues { (_, v) ->
                if (v is AnalyticsEvent.CustomData) {
                    runCatching { gson.toJson(v) }.fold(
                        onSuccess = { customDataStr ->
                            customDataStr
                        },
                        onFailure = { e ->
                            Logger.log(ERROR) { "Couldn't handle system event. $e" }
                            "{\"event_name\":\"error\",\"message\":\"${e.localizedMessage}\"}"
                        }
                    ).orEmpty()
                } else {
                    v
                }
            }
        )
    }

    private suspend fun retainEvent(event: AnalyticsEvent) {
        val isSystemLog = event.isSystemLog
        dataLocalSemaphore.withPermit {
            val (events, prevOrdinal) = cacheRepository.getAnalyticsData(isSystemLog)
            val ordinal = prevOrdinal + 1
            event.ordinal = ordinal
            cacheRepository.saveAnalyticsData(
                AnalyticsData(
                    events.sortedBy { event -> event.ordinal }
                        .takeLast(RETAIN_LIMIT - 1).toMutableList()
                        .apply { add(event) },
                    ordinal,
                ),
                isSystemLog,
            )
        }
    }
}