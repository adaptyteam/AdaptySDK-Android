@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.utils.CONFIGURATION_FORMAT_VERSION

internal class IfElementMapper(
    commonAttributeMapper: CommonAttributeMapper,
    private val hasVideoSupport: Boolean,
) : BaseUIComplexElementMapper("if", commonAttributeMapper), UIComplexShrinkableElementMapper {
    override fun map(
        config: Map<*, *>,
        assets: Assets,
        refBundles: ReferenceBundles,
        stateMap: MutableMap<String, Any>,
        inheritShrink: Int,
        childMapper: ChildMapperShrinkable,
    ): UIElement {
        val key = when {
            (config["platform"] != null && config["platform"] != "android")
                    || (config["version"] != null && config["version"] != CONFIGURATION_FORMAT_VERSION) -> "else"
            else -> {
                setOf("then", "else").firstOrNull { key ->
                    hasVideoSupport || (config[key] as? Map<*, *>)?.get("type") != "video"
                } ?: "then"
            }
        }
        return (config[key] as? Map<*, *>)?.let { item -> childMapper(item, inheritShrink) }
            ?: throw adaptyError(
                message = "$key in If must not be empty",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
    }
}