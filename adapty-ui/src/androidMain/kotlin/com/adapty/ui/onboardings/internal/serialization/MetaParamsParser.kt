package com.adapty.ui.onboardings.internal.serialization

import com.adapty.ui.onboardings.AdaptyOnboardingMetaParams
import org.json.JSONObject

internal class MetaParamsParser : JsonObjectParser<AdaptyOnboardingMetaParams> {
    override fun parse(input: JSONObject): Result<AdaptyOnboardingMetaParams> {
        return runCatching {
            AdaptyOnboardingMetaParams(
                input.getString("onboarding_id"),
                input.getString("screen_cid"),
                input.getInt("screen_index"),
                input.getInt("total_screens"),
            )
        }
    }
}
