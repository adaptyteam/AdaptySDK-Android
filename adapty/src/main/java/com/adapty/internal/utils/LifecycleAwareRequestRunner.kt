package com.adapty.internal.utils

import android.os.SystemClock
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.domain.ProfileInteractor
import com.adapty.internal.domain.PurchasesInteractor
import com.adapty.internal.domain.UserAcquisitionInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LifecycleAwareRequestRunner(
    lifecycleManager: LifecycleManager,
    private val profileInteractor: ProfileInteractor,
    private val userAcquisitionInteractor: UserAcquisitionInteractor,
    private val purchasesInteractor: PurchasesInteractor,
    private val analyticsTracker: AnalyticsTracker,
    private val cacheRepository: CacheRepository,
) : LifecycleManager.StateCallback {

    private val PERIODIC_REQUEST_INTERVAL = (60 * 1000).toLong()

    private val APP_OPENED_EVENT_MIN_INTERVAL = 60_000L

    private val CROSSPLACEMENT_INFO_REQUEST_MIN_INTERVAL = 60_000L

    private val WEB_PAYWALL_OPENED_MAX_DURATION = 20 * 60 * 1000L
    private val WEB_PAYWALL_FREQUENT_REFRESH_DURATION = 5 * 60 * 1000L
    private val WEB_PAYWALL_FREQUENT_REFRESH_INTERVAL = 3000L

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
            handleSyncUnsyncedValidateData()
            scheduleGetProfileRequest(initialDelayMillis = PERIODIC_REQUEST_INTERVAL)
        }
    }

    @JvmSynthetic
    override fun onGoForeground() {
        if (areRequestsAllowed.get()) {
            handleAppOpenedEvent()
            handleRequestCrossPlacementInfo()
            handleRegisterInstall()
            handleSyncUnsyncedValidateData()
            updateWebPaywallProfileRefreshStartTimeIfNeeded()

            scheduleGetProfileRequest(initialDelayMillis = 0)
        }
    }

    private fun updateWebPaywallProfileRefreshStartTimeIfNeeded() {
        val now = System.currentTimeMillis()
        val openedTime = cacheRepository.getLastWebPaywallOpenedTime()

        if (openedTime > 0 && now - openedTime < WEB_PAYWALL_OPENED_MAX_DURATION) {
            val startTime = cacheRepository.getLastWebPaywallProfileRefreshStartTime()
            if (startTime == 0L || startTime < openedTime) {
                cacheRepository.saveLastWebPaywallProfileRefreshStartTime(now)
            }
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
                    .collect()
            }
        }
    }

    private fun handleSyncUnsyncedValidateData() {
        execute {
            purchasesInteractor
                .syncUnsyncedValidateData()
                .catch { }
                .collect()
        }
    }

    private fun handleRegisterInstall() {
        execute {
            userAcquisitionInteractor
                .registerInstall()
                .catch { }
                .collect()
        }
    }

    private fun scheduleGetProfileRequest(initialDelayMillis: Long) {
        execute {
            runPeriodically(initialDelayMillis, ::getProfileRefreshDelay) {
                profileInteractor
                    .getProfile(INFINITE_RETRY)
            }
        }.also { scheduleGetProfileJob = it }
    }

    private fun getProfileRefreshDelay(): Long {
        val now = System.currentTimeMillis()
        val openedTime = cacheRepository.getLastWebPaywallOpenedTime()

        if (openedTime > 0 && now - openedTime < WEB_PAYWALL_OPENED_MAX_DURATION) {
            val startTime = cacheRepository.getLastWebPaywallProfileRefreshStartTime()
            if (startTime > openedTime && now - startTime < WEB_PAYWALL_FREQUENT_REFRESH_DURATION) {
                return WEB_PAYWALL_FREQUENT_REFRESH_INTERVAL
            }
        }
        return PERIODIC_REQUEST_INTERVAL
    }

    private suspend fun runPeriodically(
        initialDelayMillis: Long,
        delayMillisProvider: () -> Long,
        call: () -> Flow<*>
    ) {
        if (initialDelayMillis > 0)
            delay(initialDelayMillis)
        while (true) {
            call()
                .catch { }
                .collect()
            delay(delayMillisProvider())
        }
    }
}