package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.models.AnalyticsConfig
import com.adapty.internal.data.models.AnalyticsData
import com.adapty.internal.data.models.AnalyticsEvent
import com.adapty.internal.data.models.AnalyticsEvent.Companion.RETAIN_LIMIT
import com.adapty.internal.data.models.AnalyticsEvent.Companion.SEND_LIMIT
import com.adapty.internal.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AnalyticsEventQueueDispatcher(
    private val cacheRepository: CacheRepository,
    private val httpClient: HttpClient,
    private val requestFactory: RequestFactory,
    private val dataLocalSemaphore: Semaphore,
    private val dataRemoteSemaphore: Semaphore,
) {

    private val eventFlow = MutableSharedFlow<AnalyticsEvent>()

    init {
        startProcessingEvents()
    }

    suspend fun addToQueue(event: AnalyticsEvent) {
        eventFlow.emit(event)
    }

    private fun startProcessingEvents() {
        execute {
            eventFlow
                .flatMapConcat { event ->
                    dataRemoteSemaphore.acquire()
                    fetchDisabledEventTypes()
                        .retryIfNecessary(DEFAULT_RETRY_COUNT)
                        .flatMapConcat { disabledEventTypes ->
                            val (filteredEvents, processedEvents) =
                                prepareData(disabledEventTypes, event.isSystemLog)
                            sendData(filteredEvents)
                                .retryIfNecessary(DEFAULT_RETRY_COUNT)
                                .map {
                                    removeProcessedEventsOnSuccess(processedEvents, event.isSystemLog)
                                }
                        }
                        .onEach { dataRemoteSemaphore.release() }
                        .catch { dataRemoteSemaphore.release() }
                }
                .flowOnIO()
                .collect()
        }
    }

    private suspend fun fetchDisabledEventTypes() = flow {
        val (disabledEventTypes, expiresAt) = cacheRepository.analyticsConfig
        if (System.currentTimeMillis() < expiresAt) {
            emit(disabledEventTypes)
        } else {
            val response = httpClient.newCall<AnalyticsConfig>(
                requestFactory.getAnalyticsConfig(),
                AnalyticsConfig::class.java
            )
            when (response) {
                is Response.Success -> {
                    cacheRepository.analyticsConfig = response.body
                    emit(response.body.disabledEventTypes)
                }
                is Response.Error -> {
                    throw response.error
                }
            }
        }
    }

    private suspend fun prepareData(
        disabledEventTypes: List<String>,
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
            if (event.eventName !in disabledEventTypes)
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

        val response = httpClient.newCall<Unit>(
            requestFactory.sendAnalyticsEventsRequest(filteredEvents),
            Unit::class.java
        )
        when (response) {
            is Response.Success -> {
                emit(Unit)
            }
            is Response.Error -> {
                throw response.error
            }
        }
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