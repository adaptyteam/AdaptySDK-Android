@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toAction
import com.adapty.ui.internal.mapping.attributes.toAspectRatio
import com.adapty.ui.internal.mapping.attributes.toAssetVisualValue
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.VideoElement

@InternalAdaptyApi
public fun Map<*, *>.toVideoElement(assets: Assets): UIElement {
    val assetId = this["asset_id"]?.toAssetVisualValue()
        ?: throw adaptyError(
            message = "asset_id in Video must not be empty",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val aspectRatio = this["aspect"].toAspectRatio()
    val actions = (this["action"] as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.toAction() }
        ?: (this["action"] as? Map<*, *>)?.toAction()?.let { action -> listOf(action) }.orEmpty()
    val baseProps = this.extractBaseProps()
    return VideoElement(
        assetId,
        aspectRatio,
        (this["loop"] as? Boolean) ?: true,
        actions,
        baseProps,
    )
}
