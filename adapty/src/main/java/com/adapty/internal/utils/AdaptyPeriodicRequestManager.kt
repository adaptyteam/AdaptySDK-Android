package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.adapty.internal.data.cloud.KinesisManager
import com.adapty.internal.domain.PurchaserInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AdaptyPeriodicRequestManager(
    lifecycleManager: AdaptyLifecycleManager,
    private val purchaserInteractor: PurchaserInteractor,
    private val kinesisManager: KinesisManager,
) : AdaptyLifecycleManager.StateCallback {

    private val TRACKING_INTERVAL = (60 * 1000).toLong()
    private val LIVE_EVENT_NAME = "live"

    private var schedulePurchaserInfoJob: Job? = null
    private var trackLiveJob: Job? = null

    init {
        lifecycleManager.stateCallback = this
    }

    private val areRequestsAllowed = AtomicBoolean(false)

    @JvmSynthetic
    fun startPeriodicRequests() {
        stopAll()
        areRequestsAllowed.set(true)
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            scheduleAll(initialDelayMillis = TRACKING_INTERVAL)
        }
    }

    @JvmSynthetic
    override fun onGoForeground() {
        if (areRequestsAllowed.get())
            scheduleAll(initialDelayMillis = 0)
    }

    @JvmSynthetic
    override fun onGoBackground() {
        stopAll()
    }

    private fun stopAll() {
        if (schedulePurchaserInfoJob?.isActive == true) {
            schedulePurchaserInfoJob?.cancel()
        }
        schedulePurchaserInfoJob = null
        if (trackLiveJob?.isActive == true) {
            trackLiveJob?.cancel()
        }
        trackLiveJob = null
    }

    private fun trackLive() {
        execute {
            createTimer()
                .map {
                    if (it > 0)
                        delay(TRACKING_INTERVAL)
                    kinesisManager.trackEvent(LIVE_EVENT_NAME)
                }
                .catch { }
                .collect()
        }.also { trackLiveJob = it }
    }

    private fun schedulePurchaserInfo(initialDelayMillis: Long = 0L) {
        execute {
            runPeriodically(initialDelayMillis) {
                purchaserInteractor
                    .getPurchaserInfoFromCloud(INFINITE_RETRY)
            }
        }.also { schedulePurchaserInfoJob = it }
    }

    private fun scheduleAll(initialDelayMillis: Long) {
        trackLive()
        schedulePurchaserInfo(initialDelayMillis)
    }

    private suspend fun runPeriodically(
        initialDelayMillis: Long,
        delayMillis: Long = TRACKING_INTERVAL,
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

    private fun createTimer() =
        (0..Int.MAX_VALUE)
            .asSequence()
            .asFlow()
}