package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.google.gson.JsonElement
import com.google.gson.JsonObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun interface ResponseDataExtractor {

    fun extract(jsonElement: JsonElement): JsonElement?
}