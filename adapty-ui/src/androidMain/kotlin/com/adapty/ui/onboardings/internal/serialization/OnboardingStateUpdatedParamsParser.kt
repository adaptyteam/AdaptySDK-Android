package com.adapty.ui.onboardings.internal.serialization

import com.adapty.ui.onboardings.actions.AdaptyOnboardingDatePickerParams
import com.adapty.ui.onboardings.actions.AdaptyOnboardingInputParams
import com.adapty.ui.onboardings.actions.AdaptyOnboardingSelectParams
import com.adapty.ui.onboardings.actions.AdaptyOnboardingStateUpdatedParams
import org.json.JSONObject

internal class OnboardingStateUpdatedParamsParser: JsonObjectParser<AdaptyOnboardingStateUpdatedParams> {

    override fun parse(input: JSONObject): Result<AdaptyOnboardingStateUpdatedParams> {
        return kotlin.runCatching {
            when (val elementType = input.getString("element_type")) {
                "select" -> {
                    val value = input.getJSONObject("value")
                    AdaptyOnboardingStateUpdatedParams.Select(
                        AdaptyOnboardingSelectParams(
                            value.getString("id"),
                            value.getString("value"),
                            value.getString("label"),
                        )
                    )
                }
                "multi_select" -> {
                    val value = input.getJSONArray("value")
                    val params = mutableListOf<AdaptyOnboardingSelectParams>()
                    for (i in 0 until value.length()) {
                        val param = value.getJSONObject(i)
                        params.add(
                            AdaptyOnboardingSelectParams(
                                param.getString("id"),
                                param.getString("value"),
                                param.getString("label"),
                            )
                        )
                    }
                    AdaptyOnboardingStateUpdatedParams.MultiSelect(params)
                }
                "input" -> {
                    val value = input.getJSONObject("value")
                    AdaptyOnboardingStateUpdatedParams.Input(
                        when (val type = value.getString("type")) {
                            "text" -> AdaptyOnboardingInputParams.Text(value.getString("value"))
                            "number" -> AdaptyOnboardingInputParams.Number(value.getDouble("value"))
                            "email" -> AdaptyOnboardingInputParams.Email(value.getString("value"))
                            else -> throw RuntimeException("Failed to parse the type '${type}' in 'input'")
                        }
                    )
                }
                "date_picker" -> {
                    val value = input.getJSONObject("value")
                    AdaptyOnboardingStateUpdatedParams.DatePicker(
                        AdaptyOnboardingDatePickerParams(
                            if (value.has("day")) value.getInt("day") else null,
                            if (value.has("month")) value.getInt("month") else null,
                            if (value.has("year")) value.getInt("year") else null,
                        )
                    )
                }
                else -> {
                    throw RuntimeException("Failed to parse the elementType '${elementType}' in OnboardingsStateUpdatedParams")
                }
            }
        }
    }
}