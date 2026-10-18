@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.preview

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Effect
import com.adapty.ui.internal.store.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Answers preview purchase/restore effects with a mocked success after [MOCK_DELAY_MS]. */
internal class PreviewPurchaseEffectHandler(
    private val scope: CoroutineScope,
    private val previewCallback: PreviewEventCallback? = null,
) {

    fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        when (effect) {
            is Effect.PreviewPurchase -> scope.launch {
                previewCallback?.onPurchaseStarted(effect.productId)
                delay(MOCK_DELAY_MS)
                previewCallback?.onPurchaseFinished(effect.productId, RESULT_SUCCESS)
                dispatch(Message.PreviewPurchaseCompleted(effect.productId, effect.callbackId))
            }
            is Effect.PreviewRestore -> scope.launch {
                previewCallback?.onRestoreStarted()
                delay(MOCK_DELAY_MS)
                previewCallback?.onRestoreFinished(RESULT_SUCCESS)
                dispatch(Message.PreviewRestoreCompleted(effect.callbackId))
            }
            else -> Unit
        }
    }

    companion object {
        const val MOCK_DELAY_MS = 1500L
        private const val RESULT_SUCCESS = "success"
    }
}
