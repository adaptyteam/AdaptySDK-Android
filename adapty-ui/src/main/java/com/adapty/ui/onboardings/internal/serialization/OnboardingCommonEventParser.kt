package com.adapty.ui.onboardings.internal.serialization

import com.adapty.ui.onboardings.events.*
import com.adapty.ui.onboardings.internal.util.OnboardingLoadedEvent
import com.adapty.ui.onboardings.internal.util.OneOf
import org.json.JSONObject

internal class OnboardingCommonEventParser(
    private val eventsParser: OnboardingEventsParser,
    private val loadedEventParser: OnboardingLoadedEventParser
) : JsonObjectParser<OneOf<AdaptyOnboardingAnalyticsEvent, OnboardingLoadedEvent>> {

    override fun parse(input: JSONObject): Result<OneOf<AdaptyOnboardingAnalyticsEvent, OnboardingLoadedEvent>> {
        return runCatching {
            when (input.getString("type")) {
                "onboarding_loaded" -> loadedEventParser.parse(input)
                    .map { event -> OneOf.Second<AdaptyOnboardingAnalyticsEvent, OnboardingLoadedEvent>(event) }
                    .getOrThrow()

                else -> eventsParser.parse(input)
                    .map { event -> OneOf.First<AdaptyOnboardingAnalyticsEvent, OnboardingLoadedEvent>(event) }
                    .getOrThrow()
            }
        }
    }
}
