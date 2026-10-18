@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.store

import com.adapty.ui.internal.platform.InternalAdaptyUiApi

internal fun interface EffectHandler {
    fun handle(effect: Effect, dispatch: (Message) -> Unit)
}
