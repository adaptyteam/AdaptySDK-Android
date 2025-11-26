package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.models.AnalyticsData
import com.adapty.internal.data.models.AnalyticsEvent
import com.adapty.internal.data.models.AnalyticsEvent.Companion.RETAIN_LIMIT
import com.adapty.internal.data.models.AnalyticsEvent.Companion.SEND_LIMIT
import com.adapty.internal.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AnalyticsEventQueueDispatcher(
    private val cacheRepository: CacheRepository,
    private val httpClient: HttpClient,
    private val mainRequestFactory: MainRequestFactory,
    private val netConfigManager: NetConfigManager,
    private val lifecycleManager: LifecycleManager,
    private val dataLocalSemaphore: Semaphore,
    private val dataRemoteSemaphore: Semaphore,
) {

    private val eventFlow = MutableSharedFlow<AnalyticsEvent>()

    init {
        startProcessingEvents()
    }

    fun addToQueue(event: AnalyticsEvent) {
        execute {
            eventFlow.emit(event)
        }
    }

    private fun startProcessingEvents() {
        execute {
            eventFlow
                .flatMapConcat { event ->
                    dataRemoteSemaphore.acquire()
                    lifecycleManager.onActivateAllowed()
                        .mapLatest { fetchEventsExcludedFromSending() }
                        .flatMapConcat { excludedEvents ->
                            val (filteredEvents, processedEvents) =
                                prepareData(excludedEvents, event.isSystemLog)
                            sendData(filteredEvents)
                                .retryIfNecessary(DEFAULT_RETRY_COUNT)
                                .map {
                                    removeProcessedEventsOnSuccess(processedEvents, event.isSystemLog)
                                }
                        }
                        .onEach { dataRemoteSemaphore.release() }
                        .catch { dataRemoteSemaphore.release() }
                }
                .collect()
        }
    }

    private fun fetchEventsExcludedFromSending() : List<String> {
        return netConfigManager.getConfig().eventsExcludedFromSending
    }

    private suspend fun prepareData(
        excludedEvents: List<String>,
        isSystemLog: Boolean,
    ): Pair<List<AnalyticsEvent>, List<AnalyticsEvent>> {
        val events = dataLocalSemaphore.withPermit {
            cacheRepository.getAnalyticsData(isSystemLog).events.sortedByDescending { event -> event.ordinal }
        }

        var processedCount = 0
        val filteredEvents = mutableListOf<AnalyticsEvent>()
        for (event in events) {
            if (filteredEvents.size >= SEND_LIMIT)
                break
            if (event.eventName !in excludedEvents)
                filteredEvents.add(event)
            processedCount++
        }

        val processedEvents = events.take(processedCount)
        return filteredEvents to processedEvents
    }

    private suspend fun sendData(filteredEvents: List<AnalyticsEvent>) = flow {
        if (filteredEvents.isEmpty()) {
            emit(Unit)
            return@flow
        }

        httpClient.newCall<Unit>(
            mainRequestFactory.sendAnalyticsEventsRequest(filteredEvents),
            Unit::class.java
        )
        emit(Unit)
    }

    private suspend fun removeProcessedEventsOnSuccess(
        processedEvents: List<AnalyticsEvent>,
        isSystemLog: Boolean,
    ) {
        dataLocalSemaphore.withPermit {
            cacheRepository.getAnalyticsData(isSystemLog).let { data ->
                cacheRepository.saveAnalyticsData(
                    AnalyticsData(
                        data.events.subtract(processedEvents)
                            .sortedBy { event -> event.ordinal }
                            .takeLast(RETAIN_LIMIT).toMutableList(),
                        data.prevOrdinal,
                    ),
                    isSystemLog,
                )
            }
        }
    }
}