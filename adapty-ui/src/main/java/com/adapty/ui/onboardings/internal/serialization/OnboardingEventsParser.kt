package com.adapty.ui.onboardings.internal.serialization

import com.adapty.ui.onboardings.events.*
import org.json.JSONObject

internal class OnboardingEventsParser(
    private val metaParamsParser: MetaParamsParser
) : JsonObjectParser<AdaptyOnboardingAnalyticsEvent> {

    private val eventParsers = mapOf(
        "onboarding_started" to { input: JSONObject ->
            metaParamsParser.parse(input.getJSONObject("meta"))
                .map { AdaptyOnboardingAnalyticsEvent.OnboardingStarted(it) }
        },
        "screen_presented" to { input: JSONObject ->
            metaParamsParser.parse(input.getJSONObject("meta"))
                .map { AdaptyOnboardingAnalyticsEvent.ScreenPresented(it) }
        },
        "screen_completed" to { input: JSONObject ->
            val meta = metaParamsParser.parse(input.getJSONObject("meta")).getOrThrow()
            val params = input.optJSONObject("params")
            Result.success(
                AdaptyOnboardingAnalyticsEvent.ScreenCompleted(
                    meta,
                    params?.optString("element_id"),
                    params?.optString("reply"),
                )
            )
        },
        "second_screen_presented" to { input: JSONObject ->
            metaParamsParser.parse(input.getJSONObject("meta"))
                .map { AdaptyOnboardingAnalyticsEvent.SecondScreenPresented(it) }
        },
        "registration_screen_presented" to { input: JSONObject ->
            metaParamsParser.parse(input.getJSONObject("meta"))
                .map { AdaptyOnboardingAnalyticsEvent.RegistrationScreenPresented(it) }
        },
        "products_screen_presented" to { input: JSONObject ->
            metaParamsParser.parse(input.getJSONObject("meta"))
                .map { AdaptyOnboardingAnalyticsEvent.ProductsScreenPresented(it) }
        },
        "user_email_collected" to { input: JSONObject ->
            metaParamsParser.parse(input.getJSONObject("meta"))
                .map { AdaptyOnboardingAnalyticsEvent.UserEmailCollected(it) }
        },
        "onboarding_completed" to { input: JSONObject ->
            metaParamsParser.parse(input.getJSONObject("meta"))
                .map { AdaptyOnboardingAnalyticsEvent.OnboardingCompleted(it) }
        },
    )

    override fun parse(input: JSONObject): Result<AdaptyOnboardingAnalyticsEvent> {
        return eventParsers[input.getString("name")]?.invoke(input)
            ?: metaParamsParser.parse(input.optJSONObject("meta") ?: JSONObject())
                .map { meta -> AdaptyOnboardingAnalyticsEvent.Unknown(meta, input.getString("name")) }
    }
}
