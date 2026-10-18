@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.ScreenHolderElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toScreenHolderElement(assets: Assets): UIElement {
    return ScreenHolderElement(BaseProps.EMPTY)
}
