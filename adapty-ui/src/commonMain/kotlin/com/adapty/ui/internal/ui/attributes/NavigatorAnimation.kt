@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.ui.attributes

import com.adapty.ui.internal.platform.InternalAdaptyUiApi

internal data class AppearanceAnimation(
    val background: Animation<*>?,
    val content: List<Animation<*>>,
)

internal data class ScreenTransition(
    val outgoing: List<Animation<*>>,
    val incoming: List<Animation<*>>,
    val isIncomingOnTop: Boolean,
)
