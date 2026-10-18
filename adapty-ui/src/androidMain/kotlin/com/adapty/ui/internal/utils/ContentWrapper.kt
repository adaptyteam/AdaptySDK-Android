@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.utils

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.Align
import com.adapty.ui.internal.ui.attributes.Offset
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.UIElement

internal class ContentWrapper(
    val content: UIElement,
    val contentAlign: Align,
    val wrapperProps: BaseProps?,
    val offset: Offset?,
)
