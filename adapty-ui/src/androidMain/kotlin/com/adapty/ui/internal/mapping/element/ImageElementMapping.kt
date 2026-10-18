@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.ui.internal.mapping.attributes.toAspectRatio
import com.adapty.ui.internal.mapping.attributes.toAssetVisualValue
import com.adapty.ui.internal.mapping.attributes.toVisualValue
import com.adapty.ui.internal.platform.AdaptyErrorCode
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.adaptyError
import com.adapty.ui.internal.ui.element.ImageElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toImageElement(assets: Assets): UIElement {
    val assetId = this["asset_id"]?.toAssetVisualValue()
        ?: throw adaptyError(
            message = "asset_id in Image must not be empty",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    return ImageElement(
        assetId,
        this["aspect"].toAspectRatio(),
        this["tint"]?.toVisualValue(),
        this.extractBaseProps(),
    )
}
