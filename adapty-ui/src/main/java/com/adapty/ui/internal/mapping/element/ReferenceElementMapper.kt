@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.element.ReferenceElement
import com.adapty.ui.internal.ui.element.UIElement

internal class ReferenceElementMapper(
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIElementMapper("reference", commonAttributeMapper), UIPlainElementMapper {
    override fun map(config: Map<*, *>, assets: Assets, refBundles: ReferenceBundles): UIElement {
        return ReferenceElement(
            (config["element_id"] as? String)?.takeIf { it.isNotEmpty() }
                ?: throw adaptyError(
                    message = "element_id in Reference must not be empty",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                ),
        )
    }
}