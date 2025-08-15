package com.adapty.ui.onboardings.internal.serialization

import org.json.JSONObject

internal interface JsonObjectParser<T> {

    fun parse(input: JSONObject): Result<T>
}