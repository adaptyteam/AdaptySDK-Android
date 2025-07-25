package com.adapty.internal.utils

import android.os.SystemClock
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.domain.ProfileInteractor
import com.adapty.internal.domain.UserAcquisitionInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LifecycleAwareRequestRunner(
    lifecycleManager: LifecycleManager,
    private val profileInteractor: ProfileInteractor,
    private val userAcquisitionInteractor: UserAcquisitionInteractor,
    private val analyticsTracker: AnalyticsTracker,
    private val cacheRepository: CacheRepository,
) : LifecycleManager.StateCallback {

    private val PERIODIC_REQUEST_INTERVAL = (60 * 1000).toLong()

    private val APP_OPENED_EVENT_MIN_INTERVAL = 60_000L

    private val CROSSPLACEMENT_INFO_REQUEST_MIN_INTERVAL = 60_000L

    private var scheduleGetProfileJob: Job? = null

    init {
        lifecycleManager.stateCallback = this
    }

    private val areRequestsAllowed = AtomicBoolean(false)

    private val appOpenedHandlingExecutor = Executors.newSingleThreadExecutor()

    @JvmSynthetic
    fun restart() {
        cancelScheduledRequests()
        areRequestsAllowed.set(true)
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            handleAppOpenedEvent()
            handleRequestCrossPlacementInfo()
            handleRegisterInstall()
            scheduleGetProfileRequest(initialDelayMillis = PERIODIC_REQUEST_INTERVAL)
        }
    }

    @JvmSynthetic
    override fun onGoForeground() {
        if (areRequestsAllowed.get()) {
            handleAppOpenedEvent()
            handleRequestCrossPlacementInfo()
            handleRegisterInstall()
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
                    appOpenedHandlingExecutor.execute {
                        cacheRepository.saveLastAppOpenedTime(now)
                    }
                }
            })
        }
    }

    private fun handleRequestCrossPlacementInfo() {
        val now = SystemClock.elapsedRealtime()
        if (now - cacheRepository.getLastRequestedCrossPlacementInfoTime() !in 0L..CROSSPLACEMENT_INFO_REQUEST_MIN_INTERVAL) {
            execute {
                cacheRepository.saveLastRequestedCrossPlacementInfoTime(now)
                profileInteractor
                    .syncCrossPlacementInfo()
                    .catch {
                        cacheRepository.clearLastRequestedCrossPlacementInfoTime()
                    }
            }
        }
    }

    private fun handleRegisterInstall() {
        execute {
            userAcquisitionInteractor
                .registerInstall()
                .catch { }
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