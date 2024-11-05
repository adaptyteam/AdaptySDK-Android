@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.SpaceElement
import com.adapty.ui.internal.ui.element.UIElement

internal class SpaceElementMapper(
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIElementMapper("space", commonAttributeMapper), UIPlainElementMapper {

    override fun map(config: Map<*, *>, assets: Assets, refBundles: ReferenceBundles): UIElement {
        return SpaceElement(BaseProps(weight = config["count"]?.toFloatOrNull() ?: 1f))
            .also { element ->
                addToReferenceTargetsIfNeeded(config, element, refBundles)
            }
    }
}