package com.adapty.ui.onboardings.internal.serialization

import com.adapty.ui.onboardings.actions.*
import org.json.JSONObject

internal class OnboardingActionsParser(
    private val metaParamsParser: MetaParamsParser,
    private val stateUpdatedParamsParser: OnboardingStateUpdatedParamsParser
) : JsonObjectParser<AdaptyOnboardingAction> {

    override fun parse(input: JSONObject): Result<AdaptyOnboardingAction> {
        return runCatching {
            when (input.getString("type")) {
                "state_updated" -> AdaptyOnboardingStateUpdatedAction(
                    input.getString("element_id"),
                    metaParamsParser.parse(input.getJSONObject("meta")).getOrThrow(),
                    stateUpdatedParamsParser.parse(input).getOrThrow()
                )
                "open_paywall" -> AdaptyOnboardingOpenPaywallAction(
                    input.getString("action_id"),
                    metaParamsParser.parse(input.getJSONObject("meta")).getOrThrow()
                )
                "close" -> AdaptyOnboardingCloseAction(
                    input.getString("action_id"),
                    metaParamsParser.parse(input.getJSONObject("meta")).getOrThrow()
                )
                "custom" -> AdaptyOnboardingCustomAction(
                    input.getString("action_id"),
                    metaParamsParser.parse(input.getJSONObject("meta")).getOrThrow()
                )
                "onboarding_loaded" -> AdaptyOnboardingLoadedAction(
                    metaParamsParser.parse(input.getJSONObject("meta")).getOrThrow()
                )
                else -> throw IllegalArgumentException("Unknown action type: ${input.getString("type")}")
            }
        }
    }
}
