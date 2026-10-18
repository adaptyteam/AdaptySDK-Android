@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.platform.AdaptyEligibility
import com.adapty.ui.internal.platform.AdaptyLogLevel
import com.adapty.ui.internal.platform.AdaptyPaywallProduct
import com.adapty.ui.internal.platform.AdaptyProductDiscountPhase
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.VERBOSE
import com.adapty.ui.internal.platform.WARN
import com.adapty.ui.internal.script.StateAccessor
import com.adapty.ui.internal.script.StateHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

internal fun AdaptyPaywallProduct.firstDiscountOfferOrNull(): AdaptyProductDiscountPhase? {
    return subscriptionDetails?.let { subDetails ->
        subDetails.introductoryOfferPhases.firstOrNull()?.takeIf { subDetails.introductoryOfferEligibility == AdaptyEligibility.ELIGIBLE }
    }
}

internal fun getProductGroupKey(groupId: String) = "group_${groupId}"

internal inline fun <T> withAdaptyUIActivated(body: () -> T): T {
    return runCatching { body() }.getOrElse { e ->
        if (e is NullPointerException) {
            log(WARN) { "$LOG_PREFIX Ensuring AdaptyUI initialization..." }
            ensureAdaptyUIInitialized()
            body()
        } else {
            throw e
        }
    }
}

private fun ensureAdaptyUIInitialized() {
    AdaptyUI
}

@InternalAdaptyUiApi
@Composable
public inline fun <reified T: Asset> Assets.getAsset(assetId: String): Asset.Composite<T>? {
    val customAsset = getAsset(assetId, true)?.let { it as? T }
    val defaultAsset = getAsset(assetId, false)?.let { it as? T }

    return when {
        customAsset != null && defaultAsset != null -> Asset.Composite(customAsset, defaultAsset)
        defaultAsset != null -> Asset.Composite(defaultAsset)
        else -> null
    }
}

@PublishedApi
@Composable
internal fun Assets.getAsset(assetId: String, isCustom: Boolean): Asset? {
    if (!isCustom) {
        val asset = if (!isSystemInDarkTheme())
            get(assetId)
        else
            get("${assetId}${DARK_THEME_ASSET_SUFFIX}") ?: get(assetId)
        return resolveFallbackChain(asset)
    }
    return getAsset("${assetId}${CUSTOM_ASSET_SUFFIX}", false)
}

internal fun <V> Map<String, V>.getRtlAware(key: String, isRtl: Boolean): V? =
    (if (isRtl) get("$key$RTL_VARIANT_SUFFIX") else null) ?: get(key)

private fun Assets.resolveFallbackChain(asset: Asset?): Asset? {
    if (asset !is Asset.Unknown) return asset
    val visited = mutableSetOf<String>()
    var current: Asset? = asset
    while (current is Asset.Unknown) {
        val fallbackAssetId = current.fallbackAssetId ?: return null
        if (!visited.add(fallbackAssetId)) return null
        current = get(fallbackAssetId)
    }
    return current
}

internal fun StateHandler.initializeFlowStateAsync(
    viewConfiguration: AdaptyUI.FlowConfiguration,
    sdkEnvJson: String,
    sdkProductsJson: String,
    callback: () -> Unit,
) {
    suspend {
        initializeFlowState(viewConfiguration, sdkEnvJson, sdkProductsJson)
    }.startCoroutine(Continuation(EmptyCoroutineContext) { callback() })
}

internal suspend fun StateHandler.initializeFlowState(
    viewConfiguration: AdaptyUI.FlowConfiguration,
    sdkEnvJson: String,
    sdkProductsJson: String,
) {
    if (stateOwner === viewConfiguration) {
        log(VERBOSE) { "$LOG_PREFIX initializeFlowState: VM already holds this flow's state, keeping it" }
        return
    }
    (stateOwner as? AdaptyUI.FlowConfiguration)?.let { previousOwner ->
        collectStateSnapshot()?.let { snapshot ->
            log(VERBOSE) { "$LOG_PREFIX initializeFlowState: snapshotting outgoing flow state (${snapshot.size} keys)" }
            previousOwner.runtimeState.jsSnapshot = snapshot
        }
    }
    stateOwner = null

    val snapshot = viewConfiguration.runtimeState.jsSnapshot
    val restoring = snapshot != null && viewConfiguration.runtimeState.restorableNavigation != null
    reset()
    injectSDKGlobals(sdkEnvJson, sdkProductsJson)
    if (restoring) setActionsSuppressed(true)
    loadScript(viewConfiguration.initialScript)
    if (sdkProductsJson != "{}") {
        sendSDKEvent("""{"name":"productsLoaded"}""")
    }
    if (snapshot != null) {
        log(VERBOSE) { "$LOG_PREFIX initializeFlowState: restoring saved flow state (${snapshot.size} keys)" }
        applyStateSnapshot(snapshot)
    }
    if (restoring) setActionsSuppressed(false)
    stateOwner = viewConfiguration
}

internal fun StateHandler.adoptFlowStateForConfigChange(
    previous: AdaptyUI.FlowConfiguration,
    next: AdaptyUI.FlowConfiguration,
) {
    log(VERBOSE) { "$LOG_PREFIX adoptFlowStateForConfigChange: handing flow state over to the re-created configuration instance" }
    next.runtimeState.adoptFrom(previous.runtimeState)
    if (stateOwner === previous) stateOwner = next
}

@InternalAdaptyUiApi
public fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    com.adapty.ui.internal.platform.log(messageLogLevel, msg)
}