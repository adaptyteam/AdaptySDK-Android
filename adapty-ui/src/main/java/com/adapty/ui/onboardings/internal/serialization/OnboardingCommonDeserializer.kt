@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.onboardings.internal.serialization

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.ui.onboardings.actions.AdaptyOnboardingAction
import com.adapty.ui.onboardings.events.*
import com.adapty.ui.onboardings.internal.util.OneOf
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import org.json.JSONObject

internal class OnboardingCommonDeserializer(
    private val actionsParser: OnboardingActionsParser,
    private val commonEventsParser: OnboardingCommonEventParser
) : Deserializer<OneOf<AdaptyOnboardingAction, AdaptyOnboardingAnalyticsEvent>> {

    companion object {
        private val actionTypes = setOf("state_updated", "open_paywall", "close", "custom", "onboarding_loaded")
        private val eventTypes = setOf("analytics")
    }

    override fun deserialize(input: String): Result<OneOf<AdaptyOnboardingAction, AdaptyOnboardingAnalyticsEvent>> {
        return runCatching {
            val jsonObject = JSONObject(input)

            when (val type = jsonObject.getString("type")) {
                in actionTypes -> actionsParser.parse(jsonObject)
                    .map { action -> OneOf.First<AdaptyOnboardingAction, AdaptyOnboardingAnalyticsEvent>(action) }
                    .getOrThrow()

                in eventTypes -> commonEventsParser.parse(jsonObject)
                    .map { event -> OneOf.Second<AdaptyOnboardingAction, AdaptyOnboardingAnalyticsEvent>(event) }
                    .getOrThrow()

                else -> throw IllegalArgumentException("Failed to parse the type '${type}' in root")
            }
        }.onFailure { e -> log(ERROR) { "$LOG_PREFIX_ERROR OnboardingCommonDeserializer failed: ${e.message}) ; input: $input" } }
    }
}
