package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode.AUTHENTICATION_ERROR
import com.adapty.errors.AdaptyErrorCode.BAD_REQUEST
import com.adapty.errors.AdaptyErrorCode.DECODING_FAILED
import com.adapty.errors.AdaptyErrorCode.REQUEST_FAILED
import com.adapty.errors.AdaptyErrorCode.SERVER_ERROR
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AnalyticsEventQueueDispatcher(
    private val cacheRepository: CacheRepository,
    private val httpClient: HttpClient,
    private val requestFactory: RequestFactory,
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
                        .mapLatest { fetchDisabledEventTypes() }
                        .retryIfNecessary(DEFAULT_RETRY_COUNT)
                        .catch { error ->
                            if (error is AdaptyError && (error.isHttpError() || error.isDecodingError())) {
                                val fallbackConfig = AnalyticsConfig.createFallback()
                                cacheRepository.analyticsConfig = fallbackConfig
                                emit(fallbackConfig.disabledEventTypes)
                            } else {
                                throw error
                            }
                        }
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
                .collect()
        }
    }

    private fun fetchDisabledEventTypes() : List<String> {
        val (disabledEventTypes, expiresAt) = cacheRepository.analyticsConfig
        if (System.currentTimeMillis() < expiresAt) {
            return disabledEventTypes
        } else {
            val response = httpClient.newCall<AnalyticsConfig>(
                requestFactory.getAnalyticsConfig(),
                AnalyticsConfig::class.java
            )
            when (response) {
                is Response.Success -> {
                    cacheRepository.analyticsConfig = response.body
                    return response.body.disabledEventTypes
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

    private fun AdaptyError.isHttpError() =
        adaptyErrorCode in arrayOf(SERVER_ERROR, BAD_REQUEST, AUTHENTICATION_ERROR)
                || (adaptyErrorCode == REQUEST_FAILED && originalError == null)

    private fun AdaptyError.isDecodingError() = adaptyErrorCode == DECODING_FAILED
}