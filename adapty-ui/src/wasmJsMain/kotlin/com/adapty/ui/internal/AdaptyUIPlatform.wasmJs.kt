@file:Suppress("DEPRECATION")
@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal

import androidx.compose.runtime.Composable
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
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.script.ActionHandler
import com.adapty.ui.internal.script.StateAccessor
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.utils.Scope
import com.adapty.ui.internal.utils.TwoWayBinding
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

/** Inert state handler: no JS state, actions are dropped. */
private class NoOpStateHandler : StateHandler {
    private val emptyState = object : StateAccessor {
        override fun get(key: String): Any? = null
    }

    @Composable
    override fun observeState(): StateAccessor = emptyState
    override suspend fun reset() {}
    override suspend fun loadScript(script: String) {}
    override var stateOwner: Any? = null
    override suspend fun collectStateSnapshot(): Map<String, Any?>? = null
    override suspend fun applyStateSnapshot(snapshot: Map<String, Any?>) {}
    override suspend fun setActionsSuppressed(suppressed: Boolean) {}
    override var onStateRefreshed: (() -> Unit)? = null
    override suspend fun refreshState() {}
    override suspend fun setValue(binding: TwoWayBinding, value: Any?, screen: NavigationEntry) {}
    override suspend fun executeAction(func: String, params: Map<String, Any?>, scope: Scope, screen: NavigationEntry) {}
    override suspend fun invokeTimerCallback(timerId: String) {}
    override suspend fun sendSDKEvent(eventJson: String) {}
    override suspend fun invokePurchaseCallback(callbackId: String, response: Map<String, Any?>) {}
    override suspend fun invokeRestoreCallback(callbackId: String, response: Map<String, Any?>) {}
    override suspend fun invokeAlertCallback(callbackId: String, response: Map<String, Any?>) {}
    override suspend fun invokePermissionCallback(callbackId: String, response: Map<String, Any?>) {}
    override suspend fun injectSDKGlobals(sdkEnvJson: String, sdkProductsJson: String) {}
    override suspend fun updateSDKProducts(sdkProductsJson: String) {}
    override fun setActionHandler(handler: ActionHandler?) {}
}

internal actual fun injectStateHandler(): StateHandler {
    webStub("injectStateHandler")
    return NoOpStateHandler()
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
