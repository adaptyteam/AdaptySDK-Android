package com.adapty.ui.onboardings.internal.serialization

import com.adapty.ui.onboardings.internal.util.OnboardingLoadedEvent
import org.json.JSONObject

internal class OnboardingLoadedEventParser(
    private val metaParamsParser: MetaParamsParser
) : JsonObjectParser<OnboardingLoadedEvent> {

    override fun parse(input: JSONObject): Result<OnboardingLoadedEvent> {
        return metaParamsParser.parse(input.getJSONObject("meta"))
            .map { OnboardingLoadedEvent(it) }
    }
}
