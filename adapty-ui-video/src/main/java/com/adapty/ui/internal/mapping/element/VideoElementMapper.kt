@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.element.ImageElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.VideoElement

@InternalAdaptyApi
public class VideoElementMapper(
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIElementMapper("video", commonAttributeMapper), UIPlainElementMapper {
    override fun map(config: Map<*, *>, assets: Assets, refBundles: ReferenceBundles): UIElement {
        val assetId = (config["asset_id"] as? String)?.takeIf { it.isNotEmpty() }
            ?: throw adaptyError(
                message = "asset_id in Video must not be empty",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        checkAsset(assetId, assets)
        val previewAssetId = "${assetId}${VIDEO_PREVIEW_ASSET_SUFFIX}"
        checkAsset(previewAssetId, assets)
        val aspectRatio = commonAttributeMapper.mapAspectRatio(config["aspect"])
        val baseProps = config.extractBaseProps()
        return VideoElement(
            assetId,
            aspectRatio,
            (config["loop"] as? Boolean) ?: false,
            baseProps,
            ImageElement(
                previewAssetId,
                aspectRatio,
                baseProps,
            )
        )
            .also { element ->
                addToReferenceTargetsIfNeeded(config, element, refBundles)
            }
    }

    private companion object {
        const val VIDEO_PREVIEW_ASSET_SUFFIX = "\$\$preview"
    }
}