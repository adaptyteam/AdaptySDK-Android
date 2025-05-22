@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.onboardings.internal.serialization

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.ui.onboardings.actions.AdaptyOnboardingAction
import com.adapty.ui.onboardings.events.*
import com.adapty.ui.onboardings.internal.util.OnboardingLoadedEvent
import com.adapty.ui.onboardings.internal.util.OneOf
import com.adapty.ui.onboardings.internal.util.OneOf3
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import org.json.JSONObject

internal class OnboardingCommonDeserializer(
    private val actionsParser: OnboardingActionsParser,
    private val commonEventsParser: OnboardingCommonEventParser
) : Deserializer<OneOf3<AdaptyOnboardingAction, AdaptyOnboardingAnalyticsEvent, OnboardingLoadedEvent>> {

    companion object {
        private val actionTypes = setOf("state_updated", "open_paywall", "close", "custom")
        private val eventTypes = setOf("analytics", "onboarding_loaded")
    }

    override fun deserialize(input: String): Result<OneOf3<AdaptyOnboardingAction, AdaptyOnboardingAnalyticsEvent, OnboardingLoadedEvent>> {
        return runCatching {
            val jsonObject = JSONObject(input)

            when (val type = jsonObject.getString("type")) {
                in actionTypes -> actionsParser.parse(jsonObject)
                    .map { action -> OneOf3.First<AdaptyOnboardingAction, AdaptyOnboardingAnalyticsEvent, OnboardingLoadedEvent>(action) }
                    .getOrThrow()

                in eventTypes -> commonEventsParser.parse(jsonObject)
                    .map { event ->
                        when (event) {
                            is OneOf.First -> OneOf3.Second<AdaptyOnboardingAction, AdaptyOnboardingAnalyticsEvent, OnboardingLoadedEvent>(event.value)
                            is OneOf.Second -> OneOf3.Third<AdaptyOnboardingAction, AdaptyOnboardingAnalyticsEvent, OnboardingLoadedEvent>(event.value)
                        }
                    }
                    .getOrThrow()

                else -> throw IllegalArgumentException("Failed to parse the type '${type}' in root")
            }
        }.onFailure { e -> log(ERROR) { "$LOG_PREFIX_ERROR OnboardingCommonDeserializer failed: ${e.message}) ; input: $input" } }
    }
}
