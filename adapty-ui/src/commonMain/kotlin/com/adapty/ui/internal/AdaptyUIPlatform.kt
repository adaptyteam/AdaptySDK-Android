@file:OptIn(InternalAdaptyUiApi::class)
@file:Suppress("DEPRECATION")

package com.adapty.ui.internal

import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.AdaptyFlowView
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.ClearCacheStrategy
import com.adapty.ui.AdaptyUI.FlowConfiguration
import com.adapty.ui.AdaptyUI.MediaCacheConfiguration
import com.adapty.ui.internal.platform.Activity
import com.adapty.ui.internal.platform.AdaptyFlow
import com.adapty.ui.internal.platform.AdaptyOnboarding
import com.adapty.ui.internal.platform.AdaptyPaywallProduct
import com.adapty.ui.internal.platform.AdaptyWebPresentation
import com.adapty.ui.internal.platform.ResultCallback
import com.adapty.ui.internal.platform.TimeInterval
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import com.adapty.ui.onboardings.AdaptyOnboardingConfiguration
import com.adapty.ui.onboardings.AdaptyOnboardingView
import com.adapty.ui.onboardings.listeners.AdaptyOnboardingEventListener
import com.adapty.ui.internal.platform.InternalAdaptyUiApi

// Platform bodies of AdaptyUI (SDK DI, views, media cache).

internal expect val defaultFlowLoadTimeout: TimeInterval

internal expect fun initAdaptyUIPlatform()

/** Throws when AdaptyUI deps aren't contributed yet; wrap with `withAdaptyUIActivated`. */
internal expect fun injectStateHandler(): StateHandler

internal expect fun stateHandlerOrNull(): StateHandler?

internal expect fun createFlowView(
    activity: Activity,
    flowConfiguration: FlowConfiguration,
    products: List<AdaptyPaywallProduct>?,
    eventListener: AdaptyFlowEventListener,
    insets: AdaptyFlowInsets,
    customAssets: AdaptyCustomAssets,
    tagResolver: AdaptyUiTagResolver,
    timerResolver: AdaptyUiTimerResolver,
    observerModeHandler: AdaptyUiObserverModeHandler?,
): AdaptyFlowView

internal expect fun createOnboardingView(
    activity: Activity,
    viewConfig: AdaptyOnboardingConfiguration,
    eventListener: AdaptyOnboardingEventListener,
    safeAreaPaddings: Boolean,
): AdaptyOnboardingView

internal expect fun createOnboardingConfiguration(
    onboarding: AdaptyOnboarding,
    externalUrlsPresentation: AdaptyWebPresentation,
): AdaptyOnboardingConfiguration

internal expect fun loadFlowConfiguration(
    flow: AdaptyFlow,
    locale: String?,
    loadTimeout: TimeInterval,
    customLayoutId: String?,
    callback: ResultCallback<FlowConfiguration>,
)

internal expect fun preloadMediaPlatform(rawConfig: Map<String, Any>)

internal expect fun configureMediaCachePlatform(config: MediaCacheConfiguration)

internal expect fun clearMediaCachePlatform(strategy: ClearCacheStrategy)
