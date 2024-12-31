@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.AttributionData
import com.google.gson.Gson
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AttributionHelper(
    private val gson: Gson,
) {

    @JvmSynthetic
    fun createAttributionData(
        attribution: Any,
        source: String,
        profileId: String,
    ) = AttributionData(
        source,
        convertAttribution(attribution),
        profileId
    )

    private fun convertAttribution(attribution: Any) = when {
        attribution is JSONObject -> {
            hashMapOf<String, Any>().apply {
                for (key in attribution.keys()) {
                    this[key] = attribution.get(key)
                }
            }
        }
        adjustAttributionClass?.isAssignableFrom(attribution::class.java) == true -> {
            convertAdjustAttributionToMap(attribution)
        }
        else -> {
            attribution
        }
    }.let(gson::toJson)

    private val adjustAttributionClass: Class<*>? by lazy {
        getClassForNameOrNull("com.adjust.sdk.AdjustAttribution")
    }

    private fun convertAdjustAttributionToMap(adjustAttribution: Any) = mapOf(
        "adgroup" to getAdjustProperty(adjustAttribution, "adgroup"),
        "adid" to getAdjustProperty(adjustAttribution, "adid"),
        "campaign" to getAdjustProperty(adjustAttribution, "campaign"),
        "click_label" to getAdjustProperty(adjustAttribution, "clickLabel"),
        "creative" to getAdjustProperty(adjustAttribution, "creative"),
        "fbInstallReferrer" to getAdjustProperty(adjustAttribution, "fbInstallReferrer"),
        "network" to getAdjustProperty(adjustAttribution, "network"),
        "tracker_name" to getAdjustProperty(adjustAttribution, "trackerName"),
        "tracker_token" to getAdjustProperty(adjustAttribution, "trackerToken")
    )

    private fun getAdjustProperty(adjustAttribution: Any, propName: String): Any {
        return try {
            adjustAttributionClass?.getField(propName)?.get(adjustAttribution) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}