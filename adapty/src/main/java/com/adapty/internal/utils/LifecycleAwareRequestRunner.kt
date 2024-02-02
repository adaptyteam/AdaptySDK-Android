package com.adapty.internal.utils

import android.os.SystemClock
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.domain.ProfileInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LifecycleAwareRequestRunner(
    lifecycleManager: LifecycleManager,
    private val profileInteractor: ProfileInteractor,
    private val analyticsTracker: AnalyticsTracker,
    private val cacheRepository: CacheRepository,
) : LifecycleManager.StateCallback {

    private val PERIODIC_REQUEST_INTERVAL = (60 * 1000).toLong()

    private val APP_OPENED_EVENT_MIN_INTERVAL = 3_600_000L

    private var scheduleGetProfileJob: Job? = null

    init {
        lifecycleManager.stateCallback = this
    }

    private val areRequestsAllowed = AtomicBoolean(false)

    @JvmSynthetic
    fun restart() {
        cancelScheduledRequests()
        areRequestsAllowed.set(true)
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            handleAppOpenedEvent()
            scheduleGetProfileRequest(initialDelayMillis = PERIODIC_REQUEST_INTERVAL)
        }
    }

    @JvmSynthetic
    override fun onGoForeground() {
        if (areRequestsAllowed.get()) {
            handleAppOpenedEvent()
            scheduleGetProfileRequest(initialDelayMillis = 0)
        }
    }

    @JvmSynthetic
    override fun onGoBackground() {
        cancelScheduledRequests()
    }

    private fun cancelScheduledRequests() {
        if (scheduleGetProfileJob?.isActive == true) {
            scheduleGetProfileJob?.cancel()
        }
        scheduleGetProfileJob = null
    }

    private fun handleAppOpenedEvent() {
        val now = SystemClock.elapsedRealtime()
        if (now - cacheRepository.getLastAppOpenedTime() !in 0L..APP_OPENED_EVENT_MIN_INTERVAL) {
            analyticsTracker.trackEvent("app_opened", completion = { error ->
                if (error == null) {
                    cacheRepository.saveLastAppOpenedTime(now)
                }
            })
        }
    }

    private fun scheduleGetProfileRequest(initialDelayMillis: Long) {
        execute {
            runPeriodically(initialDelayMillis) {
                profileInteractor
                    .getProfile(INFINITE_RETRY)
            }
        }.also { scheduleGetProfileJob = it }
    }

    private suspend fun runPeriodically(
        initialDelayMillis: Long,
        delayMillis: Long = PERIODIC_REQUEST_INTERVAL,
        call: () -> Flow<*>
    ) {
        if (initialDelayMillis > 0)
            delay(initialDelayMillis)
        call()
            .onEach {
                delay(delayMillis)
                runPeriodically(0, delayMillis, call)
            }
            .catch { }
            .collect()
    }
}