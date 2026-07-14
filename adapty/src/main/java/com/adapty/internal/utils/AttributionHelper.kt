@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.AttributionData
import com.google.gson.Gson

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AttributionHelper(
    private val gson: Gson,
) {

    fun createAttributionData(
        attribution: Map<String, Any>,
        source: String,
        profileId: String,
    ) = createAttributionData(gson.toJson(attribution), source, profileId)

    fun createAttributionData(
        attributionJson: String,
        source: String,
        profileId: String,
    ) = AttributionData(
        source,
        attributionJson,
        profileId,
    )
}
