@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.onboardings.internal.ui

import androidx.lifecycle.ViewModel
import com.adapty.Adapty
import com.adapty.ui.onboardings.AdaptyOnboardingConfiguration
import com.adapty.ui.onboardings.actions.AdaptyOnboardingAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingLoadedAction
import com.adapty.ui.onboardings.errors.AdaptyOnboardingError
import com.adapty.ui.onboardings.events.AdaptyOnboardingAnalyticsEvent
import com.adapty.ui.onboardings.internal.serialization.OnboardingCommonDeserializer
import com.adapty.ui.onboardings.internal.util.OneOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

internal class OnboardingViewModel(
    private val deserializer: OnboardingCommonDeserializer
) : ViewModel() {

    private val sessionCounter = AtomicLong(0)
    private var currentSessionId: Long = sessionCounter.incrementAndGet()

    private val _actions = MutableSharedFlow<SessionedEmission<AdaptyOnboardingAction>>(extraBufferCapacity = 1)
    val actions: Flow<AdaptyOnboardingAction> = _actions.asSharedFlow()
        .filter { it.sessionId == currentSessionId }
        .map { it.value }

    private val _analytics = MutableSharedFlow<SessionedEmission<AdaptyOnboardingAnalyticsEvent>>(extraBufferCapacity = 1)
    val analytics: Flow<AdaptyOnboardingAnalyticsEvent> = _analytics.asSharedFlow()
        .filter { it.sessionId == currentSessionId }
        .map { it.value }

    private val _errors = MutableSharedFlow<SessionedEmission<AdaptyOnboardingError>>(extraBufferCapacity = 1)
    val errors: Flow<AdaptyOnboardingError> = _errors.asSharedFlow()
        .filter { it.sessionId == currentSessionId }
        .map { it.value }

    private val _onboardingLoaded = MutableSharedFlow<SessionedEmission<AdaptyOnboardingLoadedAction>>(extraBufferCapacity = 1)
    val onboardingLoaded: Flow<AdaptyOnboardingLoadedAction> = _onboardingLoaded.asSharedFlow()
        .filter { it.sessionId == currentSessionId }
        .map { it.value }

    var onboardingConfig: AdaptyOnboardingConfiguration? = null
    var hasFinishedLoading: Boolean = false
    var safeAreaPaddings: Boolean = true

    fun processMessage(message: String) {
        deserializer.deserialize(message)
            .fold(
                { result ->
                    when (result) {
                        is OneOf.First -> when (result.value) {
                            is AdaptyOnboardingLoadedAction -> _onboardingLoaded.tryEmit(
                                SessionedEmission(result.value, currentSessionId)
                            )
                            else -> _actions.tryEmit(
                                SessionedEmission(result.value, currentSessionId)
                            )
                        }
                        is OneOf.Second -> handleAnalyticsEvent(result.value)
                    }
                },
            ) { e ->
                emitError(AdaptyOnboardingError.Unknown(e))
            }
    }

    private fun handleAnalyticsEvent(event: AdaptyOnboardingAnalyticsEvent) {
        _analytics.tryEmit(SessionedEmission(event, currentSessionId))

        if (event is AdaptyOnboardingAnalyticsEvent.ScreenPresented) {
            onboardingConfig?.let { config ->
                Adapty.logShowOnboardingInternal(
                    onboarding = config.onboarding,
                    screenName = event.meta.screenClientId,
                    screenOrder = event.meta.screenIndex,
                    isLastScreen = event.meta.isLastScreen,
                )
            }
        }
    }

    fun emitError(error: AdaptyOnboardingError) {
        _errors.tryEmit(SessionedEmission(error, currentSessionId))
    }

    fun clearState() {
        hasFinishedLoading = false
        onboardingConfig = null
        safeAreaPaddings = true
        currentSessionId = sessionCounter.incrementAndGet()
    }
}

private data class SessionedEmission<T>(
    val value: T,
    val sessionId: Long
)
