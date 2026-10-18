@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.store

import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.text.TextResolver

internal class ConfigEffectHandler(
    private val textResolver: TextResolver,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        when (effect) {
            is Effect.UpdateTagResolver -> {
                textResolver.setCustomTagResolver(effect.resolver)
            }
            else -> return
        }
    }
}
