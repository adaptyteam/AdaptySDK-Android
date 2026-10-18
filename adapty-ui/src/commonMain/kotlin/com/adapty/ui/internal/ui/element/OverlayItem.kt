package com.adapty.ui.internal.ui.element

import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.ui.attributes.Align

@InternalAdaptyUiApi
public data class OverlayItem internal constructor(
    internal val align: Align,
    internal val content: UIElement,
)
