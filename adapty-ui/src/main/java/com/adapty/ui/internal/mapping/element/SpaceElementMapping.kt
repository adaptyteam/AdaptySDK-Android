@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.SpaceElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toSpaceElement(assets: Assets): UIElement {
    return SpaceElement(BaseProps(weight = this["count"]?.toWeightOrNull() ?: 1f))
}
