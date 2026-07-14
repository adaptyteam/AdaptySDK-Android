@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import com.adapty.internal.utils.InternalAdaptyApi

internal data class AppearanceAnimation(
    val background: Animation<*>?,
    val content: List<Animation<*>>,
)

internal data class ScreenTransition(
    val outgoing: List<Animation<*>>,
    val incoming: List<Animation<*>>,
    val isIncomingOnTop: Boolean,
)
