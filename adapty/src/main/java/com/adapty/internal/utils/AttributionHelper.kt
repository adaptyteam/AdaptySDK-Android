package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.AttributionData
import com.adapty.models.AttributionType
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AttributionHelper {

    @JvmSynthetic
    fun createAttributionData(
        attribution: Any,
        source: AttributionType,
        networkUserId: String?
    ) = AttributionData(
        source.toString(),
        convertAttribution(attribution),
        networkUserId
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
    }

    private val adjustAttributionClass: Class<*>? by lazy {
        try {
            Class.forName("com.adjust.sdk.AdjustAttribution")
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    private fun convertAdjustAttributionToMap(adjustAttribution: Any) = hashMapOf(
        "adgroup" to getAdjustProperty(adjustAttribution, "adgroup"),
        "adid" to getAdjustProperty(adjustAttribution, "adid"),
        "campaign" to getAdjustProperty(adjustAttribution, "campaign"),
        "click_label" to getAdjustProperty(adjustAttribution, "clickLabel"),
        "creative" to getAdjustProperty(adjustAttribution, "creative"),
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