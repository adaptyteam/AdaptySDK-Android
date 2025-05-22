@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.onboardings.internal.ui

import androidx.lifecycle.ViewModel
import com.adapty.Adapty
import com.adapty.ui.onboardings.AdaptyOnboardingConfiguration
import com.adapty.ui.onboardings.actions.AdaptyOnboardingAction
import com.adapty.ui.onboardings.errors.AdaptyOnboardingError
import com.adapty.ui.onboardings.events.AdaptyOnboardingAnalyticsEvent
import com.adapty.ui.onboardings.internal.util.OnboardingLoadedEvent
import com.adapty.ui.onboardings.internal.util.OneOf3
import com.adapty.ui.onboardings.internal.serialization.OnboardingCommonDeserializer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class OnboardingViewModel(
    private val deserializer: OnboardingCommonDeserializer
) : ViewModel() {

    private val _actions = MutableSharedFlow<AdaptyOnboardingAction>(extraBufferCapacity = 1)
    val actions: SharedFlow<AdaptyOnboardingAction> = _actions.asSharedFlow()

    private val _analytics = MutableSharedFlow<AdaptyOnboardingAnalyticsEvent>(extraBufferCapacity = 1)
    val analytics: SharedFlow<AdaptyOnboardingAnalyticsEvent> = _analytics.asSharedFlow()

    private val _errors = MutableSharedFlow<AdaptyOnboardingError>(extraBufferCapacity = 1)
    val errors: SharedFlow<AdaptyOnboardingError> = _errors.asSharedFlow()

    private val _loadedEvents = MutableSharedFlow<OnboardingLoadedEvent>(extraBufferCapacity = 1)
    val loadedEvents: SharedFlow<OnboardingLoadedEvent> = _loadedEvents.asSharedFlow()

    var onboardingConfig: AdaptyOnboardingConfiguration? = null
    var hasFinishedLoading: Boolean = false

    fun processMessage(message: String) {
        deserializer.deserialize(message)
            .fold(
                { result ->
                    when (result) {
                        is OneOf3.First -> _actions.tryEmit(result.value)
                        is OneOf3.Second -> handleAnalyticsEvent(result.value)
                        is OneOf3.Third -> _loadedEvents.tryEmit(result.value)
                    }
                },
            ) { e ->
                emitError(AdaptyOnboardingError.Unknown(e))
            }
    }

    private fun handleAnalyticsEvent(event: AdaptyOnboardingAnalyticsEvent) {
        _analytics.tryEmit(event)

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
        _errors.tryEmit(error)
    }
}
