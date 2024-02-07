package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ViewConfigurationDto(
    @SerializedName("paywall_builder_config")
    val config: ViewConfigurationConfig?,
    @SerializedName("paywall_builder_id")
    val id: String?,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ViewConfigurationConfig(
    @SerializedName("assets")
    val assets: List<Asset>?,
    @SerializedName("default_localization")
    val defaultLocalization: String?,
    @SerializedName("is_hard_paywall")
    val isHard: Boolean?,
    @SerializedName("localizations")
    val localizations: List<Localization>,
    @SerializedName("styles")
    val styles: HashMap<String, Any?>,
    @SerializedName("template_id")
    val templateId: String?,
    @SerializedName("main_image_relative_height")
    val mainImageRelativeHeight: Float?,
) {
    class Asset(
        @SerializedName("id")
        val id: String?,
        @SerializedName("size")
        val size: Float?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("color")
        val color: String?,
        @SerializedName("value")
        val value: Any?,
        @SerializedName("points")
        val points: HashMap<String, Float?>?,
        @SerializedName("horizontal_align")
        val horizontalAlign: String?,
        @SerializedName("family_name")
        val familyName: String?,
        @SerializedName("weight")
        val weight: Int?,
        @SerializedName("italic")
        val isItalic: Boolean?,
        @SerializedName("resources")
        val resources: Array<String>?,
        @SerializedName("values")
        val values: List<HashMap<String, Any?>?>?,
    )

    class Localization(
        @SerializedName("id")
        val id: String?,
        @SerializedName("strings")
        val strings: List<Str>?,
        @SerializedName("assets")
        val assets: List<Asset>?,
    ) {
        class Str(
            @SerializedName("id")
            val id: String?,
            @SerializedName("value")
            val value: String?,
            @SerializedName("has_tags")
            val hasTags: Boolean?,
            @SerializedName("fallback")
            val fallback: String?,
        )
    }
}