@file:Suppress("DEPRECATION")

package com.adapty.ui.internal

import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.AdaptyFlowView
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
import com.adapty.ui.internal.platform.days
import com.adapty.ui.internal.platform.webStub
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import com.adapty.ui.onboardings.AdaptyOnboardingConfiguration
import com.adapty.ui.onboardings.AdaptyOnboardingView
import com.adapty.ui.onboardings.listeners.AdaptyOnboardingEventListener

// The live SDK (DI, store, media cache, views) does not exist on web.

internal actual val defaultFlowLoadTimeout: TimeInterval get() = 0.days

internal actual fun initAdaptyUIPlatform() {}

internal actual fun injectStateHandler(): StateHandler {
    webStub("injectStateHandler")
    throw IllegalStateException("AdaptyUI SDK dependencies are not available on web")
}

internal actual fun stateHandlerOrNull(): StateHandler? = null

internal actual fun createFlowView(
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
    webStub("createFlowView")
    return AdaptyFlowView()
}

internal actual fun createOnboardingView(
    activity: Activity,
    viewConfig: AdaptyOnboardingConfiguration,
    eventListener: AdaptyOnboardingEventListener,
    safeAreaPaddings: Boolean,
): AdaptyOnboardingView {
    webStub("createOnboardingView")
    return AdaptyOnboardingView()
}

internal actual fun createOnboardingConfiguration(
    onboarding: AdaptyOnboarding,
    externalUrlsPresentation: AdaptyWebPresentation,
): AdaptyOnboardingConfiguration {
    webStub("createOnboardingConfiguration")
    return AdaptyOnboardingConfiguration()
}

internal actual fun loadFlowConfiguration(
    flow: AdaptyFlow,
    locale: String?,
    loadTimeout: TimeInterval,
    customLayoutId: String?,
    callback: ResultCallback<FlowConfiguration>,
) {
    webStub("loadFlowConfiguration")
}

internal actual fun preloadMediaPlatform(rawConfig: Map<String, Any>) {
    webStub("preloadMediaPlatform")
}

internal actual fun configureMediaCachePlatform(config: MediaCacheConfiguration) {
    webStub("configureMediaCachePlatform")
}

internal actual fun clearMediaCachePlatform(strategy: ClearCacheStrategy) {
    webStub("clearMediaCachePlatform")
}
