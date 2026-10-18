package com.adapty.ui.internal.ui.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.Align

@InternalAdaptyApi
public data class OverlayItem internal constructor(
    internal val align: Align,
    internal val content: UIElement,
)
