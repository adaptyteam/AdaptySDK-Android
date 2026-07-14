@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.internal.utils.InternalAdaptyApi

internal fun interface EffectHandler {
    fun handle(effect: Effect, dispatch: (Message) -> Unit)
}
