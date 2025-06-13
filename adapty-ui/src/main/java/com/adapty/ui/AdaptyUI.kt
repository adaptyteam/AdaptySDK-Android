@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import com.adapty.Adapty
import com.adapty.internal.di.Dependencies
import com.adapty.internal.di.Dependencies.inject
import com.adapty.internal.utils.DEFAULT_PAYWALL_TIMEOUT
import com.adapty.internal.utils.HashingHelper
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyOnboarding
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.internal.cache.CacheCleanupService
import com.adapty.ui.internal.cache.CacheFileManager
import com.adapty.ui.internal.cache.MediaCacheConfigManager
import com.adapty.ui.internal.cache.MediaDownloader
import com.adapty.ui.internal.cache.MediaFetchService
import com.adapty.ui.internal.cache.MediaSaver
import com.adapty.ui.internal.cache.SingleMediaHandlerFactory
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.mapping.attributes.InteractiveAttributeMapper
import com.adapty.ui.internal.mapping.attributes.PagerAttributeMapper
import com.adapty.ui.internal.mapping.attributes.TextAttributeMapper
import com.adapty.ui.internal.mapping.element.BoxElementMapper
import com.adapty.ui.internal.mapping.element.ButtonElementMapper
import com.adapty.ui.internal.mapping.element.ColumnElementMapper
import com.adapty.ui.internal.mapping.element.HStackElementMapper
import com.adapty.ui.internal.mapping.element.IfElementMapper
import com.adapty.ui.internal.mapping.element.ImageElementMapper
import com.adapty.ui.internal.mapping.element.PagerElementMapper
import com.adapty.ui.internal.mapping.element.ReferenceElementMapper
import com.adapty.ui.internal.mapping.element.RowElementMapper
import com.adapty.ui.internal.mapping.element.SectionElementMapper
import com.adapty.ui.internal.mapping.element.SpaceElementMapper
import com.adapty.ui.internal.mapping.element.TextElementMapper
import com.adapty.ui.internal.mapping.element.TimerElementMapper
import com.adapty.ui.internal.mapping.element.ToggleElementMapper
import com.adapty.ui.internal.mapping.element.UIElementFactory
import com.adapty.ui.internal.mapping.element.UIElementMapper
import com.adapty.ui.internal.mapping.element.VStackElementMapper
import com.adapty.ui.internal.mapping.element.ZStackElementMapper
import com.adapty.ui.internal.mapping.viewconfig.ViewConfigurationAssetMapper
import com.adapty.ui.internal.mapping.viewconfig.ViewConfigurationMapper
import com.adapty.ui.internal.mapping.viewconfig.ViewConfigurationScreenMapper
import com.adapty.ui.internal.mapping.viewconfig.ViewConfigurationTextMapper
import com.adapty.ui.internal.ui.element.BoxElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.utils.AdaptyUiVideoAccessor
import com.adapty.ui.internal.utils.ContentWrapper
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.ui.listeners.AdaptyUiDefaultEventListener
import com.adapty.ui.listeners.AdaptyUiEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiPersonalizedOfferResolver
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import com.adapty.ui.onboardings.AdaptyOnboardingConfiguration
import com.adapty.ui.onboardings.AdaptyOnboardingView
import com.adapty.ui.onboardings.internal.serialization.MetaParamsParser
import com.adapty.ui.onboardings.internal.serialization.OnboardingActionsParser
import com.adapty.ui.onboardings.internal.serialization.OnboardingCommonDeserializer
import com.adapty.ui.onboardings.internal.serialization.OnboardingCommonEventParser
import com.adapty.ui.onboardings.internal.serialization.OnboardingEventsParser
import com.adapty.ui.onboardings.internal.serialization.OnboardingStateUpdatedParamsParser
import com.adapty.ui.onboardings.listeners.AdaptyOnboardingEventListener
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.ResultCallback
import com.adapty.utils.TimeInterval
import com.adapty.utils.days

public object AdaptyUI {

    /**
     * Right after receiving [LocalizedViewConfiguration], you can create the corresponding
     * [AdaptyPaywallView] to display it afterwards.
     *
     * This method should be called only on UI thread.
     *
     * @param[activity] An [Activity] instance.
     *
     * @param[viewConfiguration] A [LocalizedViewConfiguration] object containing information
     * about the visual part of the paywall. To load it, use the [AdaptyUI.getViewConfiguration] method.
     *
     * @param[products] Optional [AdaptyPaywallProduct] list. Pass this value in order to optimize
     * the display time of the products on the screen. If you pass `null`, `AdaptyUI` will
     * automatically fetch the required products.
     *
     * @param[eventListener] An object that implements the [AdaptyUiEventListener] interface.
     * Use it to respond to different events happening inside the purchase screen.
     * Also you can extend [AdaptyUiDefaultEventListener] so you don't need to override all the methods.
     *
     * @param[insets] You can override the default window inset handling by specifying the [AdaptyPaywallInsets].
     *
     * @param[personalizedOfferResolver] In case you want to indicate whether the price is personalized ([read more](https://developer.android.com/google/play/billing/integrate#personalized-price)),
     * you can implement [AdaptyUiPersonalizedOfferResolver] and pass your own logic
     * that maps [AdaptyPaywallProduct] to `true`, if the price of the product is personalized, otherwise `false`.
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
     * @return An [AdaptyPaywallView] object, representing the requested paywall screen.
     */
    @JvmStatic
    @JvmOverloads
    @UiThread
    public fun getPaywallView(
        activity: Activity,
        viewConfiguration: LocalizedViewConfiguration,
        products: List<AdaptyPaywallProduct>?,
        eventListener: AdaptyUiEventListener,
        insets: AdaptyPaywallInsets = AdaptyPaywallInsets.UNSPECIFIED,
        personalizedOfferResolver: AdaptyUiPersonalizedOfferResolver = AdaptyUiPersonalizedOfferResolver.DEFAULT,
        customAssets: AdaptyCustomAssets = AdaptyCustomAssets.Empty,
        tagResolver: AdaptyUiTagResolver = AdaptyUiTagResolver.DEFAULT,
        timerResolver: AdaptyUiTimerResolver = AdaptyUiTimerResolver.DEFAULT,
        observerModeHandler: AdaptyUiObserverModeHandler? = null,
    ): AdaptyPaywallView {
        return AdaptyPaywallView(activity).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            showPaywall(
                viewConfiguration,
                products,
                eventListener,
                insets,
                personalizedOfferResolver,
                customAssets,
                tagResolver,
                timerResolver,
                observerModeHandler,
            )
        }
    }

    @JvmStatic
    @UiThread
    public fun getOnboardingView(
        activity: Activity,
        viewConfig: AdaptyOnboardingConfiguration,
        eventListener: AdaptyOnboardingEventListener,
    ): AdaptyOnboardingView {
        return AdaptyOnboardingView(activity).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            show(viewConfig, eventListener)
        }
    }

    /**
     * If you are using the [Paywall Builder](https://adapty.io/docs/adapty-paywall-builder),
     * you can use this method to get a configuration object for your paywall.
     *
     * Should not be called before [Adapty.activate]
     *
     * @param[paywall] The [AdaptyPaywall] for which you want to get a configuration.
     *
     * @param[loadTimeout] This value limits the timeout for this method. The minimum value is 1 second.
     * If a timeout is not required, you can pass [TimeInterval.INFINITE].
     *
     * @param[callback] A result containing the [LocalizedViewConfiguration] object.
     *
     * @see <a href="https://adapty.io/docs/display-pb-paywalls">Display paywalls designed with Paywall Builder</a>
     */
    @JvmStatic
    @JvmOverloads
    public fun getViewConfiguration(
        paywall: AdaptyPaywall,
        loadTimeout: TimeInterval = DEFAULT_PAYWALL_TIMEOUT,
        callback: ResultCallback<LocalizedViewConfiguration>
    ) {
        Adapty.getViewConfiguration(paywall, loadTimeout) { result ->
            callback.onResult(result.map { rawConfig ->
                viewConfigMapper.map(rawConfig, paywall)
            })
        }
    }

    @JvmStatic
    public fun getOnboardingConfiguration(onboarding: AdaptyOnboarding): AdaptyOnboardingConfiguration {
        return AdaptyOnboardingConfiguration(onboarding)
    }

    private fun preloadMedia(rawConfig: Map<String, Any>) {
        runCatching {
            val (configId, urls) = viewConfigMapper.mapToMediaUrls(rawConfig)
            mediaFetchService.preloadMedia(configId, urls)
        }
    }

    public sealed class Action {
        public object Close : Action()
        public class OpenUrl(public val url: String) : Action()
        public class Custom(public val customId: String): Action()
    }

    public class LocalizedViewConfiguration internal constructor(
        @get:JvmSynthetic internal val id: String,
        @get:JvmSynthetic internal val paywall: AdaptyPaywall,
        public val isHard: Boolean,
        @get:JvmSynthetic internal val isRtl: Boolean,
        @get:JvmSynthetic internal val assets: Map<String, Asset>,
        @get:JvmSynthetic internal val texts: Map<String, TextItem>,
        @get:JvmSynthetic internal val screens: ScreenBundle,
    ) {
        /**
         * @suppress
         */
        public sealed class Asset(internal val customId: String?) {

            public class Composite<T: Asset>(
                public val main: T,
                public val fallback: T? = null,
            ) {
                internal inline fun <reified T: Asset> cast(): Composite<T> = this as Composite<T>
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
                customId: String? = null,
            ): Asset(customId)

            public class Image internal constructor(
                internal val source: Source,
                customId: String? = null,
            ): Filling.Local(customId) {

                public sealed class Source {
                    public class Uri internal constructor(internal val uri: android.net.Uri): Source()
                    public class Base64Str internal constructor(internal val imageBase64: String?): Source()
                    public class AndroidAsset internal constructor(internal val path: String): Source()
                    public class Bitmap internal constructor(internal val bitmap: android.graphics.Bitmap): Source()
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
                customId: String? = null,
            ): Filling(customId) {
                public sealed class Source {
                    public class Uri internal constructor(public val uri: android.net.Uri): Source() {
                        override fun equals(other: Any?): Boolean {
                            if (this === other) return true
                            if (javaClass != other?.javaClass) return false

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
                            if (javaClass != other?.javaClass) return false

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
        }

        internal class TextItem(
            val value: RichText,
            val fallback: RichText?,
        )

        /**
         * @suppress
         */
        @InternalAdaptyApi
        public class RichText internal constructor(internal val items: List<Item>) {
            internal constructor(item: Item): this(listOf(item))

            public sealed class Item(internal val attrs: Attributes?) {
                public class Text internal constructor(internal val text: String, attrs: Attributes?): Item(attrs)
                public class Image internal constructor(internal val imageAssetId: String, attrs: Attributes?): Item(attrs)
                public class Tag internal constructor(internal val tag: String, attrs: Attributes?): Item(attrs)
            }

            internal class Attributes(
                val fontAssetId: String?,
                val size: Float?,
                val strikethrough: Boolean,
                val underline: Boolean,
                val textColorAssetId: String?,
                val backgroundAssetId: String?,
                val imageTintAssetId: String?,
            )
        }

        internal class ScreenBundle(
            val defaultScreen: Screen.Default,
            val bottomSheets: Map<String, Screen.BottomSheet>,
            val initialState: Map<String, Any>,
        )

        /**
         * @suppress
         */
        @InternalAdaptyApi
        public sealed class Screen {
            public sealed class Default(
                internal val background: String,
                internal open val cover: BoxElement?,
                internal val footer: UIElement?,
                internal val overlay: UIElement?,
            ): Screen() {
                public class Basic internal constructor(
                    background: String,
                    override val cover: BoxElement,
                    internal val contentWrapper: ContentWrapper,
                    footer: UIElement?,
                    overlay: UIElement?,
                ): Default(background, cover, footer, overlay)
                public class Transparent internal constructor(
                    background: String,
                    cover: BoxElement?,
                    internal val content: UIElement,
                    footer: UIElement?,
                    overlay: UIElement?,
                ): Default(background, cover, footer, overlay)
                public class Flat internal constructor(
                    background: String,
                    cover: BoxElement?,
                    internal val contentWrapper: ContentWrapper,
                    footer: UIElement?,
                    overlay: UIElement?,
                ): Default(background, cover, footer, overlay)
            }
            public class BottomSheet internal constructor(internal val content: UIElement): Screen()
        }
    }

    @JvmStatic
    public fun configureMediaCache(config: MediaCacheConfiguration) {
        log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# configure: diskStorageSizeLimit = ${config.diskStorageSizeLimit}, diskCacheValidityTime = ${config.diskCacheValidityTime}" }
        val cacheConfigManager = runCatching { Dependencies.injectInternal<MediaCacheConfigManager>() }.getOrNull() ?: run {
            log(ERROR) { "$LOG_PREFIX #AdaptyMediaCache# couldn't be configured. Adapty was not initialized" }
            return
        }
        cacheConfigManager.currentCacheConfig = config
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
        log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# clear: ${strategy.name}" }
        val cacheCleanupService = runCatching { Dependencies.injectInternal<CacheCleanupService>() }.getOrNull() ?: run {
            log(ERROR) { "$LOG_PREFIX #AdaptyMediaCache# couldn't clear cache. Adapty was not initialized" }
            return
        }

        when (strategy) {
            ClearCacheStrategy.CLEAR_ALL -> cacheCleanupService.clearAll()
            ClearCacheStrategy.CLEAR_EXPIRED_ONLY -> cacheCleanupService.clearExpired()
        }
    }

    public enum class ClearCacheStrategy {
        CLEAR_ALL,
        CLEAR_EXPIRED_ONLY
    }

    init {
        initAllDeps()
    }

    private fun initAllDeps() {
        val adaptyUiVideoAccessor = AdaptyUiVideoAccessor()
        Dependencies.contribute(
            setOf(
                ViewConfigurationMapper::class to Dependencies.singleVariantDiObject({
                    val commonAttributeMapper = CommonAttributeMapper()
                    val textAttributeMapper = TextAttributeMapper()
                    val interactiveAttributeMapper = InteractiveAttributeMapper()
                    val videoElementMapper =
                        adaptyUiVideoAccessor.createVideoElementMapperOrNull(commonAttributeMapper)
                    ViewConfigurationMapper(
                        ViewConfigurationAssetMapper(),
                        ViewConfigurationTextMapper(),
                        ViewConfigurationScreenMapper(
                            UIElementFactory(
                                mutableListOf<UIElementMapper>(
                                    BoxElementMapper(commonAttributeMapper),
                                    ButtonElementMapper(
                                        interactiveAttributeMapper,
                                        commonAttributeMapper,
                                    ),
                                    ColumnElementMapper(commonAttributeMapper),
                                    HStackElementMapper(commonAttributeMapper),
                                    IfElementMapper(
                                        commonAttributeMapper,
                                        videoElementMapper != null,
                                    ),
                                    ImageElementMapper(commonAttributeMapper),
                                    PagerElementMapper(
                                        PagerAttributeMapper(commonAttributeMapper),
                                        commonAttributeMapper,
                                    ),
                                    ReferenceElementMapper(commonAttributeMapper),
                                    RowElementMapper(commonAttributeMapper),
                                    SectionElementMapper(commonAttributeMapper),
                                    SpaceElementMapper(commonAttributeMapper),
                                    TextElementMapper(textAttributeMapper, commonAttributeMapper),
                                    TimerElementMapper(
                                        textAttributeMapper,
                                        interactiveAttributeMapper,
                                        commonAttributeMapper,
                                    ),
                                    ToggleElementMapper(
                                        interactiveAttributeMapper,
                                        commonAttributeMapper,
                                    ),
                                    VStackElementMapper(commonAttributeMapper),
                                    ZStackElementMapper(commonAttributeMapper),
                                )
                                    .apply {
                                        if (videoElementMapper != null)
                                            add(videoElementMapper)
                                    }
                            ),
                            commonAttributeMapper,
                        )
                    )
                }),
                MediaCacheConfigManager::class to Dependencies.singleVariantDiObject({
                    MediaCacheConfigManager()
                }),
                AdaptyUiVideoAccessor::class to Dependencies.singleVariantDiObject({
                    adaptyUiVideoAccessor
                }),
                OnboardingCommonDeserializer::class to Dependencies.singleVariantDiObject({
                    val metaParamsParser = MetaParamsParser()
                    val onboardingStateUpdatedParamsParser = OnboardingStateUpdatedParamsParser()
                    val onboardingEventsParser = OnboardingEventsParser(metaParamsParser)
                    val onboardingActionsParser = OnboardingActionsParser(metaParamsParser, onboardingStateUpdatedParamsParser)
                    val onboardingCommonEventParser = OnboardingCommonEventParser(onboardingEventsParser)
                    OnboardingCommonDeserializer(
                        onboardingActionsParser, onboardingCommonEventParser,
                    )
                }),
            )
        )

        val appContext = runCatching { Dependencies.injectInternal<Context>() }.getOrNull()

        if (appContext == null) {
            Dependencies.onInitialDepsCreated = {
                contributeDepsOnAdaptyReady(Dependencies.injectInternal<Context>())
            }
        } else {
            contributeDepsOnAdaptyReady(appContext)
        }
    }

    private fun contributeDepsOnAdaptyReady(appContext: Context) {
        Dependencies.contribute(
            listOf(
                CacheFileManager::class to Dependencies.singleVariantDiObject({
                    CacheFileManager(appContext, Dependencies.injectInternal<HashingHelper>())
                }),
                CacheCleanupService::class to Dependencies.singleVariantDiObject({
                    CacheCleanupService(
                        Dependencies.injectInternal<CacheFileManager>(),
                        Dependencies.injectInternal<MediaCacheConfigManager>(),
                    )
                }),
                MediaFetchService::class to Dependencies.singleVariantDiObject({
                    val cacheFileManager = Dependencies.injectInternal<CacheFileManager>()
                    val mediaDownloader = MediaDownloader()
                    val mediaSaver = MediaSaver(cacheFileManager)
                    val singleMediaHandlerFactory =
                        SingleMediaHandlerFactory(
                            mediaDownloader,
                            mediaSaver,
                            cacheFileManager,
                            Dependencies.injectInternal<CacheCleanupService>(),
                        )
                    MediaFetchService(singleMediaHandlerFactory)
                }),
            )
        )
        val adaptyUiVideoAccessor = Dependencies.injectInternal<AdaptyUiVideoAccessor>()
        adaptyUiVideoAccessor.provideDeps(appContext)?.let { deps ->
            Dependencies.contribute(deps)
        }
    }

    private val viewConfigMapper: ViewConfigurationMapper by inject()

    private val mediaFetchService: MediaFetchService by inject()
}