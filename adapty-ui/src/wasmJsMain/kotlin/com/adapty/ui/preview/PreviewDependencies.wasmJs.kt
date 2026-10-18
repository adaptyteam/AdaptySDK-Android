@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.preview

import com.adapty.ui.internal.text.WebFonts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.RemoteImage
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.platform.BigDecimalLike
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.PlatformLocale
import com.adapty.ui.internal.platform.defaultLocale
import com.adapty.ui.internal.platform.language
import com.adapty.ui.internal.platform.country
import com.adapty.ui.internal.platform.localeForTag
import com.adapty.ui.internal.platform.webStub
import com.adapty.ui.internal.script.JSActionBridge
import com.adapty.ui.internal.script.JSStateHandler
import com.adapty.ui.internal.script.JSStateMachine
import com.adapty.ui.internal.ui.MediaSource
import com.adapty.ui.internal.ui.PriceFormatting
import com.adapty.ui.internal.ui.SdkMetaInfo
import com.adapty.ui.internal.ui.TimerValueStore
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyFlowEventListenerBase

// Browser preview: no core SDK, no disk cache; timers live in memory, media is inline-only.

private var sharedPreviewStateHandler: JSStateHandler? = null

private val previewTimerStore: TimerValueStore by lazy { InMemoryTimerValueStore() }

private val previewSdkMetaInfo: SdkMetaInfo by lazy { WebPreviewSdkMetaInfo() }

internal actual fun previewDependencies(ctx: PlatformContextHandle): PreviewDeps =
    PreviewDeps(
        media = InlineOnlyMediaSource,
        timerStore = previewTimerStore,
        metaInfo = previewSdkMetaInfo,
        priceFormatting = StubPriceFormatting,
        tagLocale = localeForTag("en-US"),
        stateHandler = sharedPreviewStateHandler
            ?: JSStateHandler(JSStateMachine(ctx, JSActionBridge())).also { sharedPreviewStateHandler = it },
    )

internal actual fun previewVideoMapperOrNull(): ((Map<*, *>, Assets) -> UIElement)? = null

internal actual fun defaultPreviewEventListener(): AdaptyFlowEventListener = AdaptyFlowEventListenerBase()

@Composable
internal actual fun ProvidePreviewViewModelStoreOwner(content: @Composable () -> Unit) {
    // Preview root on web: register glyph fallback fonts before any text is composed
    WebFonts.RegisterFallbacks()
    if (LocalViewModelStoreOwner.current != null) {
        content()
        return
    }
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}

private class InMemoryTimerValueStore : TimerValueStore {
    private val values = HashMap<String, Long>()

    override fun getLong(key: String, isPersisted: Boolean): Long? = values[key]

    override fun setLong(key: String, value: Long, isPersisted: Boolean) {
        values[key] = value
    }
}

private class WebPreviewSdkMetaInfo : SdkMetaInfo {
    override val appBuildAndVersion: Pair<String, String> = "1" to "1.0"
    override val currentLocale: PlatformLocale? get() = defaultLocale()
    override val userLocales: List<String>
        get() = defaultLocale().let { if (it.country.isEmpty()) it.language else "${it.language}-${it.country}" }
            .let(::listOf)
    override val adaptySdkVersion: String = "4.2.2"
    override val os: String = "Android"
    override val deviceName: String = "web"
    override val applicationId: String = "io.adapty.ui.gallery.web"
    override val manufacturer: String = "web"
    override val is24HourFormat: Boolean = true
}

/** Serves only an inline (base64) preview; remote URLs are not fetched. */
private object InlineOnlyMediaSource : MediaSource {
    override fun loadImage(
        remoteImage: RemoteImage,
        handlePreview: ((preview: Image) -> Unit)?,
        handleResult: ((image: Image) -> Unit)?,
    ) {
        val preview = remoteImage.preview
        if (preview != null && preview.source is Image.Source.Base64Str) {
            handlePreview?.invoke(preview)
        }
        webStub("MediaSource.remoteImage")
    }
}

private object StubPriceFormatting : PriceFormatting {
    override fun format(newPriceValue: BigDecimalLike, originalFormattedPrice: String): String {
        webStub("PriceFormatting.format")
        return originalFormattedPrice
    }
}
