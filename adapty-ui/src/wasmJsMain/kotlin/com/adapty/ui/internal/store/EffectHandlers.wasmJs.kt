@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.store

import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.RemoteImage
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.webStub
import com.adapty.ui.internal.ui.FlowViewModel
import com.adapty.ui.internal.ui.FlowViewModelArgs
import com.adapty.ui.internal.ui.MediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

// No core SDK on web: only media loading is real; products, purchases and analytics stay stubs.
internal actual fun platformEffectHandlers(
    vm: FlowViewModel,
    args: FlowViewModelArgs,
    scope: CoroutineScope,
): List<(Effect, (Message) -> Unit) -> Unit> = listOf(
    RemoteImageEffectHandler(scope, args.media)::handle,
    ::stubSdkEffects,
)

/** Web counterpart of the Android DataLoadingEffectHandler's LoadRemoteImage branch (same contract). */
internal class RemoteImageEffectHandler(
    private val scope: CoroutineScope,
    private val mediaSource: MediaSource,
) {
    fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        if (effect !is Effect.LoadRemoteImage) return
        scope.launch {
            try {
                val image = loadImage(effect.remoteImage)
                dispatch(Message.AssetLoaded(effect.assetId, image))
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun loadImage(remoteImage: RemoteImage): Image =
        suspendCancellableCoroutine { continuation ->
            mediaSource.loadImage(
                remoteImage,
                handlePreview = null,
                handleResult = { image -> continuation.resumeWith(Result.success(image)) },
            )
        }
}

private fun stubSdkEffects(effect: Effect, @Suppress("UNUSED_PARAMETER") dispatch: (Message) -> Unit) {
    when (effect) {
        is Effect.LoadProducts -> webStub("platformEffectHandlers.loadProducts")
        is Effect.AwaitPurchaseParams, is Effect.StartPurchaseFlow, is Effect.StartWebPurchase,
        Effect.StartRestoreFlow -> webStub("platformEffectHandlers.purchase")
        is Effect.LogShowFlow, is Effect.LogFlowEvent -> webStub("platformEffectHandlers.analytics")
        else -> Unit
    }
}
