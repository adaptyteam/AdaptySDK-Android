@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform.ui

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.ACTION
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.ACTION_ID
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.ERROR
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.EVENT
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.META
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.VIEW
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCloseAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCustomAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingInputParams
import com.adapty.ui.onboardings.actions.AdaptyOnboardingLoadedAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingOpenPaywallAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingStateUpdatedAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingStateUpdatedParams
import com.adapty.ui.onboardings.errors.AdaptyOnboardingError
import com.adapty.ui.onboardings.events.AdaptyOnboardingAnalyticsEvent

class AdaptyUiEvent private constructor(
    val id: String,
    val data: Map<String, Any>,
) {
    constructor(
        id: String,
        vararg data: Pair<String, Any>,
    ): this(
        id,
        data.toMap().toMutableMap().apply { put(ID, id) },
    )

    companion object {
        private const val ID = "id"
    }
}

fun AdaptyUiEvent.Companion.fromOnboardingEvent(
    error: AdaptyOnboardingError,
    view: AdaptyUiOnboardingView,
): AdaptyUiEvent {
    return AdaptyUiEvent(
        "onboarding_did_fail_with_error",
        VIEW to view,
        ERROR to error.asAdaptyError(),
    )
}

fun AdaptyUiEvent.Companion.fromOnboardingEvent(
    event: AdaptyOnboardingAnalyticsEvent,
    view: AdaptyUiOnboardingView,
): AdaptyUiEvent {
    return AdaptyUiEvent(
        "onboarding_on_analytics_action",
        VIEW to view,
        META to event.meta,
        EVENT to event.toMap(),
    )
}

fun AdaptyUiEvent.Companion.fromOnboardingEvent(
    event: AdaptyOnboardingLoadedAction,
    view: AdaptyUiOnboardingView,
): AdaptyUiEvent {
    return AdaptyUiEvent(
        "onboarding_did_finish_loading",
        VIEW to view,
        META to event.meta,
    )
}

fun AdaptyUiEvent.Companion.fromOnboardingEvent(
    event: AdaptyOnboardingCloseAction,
    view: AdaptyUiOnboardingView,
): AdaptyUiEvent {
    return AdaptyUiEvent(
        "onboarding_on_close_action",
        VIEW to view,
        META to event.meta,
        ACTION_ID to event.actionId,
    )
}

fun AdaptyUiEvent.Companion.fromOnboardingEvent(
    event: AdaptyOnboardingCustomAction,
    view: AdaptyUiOnboardingView,
): AdaptyUiEvent {
    return AdaptyUiEvent(
        "onboarding_on_custom_action",
        VIEW to view,
        META to event.meta,
        ACTION_ID to event.actionId,
    )
}

fun AdaptyUiEvent.Companion.fromOnboardingEvent(
    event: AdaptyOnboardingOpenPaywallAction,
    view: AdaptyUiOnboardingView,
): AdaptyUiEvent {
    return AdaptyUiEvent(
        "onboarding_on_paywall_action",
        VIEW to view,
        META to event.meta,
        ACTION_ID to event.actionId,
    )
}

fun AdaptyUiEvent.Companion.fromOnboardingEvent(
    event: AdaptyOnboardingStateUpdatedAction,
    view: AdaptyUiOnboardingView,
): AdaptyUiEvent {
    return AdaptyUiEvent(
        "onboarding_on_state_updated_action",
        VIEW to view,
        META to event.meta,
        ACTION to event.toMap(),
    )
}

private fun AdaptyOnboardingError.asAdaptyError(): AdaptyError {
    return adaptyError(
        adaptyErrorCode = AdaptyErrorCode.UNKNOWN,
        message = this.message,
    )
}

private val AdaptyOnboardingError.message get() = when (this) {
    is AdaptyOnboardingError.ActivateOnce -> "AdaptyOnboardingError.ActivateOnce"
    is AdaptyOnboardingError.NotActivated -> "AdaptyOnboardingError.NotActivated"
    is AdaptyOnboardingError.Unknown -> "AdaptyOnboardingError.Unknown"
    is AdaptyOnboardingError.WebKit.Http -> this.toString()
    is AdaptyOnboardingError.WebKit.Ssl -> this.toString()
    is AdaptyOnboardingError.WebKit.WebResource -> this.toString()
    is AdaptyOnboardingError.WrongApiKey -> "AdaptyOnboardingError.WrongApiKey"
}

private val AdaptyOnboardingAnalyticsEvent.name get() = when (this) {
    is AdaptyOnboardingAnalyticsEvent.OnboardingCompleted -> "onboarding_completed"
    is AdaptyOnboardingAnalyticsEvent.OnboardingStarted -> "onboarding_started"
    is AdaptyOnboardingAnalyticsEvent.ProductsScreenPresented -> "products_screen_presented"
    is AdaptyOnboardingAnalyticsEvent.RegistrationScreenPresented -> "registration_screen_presented"
    is AdaptyOnboardingAnalyticsEvent.ScreenCompleted -> "screen_completed"
    is AdaptyOnboardingAnalyticsEvent.ScreenPresented -> "screen_presented"
    is AdaptyOnboardingAnalyticsEvent.SecondScreenPresented -> "second_screen_presented"
    is AdaptyOnboardingAnalyticsEvent.Unknown -> "unknown"
    is AdaptyOnboardingAnalyticsEvent.UserEmailCollected -> "user_email_collected"
}

private fun AdaptyOnboardingAnalyticsEvent.toMap() = mutableMapOf(
    "name" to this.name
).apply {
    val event = this@toMap
    if (event is AdaptyOnboardingAnalyticsEvent.ScreenCompleted) {
        event.elementId?.let { put("element_id", it) }
        event.reply?.let { put("reply", it) }
    }
}

private fun AdaptyOnboardingStateUpdatedAction.toMap() =
    when (val params = this.params) {
        is AdaptyOnboardingStateUpdatedParams.DatePicker -> mapOf(
            "element_id" to this.elementId,
            "element_type" to "date_picker",
            "value" to mapOf(
                "day" to params.params.day,
                "month" to params.params.month,
                "year" to params.params.year,
            ),
        )
        is AdaptyOnboardingStateUpdatedParams.Input -> mapOf(
            "element_id" to this.elementId,
            "element_type" to "input",
            "value" to when (val inputParams = params.params) {
                is AdaptyOnboardingInputParams.Email -> mapOf(
                    "type" to "email",
                    "value" to inputParams.value,
                )
                is AdaptyOnboardingInputParams.Number -> mapOf(
                    "type" to "number",
                    "value" to inputParams.value,
                )
                is AdaptyOnboardingInputParams.Text -> mapOf(
                    "type" to "text",
                    "value" to inputParams.value,
                )
            },
        )
        is AdaptyOnboardingStateUpdatedParams.MultiSelect -> mapOf(
            "element_id" to this.elementId,
            "element_type" to "multi_select",
            "value" to params.params.map {
                mapOf(
                    "id" to it.id,
                    "value" to it.value,
                    "label" to it.label,
                )
            },
        )
        is AdaptyOnboardingStateUpdatedParams.Select -> mapOf(
            "element_id" to this.elementId,
            "element_type" to "select",
            "value" to mapOf(
                "id" to params.params.id,
                "value" to params.params.value,
                "label" to params.params.label,
            ),
        )
    }
