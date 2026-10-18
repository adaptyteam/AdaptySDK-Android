@file:OptIn(InternalAdaptyUiApi::class, com.adapty.internal.utils.InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")

package com.adapty.ui.internal

import android.view.View
import android.view.ViewGroup.LayoutParams
import com.adapty.Adapty
import com.adapty.internal.data.models.DeviceInfo
import com.adapty.internal.di.Dependencies
import com.adapty.internal.di.Dependencies.inject
import com.adapty.internal.utils.DEFAULT_PLACEMENT_TIMEOUT
import com.adapty.internal.utils.HashingHelper
import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.AdaptyFlowView
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.ClearCacheStrategy
import com.adapty.ui.AdaptyUI.FlowConfiguration
import com.adapty.ui.AdaptyUI.MediaCacheConfiguration
import com.adapty.ui.internal.cache.CacheCleanupService
import com.adapty.ui.internal.cache.CacheFileManager
import com.adapty.ui.internal.cache.MediaCacheConfigManager
import com.adapty.ui.internal.cache.MediaDownloader
import com.adapty.ui.internal.cache.MediaFetchService
import com.adapty.ui.internal.cache.MediaSaver
import com.adapty.ui.internal.cache.SingleMediaHandlerFactory
import com.adapty.ui.internal.mapping.viewconfig.ViewConfigurationMapper
import com.adapty.ui.internal.platform.Activity
import com.adapty.ui.internal.platform.AdaptyFlow
import com.adapty.ui.internal.platform.AdaptyOnboarding
import com.adapty.ui.internal.platform.AdaptyPaywallProduct
import com.adapty.ui.internal.platform.AdaptyWebPresentation
import com.adapty.ui.internal.platform.Context
import com.adapty.ui.internal.platform.ERROR
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.ResultCallback
import com.adapty.ui.internal.platform.TimeInterval
import com.adapty.ui.internal.platform.VERBOSE
import com.adapty.ui.internal.script.JSActionBridge
import com.adapty.ui.internal.script.JSStateHandler
import com.adapty.ui.internal.script.JSStateMachine
import com.adapty.ui.internal.script.PlatformJson
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.internal.utils.AdaptyUiVideoAccessor
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
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
import com.google.gson.Gson

// Android bodies of AdaptyUI (core SDK DI, views, media cache); AdaptyUI calls them through these seams.

internal val defaultFlowLoadTimeout: TimeInterval get() = DEFAULT_PLACEMENT_TIMEOUT

internal fun initAdaptyUIPlatform() {
    initAllDeps()
}

private fun initAllDeps() {
    val adaptyUiVideoAccessor = AdaptyUiVideoAccessor()
    Dependencies.contribute(
        setOf(
            ViewConfigurationMapper::class to Dependencies.singleVariantDiObject({
                val videoMapperFn = adaptyUiVideoAccessor.createVideoMapperFnOrNull()
                ViewConfigurationMapper.createDefault(videoMapperFn)
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
    PlatformJson.gson = Dependencies.injectInternal<Gson>("base")
    val jsEngine = JSStateMachine(
        PlatformContextHandle(appContext),
        JSActionBridge(),
    )
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
            StateHandler::class to Dependencies.singleVariantDiObject({
                JSStateHandler(jsEngine)
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

/** Throws (NPE) when AdaptyUI deps aren't contributed yet; wrap with `withAdaptyUIActivated`. */
internal fun injectStateHandler(): StateHandler = Dependencies.injectInternal<StateHandler>()

internal fun stateHandlerOrNull(): StateHandler? =
    runCatching { Dependencies.injectInternal<StateHandler>() }.getOrNull()

internal fun createFlowView(
    activity: Activity,
    flowConfiguration: FlowConfiguration,
    products: List<AdaptyPaywallProduct>?,
    eventListener: AdaptyFlowEventListener,
    insets: AdaptyFlowInsets,
    customAssets: AdaptyCustomAssets,
    tagResolver: AdaptyUiTagResolver,
    timerResolver: AdaptyUiTimerResolver,
    observerModeHandler: AdaptyUiObserverModeHandler?,
): AdaptyFlowView {
    return AdaptyFlowView(activity).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        showFlow(
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
}

internal fun createOnboardingView(
    activity: Activity,
    viewConfig: AdaptyOnboardingConfiguration,
    eventListener: AdaptyOnboardingEventListener,
    safeAreaPaddings: Boolean,
): AdaptyOnboardingView {
    return AdaptyOnboardingView(activity).apply {
        id = View.generateViewId()
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        show(viewConfig, eventListener, safeAreaPaddings)
    }
}

internal fun createOnboardingConfiguration(
    onboarding: AdaptyOnboarding,
    externalUrlsPresentation: AdaptyWebPresentation,
): AdaptyOnboardingConfiguration {
    return AdaptyOnboardingConfiguration(onboarding, externalUrlsPresentation)
}

internal fun loadFlowConfiguration(
    flow: AdaptyFlow,
    locale: String?,
    loadTimeout: TimeInterval,
    customLayoutId: String?,
    callback: ResultCallback<FlowConfiguration>,
) {
    AdaptyUI.getFlowConfigurationInternal(flow, locale, loadTimeout, customLayoutId, deviceInfoOverride = null, callback = callback)
}

@JvmSynthetic
internal fun AdaptyUI.getFlowConfigurationInternal(
    flow: AdaptyFlow,
    locale: String?,
    loadTimeout: TimeInterval,
    customLayoutId: String?,
    deviceInfoOverride: DeviceInfo?,
    callback: ResultCallback<FlowConfiguration>,
) {
    Adapty.getFlowViewConfiguration(
        flow,
        locale,
        loadTimeout,
        customLayoutId,
        deviceInfoOverride,
        transform = { rawConfig -> viewConfigMapper.map(rawConfig, flow) },
        callback = callback,
    )
}

internal fun preloadMediaPlatform(rawConfig: Map<String, Any>) {
    runCatching {
        val (configId, urls) = viewConfigMapper.mapToMediaUrls(rawConfig)
        mediaFetchService.preloadMedia(configId, urls)
    }
}

internal fun configureMediaCachePlatform(config: MediaCacheConfiguration) {
    log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# configure: diskStorageSizeLimit = ${config.diskStorageSizeLimit}, diskCacheValidityTime = ${config.diskCacheValidityTime}" }
    val cacheConfigManager = runCatching { Dependencies.injectInternal<MediaCacheConfigManager>() }.getOrNull() ?: run {
        log(ERROR) { "$LOG_PREFIX #AdaptyMediaCache# couldn't be configured. Adapty was not initialized" }
        return
    }
    cacheConfigManager.currentCacheConfig = config
}

internal fun clearMediaCachePlatform(strategy: ClearCacheStrategy) {
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
