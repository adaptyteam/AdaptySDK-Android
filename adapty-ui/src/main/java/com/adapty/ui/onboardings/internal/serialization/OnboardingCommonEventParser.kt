package com.adapty.ui.onboardings.internal.serialization

import com.adapty.ui.onboardings.events.AdaptyOnboardingAnalyticsEvent
import org.json.JSONObject

internal class OnboardingCommonEventParser(
    private val eventsParser: OnboardingEventsParser,
) : JsonObjectParser<AdaptyOnboardingAnalyticsEvent> {

    override fun parse(input: JSONObject): Result<AdaptyOnboardingAnalyticsEvent> {
        return runCatching {
            eventsParser.parse(input)
                .getOrThrow()
        }
    }
}
