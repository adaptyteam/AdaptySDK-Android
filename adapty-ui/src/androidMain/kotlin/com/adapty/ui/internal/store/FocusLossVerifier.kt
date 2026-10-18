@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.store

import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class FocusLossVerifier(
    private val scope: CoroutineScope,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        if (effect !is Effect.VerifyFocusLost) return
        scope.launch {
            delay(FOCUS_LOSS_VERIFY_DELAY_MS)
            dispatch(Message.FocusLostConfirmed(effect.focusId, effect.generation))
        }
    }

    private companion object {
        const val FOCUS_LOSS_VERIFY_DELAY_MS = 150L
    }
}
