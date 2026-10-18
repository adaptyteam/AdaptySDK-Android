@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui

import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import com.adapty.ui.internal.clearMediaCachePlatform
import com.adapty.ui.internal.configureMediaCachePlatform
import com.adapty.ui.internal.createFlowView
import com.adapty.ui.internal.createOnboardingConfiguration
import com.adapty.ui.internal.createOnboardingView
import com.adapty.ui.internal.defaultFlowLoadTimeout
import com.adapty.ui.internal.initAdaptyUIPlatform
import com.adapty.ui.internal.loadFlowConfiguration
import com.adapty.ui.internal.preloadMediaPlatform
import com.adapty.ui.internal.stateHandlerOrNull
import com.adapty.ui.internal.platform.Activity
import com.adapty.ui.internal.platform.AdaptyFlow
import com.adapty.ui.internal.platform.AdaptyOnboarding
import com.adapty.ui.internal.platform.AdaptyPaywallProduct
import com.adapty.ui.internal.platform.AdaptyWebPresentation
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformLocale
import com.adapty.ui.internal.platform.ResultCallback
import com.adapty.ui.internal.platform.TimeInterval
import com.adapty.ui.internal.platform.days
import com.adapty.ui.internal.store.FlowRuntimeState
import com.adapty.ui.internal.ui.element.BoxElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.utils.ContentWrapper
import com.adapty.ui.internal.utils.FlowMode
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import com.adapty.ui.onboardings.AdaptyOnboardingConfiguration
import com.adapty.ui.onboardings.AdaptyOnboardingView
import com.adapty.ui.onboardings.listeners.AdaptyOnboardingEventListener
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSynthetic

public object AdaptyUI {

    /**
     * Right after receiving [FlowConfiguration], you can create the corresponding
     * [AdaptyFlowView] to display it afterwards.
     *
     * This method should be called only on UI thread.
     *
     * @param[activity] An [Activity] instance.
     *
     * @param[flowConfiguration] A [FlowConfiguration] object containing information
     * about the visual part of the flow. To load it, use the [AdaptyUI.getFlowConfiguration] method.
     *
     * @param[products] Optional [AdaptyPaywallProduct] list. Pass this value in order to optimize
     * the display time of the products on the screen. If you pass `null`, `AdaptyUI` will
     * automatically fetch the required products.
     *
     * @param[eventListener] An object that implements the [AdaptyFlowEventListener] interface.
     * Use it to respond to different events happening inside the purchase screen.
     * Also you can extend [com.adapty.ui.listeners.AdaptyFlowDefaultEventListener] so you don't need to override all the methods.
     *
     * @param[insets] You can override the default window inset handling by specifying the [AdaptyFlowInsets].
     *
     * @param[customAssets] If you are going to use custom assets functionality, pass [AdaptyCustomAssets] here.
     *
     * @param[tagResolver] If you are going to use custom tags functionality, pass the resolver function here.
     *
     * @param[timerResolver] If you are going to use custom timer functionality, pass the resolver function here.
     *
     * @param[observerModeHandler] If you use Adapty in [Observer mode](https://adapty.io/docs/observer-vs-full-mode),
     * pass the [AdaptyUiObserverModeHandler] implementation to handle purchases on your own.
     *
     * @return An [AdaptyFlowView] object, representing the requested flow screen.
     */
    @JvmStatic
    @JvmOverloads
    @UiThread
    public fun getFlowView(
        activity: Activity,
        flowConfiguration: FlowConfiguration,
        products: List<AdaptyPaywallProduct>?,
        eventListener: AdaptyFlowEventListener,
        insets: AdaptyFlowInsets = AdaptyFlowInsets.Unspecified,
        customAssets: AdaptyCustomAssets = AdaptyCustomAssets.Empty,
        tagResolver: AdaptyUiTagResolver = AdaptyUiTagResolver.Default,
        timerResolver: AdaptyUiTimerResolver = AdaptyUiTimerResolver.Default,
        observerModeHandler: AdaptyUiObserverModeHandler? = null,
    ): AdaptyFlowView {
        return createFlowView(
            activity,
            flowConfiguration,
            products,
            eventListener,
            insets,
            customAssets,
            tagResolver,
            timerResolver,
            observerModeHandler,
        )
    }

    @Deprecated("Starting Adapty SDK 4.0.0, Onboarding Feature is deprecated. Please consider migrating to Flows")
    @Suppress("DEPRECATION")
    @JvmStatic
    @JvmOverloads
    @UiThread
    public fun getOnboardingView(
        activity: Activity,
        viewConfig: AdaptyOnboardingConfiguration,
        eventListener: AdaptyOnboardingEventListener,
        safeAreaPaddings: Boolean = true,
    ): AdaptyOnboardingView {
        return createOnboardingView(activity, viewConfig, eventListener, safeAreaPaddings)
    }

    /**
     * If you are using the [Flow Builder](https://adapty.io/docs/adapty-flow-builder),
     * you can use this method to get a configuration object for your flow.
     *
     * Should not be called before [Adapty.activate]
     *
     * @param[flow] The [AdaptyFlow] for which you want to get a configuration.
     *
     * @param[locale] The locale whose localization will be used for rendering. If `null`,
     * the `en` localization is used if present, otherwise the default localization
     * of the view configuration.
     *
     * @param[loadTimeout] This value limits the timeout for this method. The minimum value is 1 second.
     * If a timeout is not required, you can pass [TimeInterval.INFINITE].
     *
     * @param[customLayoutId] If the flow defines custom layouts, the id of the layout to render.
     * If `null`, the layout is chosen automatically based on the current device.
     *
     * @param[callback] A result containing the [FlowConfiguration] object.
     *
     * @see <a href="https://adapty.io/docs/adapty-flow-builder">Flows</a>
     */
    @JvmStatic
    @JvmOverloads
    public fun getFlowConfiguration(
        flow: AdaptyFlow,
        locale: String? = null,
        loadTimeout: TimeInterval = defaultFlowLoadTimeout,
        customLayoutId: String? = null,
        callback: ResultCallback<FlowConfiguration>
    ) {
        loadFlowConfiguration(flow, locale, loadTimeout, customLayoutId, callback)
    }

    @Deprecated("Starting Adapty SDK 4.0.0, Onboarding Feature is deprecated. Please consider migrating to Flows")
    @JvmStatic
    @JvmOverloads
    public fun getOnboardingConfiguration(
        onboarding: AdaptyOnboarding,
        externalUrlsPresentation: AdaptyWebPresentation = AdaptyWebPresentation.InAppBrowser,
    ): AdaptyOnboardingConfiguration {
        return createOnboardingConfiguration(onboarding, externalUrlsPresentation)
    }

    // Invoked reflectively by core `AdaptyUiAccessor.preloadMedia` (name and signature must stay).
    private fun preloadMedia(rawConfig: Map<String, Any>) {
        preloadMediaPlatform(rawConfig)
    }

    public sealed class Action {
        public object Close : Action()
        public class OpenUrl(public val url: String, public val presentation: AdaptyWebPresentation = AdaptyWebPresentation.ExternalBrowser) : Action()
        public class Custom(public val customId: String): Action()
    }

    public class FlowConfiguration internal constructor(
        @get:JvmSynthetic internal val id: String,
        @get:JvmSynthetic internal val mode: FlowMode,
        public val isHard: Boolean,
        @get:JvmSynthetic internal val isRtl: Boolean,
        @get:JvmSynthetic internal val locale: PlatformLocale,
        @get:JvmSynthetic internal val localizationId: String? = null,
        @get:JvmSynthetic internal val assets: Map<String, Asset>,
        @get:JvmSynthetic internal val texts: Map<String, TextItem>,
        @get:JvmSynthetic internal val screens: ScreenBundle,
        @get:JvmSynthetic internal val navigators: Map<String, NavigatorConfig>,
        @get:JvmSynthetic internal val initialScript: String,
        @get:JvmSynthetic internal val showPurchaseLoader: Boolean,
        @get:JvmSynthetic internal val showRestoreLoader: Boolean,
        @get:JvmSynthetic internal val isLegacyFormat: Boolean = false,
    ) {
        @get:JvmSynthetic internal val runtimeState: FlowRuntimeState = FlowRuntimeState()

        public fun prepareForReuse() {
            runtimeState.clear()
            val handler = stateHandlerOrNull()
            if (handler != null && handler.stateOwner === this) handler.stateOwner = null
        }

        /**
         * @suppress
         */
        public sealed class Asset(internal val customId: String?) {

            public class Composite<T: Asset>(
                public val main: T,
                public val fallback: T? = null,
            ) {
                internal inline fun <reified T: Asset> cast(): Composite<T> = this as Composite<T>
                internal inline fun <reified T: Asset> castOrNull(): Composite<T>? = if (this.main is T) cast() else null
            }

            public class Color internal constructor(
                @ColorInt internal val value: Int,
                customId: String? = null,
            ): Filling.Local(customId)

            public class Gradient internal constructor(
                internal val type: Type,
                internal val values: List<Value>,
                internal val points: Points,
                customId: String? = null,
            ): Filling.Local(customId) {

                internal enum class Type { LINEAR, RADIAL, CONIC }

                internal class Value(
                    val p: Float,
                    val color: Color,
                ) {
                    operator fun component1() = p
                    operator fun component2() = color
                }

                internal class Points(
                    val x0: Float,
                    val y0: Float,
                    val x1: Float,
                    val y1: Float,
                ) {
                    operator fun component1(): Float = x0
                    operator fun component2(): Float = y0
                    operator fun component3(): Float = x1
                    operator fun component4(): Float = y1
                }
            }

            public class Font internal constructor(
                internal val familyName: String,
                internal val resources: List<String>,
                internal val weight: Int,
                internal val isItalic: Boolean,
                internal val size: Float,
                @ColorInt internal val color: Int?,
                internal val letterSpacing: Float?,
                internal val lineHeight: Float?,
                customId: String? = null,
            ): Asset(customId)

            public class Image internal constructor(
                internal val source: Source,
                customId: String? = null,
            ): Filling.Local(customId) {

                public sealed class Source {
                    public class Uri internal constructor(internal val uri: com.adapty.ui.internal.platform.Uri): Source()
                    public class Base64Str internal constructor(internal val imageBase64: String?): Source()
                    public class AndroidAsset internal constructor(internal val path: String): Source()
                    public class Bitmap internal constructor(internal val bitmap: com.adapty.ui.internal.platform.Bitmap): Source()
                }

                internal enum class Dimension { WIDTH, HEIGHT }

                internal enum class ScaleType { FIT_MIN, FIT_MAX }
            }

            public class RemoteImage internal constructor(
                internal val url: String,
                internal val preview: Image?,
                customId: String? = null,
            ): Filling(customId)

            public class Video internal constructor(
                public val source: Source,
                internal val vRes: Int = 0,
                internal val hRes: Int = 0,
                customId: String? = null,
            ): Filling(customId) {
                internal val ratio: Float?
                    get() = if (hRes > 0 && vRes > 0) hRes.toFloat() / vRes.toFloat() else null

                public sealed class Source {
                    public class Uri internal constructor(public val uri: com.adapty.ui.internal.platform.Uri): Source() {
                        override fun equals(other: Any?): Boolean {
                            if (this === other) return true
                            if (other == null || this::class != other::class) return false

                            other as Uri

                            return uri == other.uri
                        }

                        override fun hashCode(): Int {
                            return uri.hashCode()
                        }
                    }
                    public class AndroidAsset(public val path: String): Source() {
                        override fun equals(other: Any?): Boolean {
                            if (this === other) return true
                            if (other == null || this::class != other::class) return false

                            other as AndroidAsset

                            return path == other.path
                        }

                        override fun hashCode(): Int {
                            return path.hashCode()
                        }
                    }
                }
            }

            public sealed class Filling(customId: String?): Asset(customId) {
                public sealed class Local(customId: String?): Filling(customId)
            }

            internal class Unknown internal constructor(
                internal val fallbackAssetId: String?,
                customId: String? = null,
            ): Asset(customId)
        }

        /**
         * @suppress
         */
        @InternalAdaptyUiApi
        public class TextItem internal constructor(
            internal val value: RichText,
            internal val fallback: RichText?,
        )

        /**
         * @suppress
         */
        @InternalAdaptyUiApi
        public class RichText internal constructor(internal val items: List<Item>) {
            internal constructor(item: Item): this(listOf(item))

            public sealed class Item(internal val attrs: Attributes?, internal val actions: List<com.adapty.ui.internal.ui.element.Action> = emptyList()) {
                public class Text internal constructor(internal val text: String, attrs: Attributes?, actions: List<com.adapty.ui.internal.ui.element.Action> = emptyList()): Item(attrs, actions)
                public class Image internal constructor(internal val imageAssetId: String, attrs: Attributes?, actions: List<com.adapty.ui.internal.ui.element.Action> = emptyList()): Item(attrs, actions)
                public class Tag internal constructor(internal val tag: String, attrs: Attributes?, actions: List<com.adapty.ui.internal.ui.element.Action> = emptyList(), internal val converterName: String? = null, internal val converterParams: Map<String, Any?>? = null): Item(attrs, actions)
            }

            internal class Attributes constructor(
                val fontAssetId: VisualValue?,
                val size: Float?,
                val strikethrough: Boolean,
                val underline: Boolean,
                val textColor: VisualValue?,
                val background: VisualValue?,
                val imageTint: VisualValue?,
                val letterSpacing: Float? = null,
            )
        }

        internal class ScreenBundle(
            val screens: Map<String, Screen>,
            val initialState: Map<String, Any>,
        )

        internal data class NavigatorConfig(
            val background: VisualValue,
            val content: UIElement,
            val order: Int,
            val overlays: List<com.adapty.ui.internal.ui.element.OverlayItem> = emptyList(),
            val onOutsideTap: List<com.adapty.ui.internal.ui.element.Action> = emptyList(),
            val onSystemBack: List<com.adapty.ui.internal.ui.element.Action>? = null,
            val onFocusChange: List<com.adapty.ui.internal.ui.element.Action>? = null,
            val onWillAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
            val onDidAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
            val onWillDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
            val onDidDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
            val appearance: Map<String, com.adapty.ui.internal.ui.attributes.AppearanceAnimation> = emptyMap(),
            val transitions: Map<String, com.adapty.ui.internal.ui.attributes.ScreenTransition> = emptyMap(),
        )

        /**
         * @suppress
         */
        @InternalAdaptyUiApi
        public sealed class Screen(
            internal val onSystemBack: List<com.adapty.ui.internal.ui.element.Action>? = null,
            internal val onOutsideTap: List<com.adapty.ui.internal.ui.element.Action>? = null,
            internal val onFocusChange: List<com.adapty.ui.internal.ui.element.Action>? = null,
            internal val onWillAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
            internal val onDidAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
            internal val onWillDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
            internal val onDidDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
            internal val contentScrollValue: com.adapty.ui.internal.utils.TwoWayBinding? = null,
            internal val footerScrollValue: com.adapty.ui.internal.utils.TwoWayBinding? = null,
            internal val overlays: List<com.adapty.ui.internal.ui.element.OverlayItem> = emptyList(),
            internal val backgrounds: List<com.adapty.ui.internal.ui.element.OverlayItem> = emptyList(),
        ) {
            public class Plain internal constructor(
                internal val content: UIElement,
                onSystemBack: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onOutsideTap: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onFocusChange: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onWillAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onDidAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onWillDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onDidDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                contentScrollValue: com.adapty.ui.internal.utils.TwoWayBinding? = null,
                footerScrollValue: com.adapty.ui.internal.utils.TwoWayBinding? = null,
                overlays: List<com.adapty.ui.internal.ui.element.OverlayItem> = emptyList(),
                backgrounds: List<com.adapty.ui.internal.ui.element.OverlayItem> = emptyList(),
            ): Screen(onSystemBack, onOutsideTap, onFocusChange, onWillAppear, onDidAppear, onWillDisappear, onDidDisappear, contentScrollValue, footerScrollValue, overlays, backgrounds)
            public class Hero internal constructor(
                internal val cover: BoxElement,
                internal val contentWrapper: ContentWrapper,
                internal val footer: UIElement?,
                onSystemBack: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onOutsideTap: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onFocusChange: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onWillAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onDidAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onWillDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onDidDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                contentScrollValue: com.adapty.ui.internal.utils.TwoWayBinding? = null,
                footerScrollValue: com.adapty.ui.internal.utils.TwoWayBinding? = null,
                overlays: List<com.adapty.ui.internal.ui.element.OverlayItem> = emptyList(),
                backgrounds: List<com.adapty.ui.internal.ui.element.OverlayItem> = emptyList(),
            ): Screen(onSystemBack, onOutsideTap, onFocusChange, onWillAppear, onDidAppear, onWillDisappear, onDidDisappear, contentScrollValue, footerScrollValue, overlays, backgrounds)
            public class Transparent internal constructor(
                internal val cover: BoxElement?,
                internal val content: UIElement,
                internal val footer: UIElement?,
                onSystemBack: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onOutsideTap: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onFocusChange: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onWillAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onDidAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onWillDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onDidDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                contentScrollValue: com.adapty.ui.internal.utils.TwoWayBinding? = null,
                footerScrollValue: com.adapty.ui.internal.utils.TwoWayBinding? = null,
                overlays: List<com.adapty.ui.internal.ui.element.OverlayItem> = emptyList(),
                backgrounds: List<com.adapty.ui.internal.ui.element.OverlayItem> = emptyList(),
            ): Screen(onSystemBack, onOutsideTap, onFocusChange, onWillAppear, onDidAppear, onWillDisappear, onDidDisappear, contentScrollValue, footerScrollValue, overlays, backgrounds)
            public class Flat internal constructor(
                internal val cover: BoxElement?,
                internal val contentWrapper: ContentWrapper,
                internal val footer: UIElement?,
                onSystemBack: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onOutsideTap: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onFocusChange: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onWillAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onDidAppear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onWillDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                onDidDisappear: List<com.adapty.ui.internal.ui.element.Action>? = null,
                contentScrollValue: com.adapty.ui.internal.utils.TwoWayBinding? = null,
                footerScrollValue: com.adapty.ui.internal.utils.TwoWayBinding? = null,
                overlays: List<com.adapty.ui.internal.ui.element.OverlayItem> = emptyList(),
                backgrounds: List<com.adapty.ui.internal.ui.element.OverlayItem> = emptyList(),
            ): Screen(onSystemBack, onOutsideTap, onFocusChange, onWillAppear, onDidAppear, onWillDisappear, onDidDisappear, contentScrollValue, footerScrollValue, overlays, backgrounds)
        }
    }

    @JvmStatic
    public fun configureMediaCache(config: MediaCacheConfiguration) {
        configureMediaCachePlatform(config)
    }

    public class MediaCacheConfiguration private constructor(
        public val diskStorageSizeLimit: Long,
        public val diskCacheValidityTime: TimeInterval,
    ) {

        private companion object {
            private const val DEFAULT_DISK_STORAGE_SIZE_LIMIT_BYTES = 100L * 1024 * 1024
            private val DEFAULT_DISK_CACHE_VALIDITY_TIME = 7.days
        }

        public class Builder {

            private var diskStorageSizeLimit: Long = DEFAULT_DISK_STORAGE_SIZE_LIMIT_BYTES

            private var diskCacheValidityTime: TimeInterval = DEFAULT_DISK_CACHE_VALIDITY_TIME

            public fun overrideDiskStorageSizeLimit(limitInBytes: Long): Builder {
                diskStorageSizeLimit = limitInBytes
                return this
            }

            public fun overrideDiskCacheValidityTime(time: TimeInterval): Builder {
                diskCacheValidityTime = time
                return this
            }

            public fun build(): MediaCacheConfiguration =
                MediaCacheConfiguration(diskStorageSizeLimit, diskCacheValidityTime)
        }
    }

    @JvmStatic
    public fun clearMediaCache(strategy: ClearCacheStrategy) {
        clearMediaCachePlatform(strategy)
    }

    public enum class ClearCacheStrategy {
        CLEAR_ALL,
        CLEAR_EXPIRED_ONLY
    }

    init {
        initAdaptyUIPlatform()
    }
}
