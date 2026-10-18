@file:OptIn(InternalAdaptyUiApi::class)
@file:JvmName("PreviewDependenciesKt")
@file:JvmMultifileClass

package com.adapty.ui.preview

import androidx.compose.runtime.Composable
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.PlatformLocale
import com.adapty.ui.internal.platform.randomUUID
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.internal.text.MissingTagKind
import com.adapty.ui.internal.text.PriceConverter
import com.adapty.ui.internal.text.TagResolver
import com.adapty.ui.internal.text.TextResolver
import com.adapty.ui.internal.ui.FlowViewModelArgs
import com.adapty.ui.internal.ui.MediaSource
import com.adapty.ui.internal.ui.PriceFormatting
import com.adapty.ui.internal.ui.SdkMetaInfo
import com.adapty.ui.internal.ui.TimerValueStore
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyUiTagResolver
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

// Preview runs without Adapty.activate(): platform services come from previewDependencies().

/** Platform services of a preview; [stateHandler] is one JS engine shared by all previews. */
internal class PreviewDeps(
    val media: MediaSource,
    val timerStore: TimerValueStore,
    val metaInfo: SdkMetaInfo,
    val priceFormatting: PriceFormatting,
    val tagLocale: PlatformLocale,
    val stateHandler: StateHandler,
)

internal expect fun previewDependencies(ctx: PlatformContextHandle): PreviewDeps

internal expect fun previewVideoMapperOrNull(): ((Map<*, *>, Assets) -> UIElement)?

/** Listener used when the preview host passes none. */
internal expect fun defaultPreviewEventListener(): AdaptyFlowEventListener

/** Supplies a ViewModelStoreOwner to [content] where the host does not provide one. */
@Composable
internal expect fun ProvidePreviewViewModelStoreOwner(content: @Composable () -> Unit)

internal fun createPreviewViewModelArgs(
    ctx: PlatformContextHandle,
    previewEventCallback: PreviewEventCallback?,
    onMissingTag: ((String, MissingTagKind) -> String)?,
): FlowViewModelArgs {
    val deps = previewDependencies(ctx)
    val tagResolver = TagResolver(
        deps.priceFormatting,
        PriceConverter(),
        AdaptyUiTagResolver.Default,
        deps.tagLocale,
        onMissingTag,
    )
    return FlowViewModelArgs(
        "preview_${randomUUID().toString().hashCode()}",
        false,
        deps.media,
        deps.timerStore,
        deps.metaInfo,
        TextResolver(tagResolver),
        deps.stateHandler,
    ) { scope ->
        listOf(PreviewPurchaseEffectHandler(scope, previewEventCallback)::handle)
    }
}
