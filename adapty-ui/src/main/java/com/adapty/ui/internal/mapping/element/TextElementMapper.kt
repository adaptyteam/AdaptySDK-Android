@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.mapping.attributes.TextAttributeMapper
import com.adapty.ui.internal.text.toStringId
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.ui.element.BaseTextElement.OnOverflowMode
import com.adapty.ui.internal.ui.element.TextElement
import com.adapty.ui.internal.ui.element.UIElement

internal class TextElementMapper(
    private val textAttributeMapper: TextAttributeMapper,
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIElementMapper("text", commonAttributeMapper), UIPlainElementMapper {

    override fun map(config: Map<*, *>, assets: Assets, refBundles: ReferenceBundles): UIElement {
        return TextElement(
            config["string_id"]?.toStringId()
                ?: throw adaptyError(
                    message = "string_id in Text must not be empty",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                ),
            textAttributeMapper.mapTextAlign(config["align"]),
            (config["max_rows"] as? Number)?.toInt()?.takeIf { it > 0 },
            (config["on_overflow"] as? List<*>)?.let {
                if ("scale" !in it) return@let null
                OnOverflowMode.SCALE
            },
            config.toTextAttributes(),
            config.extractBaseProps(),
        ).also { element ->
            addToReferenceTargetsIfNeeded(config, element, refBundles)
        }
    }

    private fun Map<*, *>.toTextAttributes(): Attributes {
        return Attributes(
            this["font"] as? String,
            this["size"]?.toFloatOrNull(),
            (this["strike"] as? Boolean) ?: false,
            (this["underline"] as? Boolean) ?: false,
            (this["color"] as? String)?.let { assetId -> Shape.Fill(assetId) },
            (this["background"] as? String)?.let { assetId -> Shape.Fill(assetId) },
            (this["tint"] as? String)?.let { assetId -> Shape.Fill(assetId) },
        )
    }
}