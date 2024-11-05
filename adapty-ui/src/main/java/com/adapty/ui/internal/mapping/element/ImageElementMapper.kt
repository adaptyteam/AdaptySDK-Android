@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.element.ImageElement
import com.adapty.ui.internal.ui.element.UIElement

internal class ImageElementMapper(
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIElementMapper("image", commonAttributeMapper), UIPlainElementMapper {
    override fun map(config: Map<*, *>, assets: Assets, refBundles: ReferenceBundles): UIElement {
        val assetId = (config["asset_id"] as? String)?.takeIf { it.isNotEmpty() }
            ?: throw adaptyError(
                message = "asset_id in Image must not be empty",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        checkAsset(assetId, assets)
        return ImageElement(
            assetId,
            commonAttributeMapper.mapAspectRatio(config["aspect"]),
            (config["tint"] as? String)?.let { assetId ->
                checkAsset(assetId, assets)
                Shape.Fill(assetId)
            },
            config.extractBaseProps(),
        )
            .also { element ->
                addToReferenceTargetsIfNeeded(config, element, refBundles)
            }
    }
}