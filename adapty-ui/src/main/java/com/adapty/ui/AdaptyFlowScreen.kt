@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adapty.internal.di.Dependencies
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.internal.script.SDKGlobals
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.internal.ui.AdaptyFlowInternal
import com.adapty.ui.internal.ui.FlowViewModel
import com.adapty.ui.internal.ui.FlowViewModelArgs
import com.adapty.ui.internal.ui.FlowViewModelFactory
import com.adapty.ui.internal.ui.UserArgs
import com.adapty.ui.internal.utils.FlowMode
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.internal.utils.adoptFlowStateForConfigChange
import com.adapty.ui.internal.utils.getActivityOrNull
import com.adapty.ui.internal.utils.getCurrentLocale
import com.adapty.ui.internal.utils.initializeFlowStateAsync
import com.adapty.ui.internal.utils.withAdaptyUIActivated
import com.adapty.ui.listeners.AdaptyFlowDefaultEventListener
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import java.util.UUID

/**
 * Flow screen composable representation
 *
 * @param[flowConfiguration] An [AdaptyUI.FlowConfiguration] object containing information
 * about the visual part of the flow. To load it, use the [AdaptyUI.getFlowConfiguration] method.
 *
 * @param[products] Optional [AdaptyPaywallProduct] list. Pass this value in order to optimize
 * the display time of the products on the screen. If you pass `null`, `AdaptyUI` will
 * automatically fetch the required products.
 *
 * @param[eventListener] An object that implements the [AdaptyFlowEventListener] interface.
 * Use it to respond to different events happening inside the purchase screen.
 * Also you can extend [AdaptyFlowDefaultEventListener] so you don't need to override all the methods.
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
 */
@Composable
public fun AdaptyFlowScreen(
    flowConfiguration: AdaptyUI.FlowConfiguration,
    products: List<AdaptyPaywallProduct>?,
    eventListener: AdaptyFlowEventListener,
    insets: AdaptyFlowInsets = AdaptyFlowInsets.Unspecified,
    customAssets: AdaptyCustomAssets = AdaptyCustomAssets.Empty,
    tagResolver: AdaptyUiTagResolver = AdaptyUiTagResolver.Default,
    timerResolver: AdaptyUiTimerResolver = AdaptyUiTimerResolver.Default,
    observerModeHandler: AdaptyUiObserverModeHandler? = null,
) {
    val context = LocalContext.current
    val vmArgs = remember {
        FlowViewModelArgs.create(
            "${UUID.randomUUID().toString().hashCode()}",
            context.getCurrentLocale(),
        )
    } ?: return

    val viewModel: FlowViewModel = viewModel(
        key = flowConfiguration.id,
        factory = FlowViewModelFactory(vmArgs)
    )
    val vmId = System.identityHashCode(viewModel)

    DisposableEffect(flowConfiguration) {
        val stateHandler = withAdaptyUIActivated {
            Dependencies.injectInternal<StateHandler>()
        }
        fun pushData() {
            viewModel.setNewData(
                UserArgs.create(
                    flowConfiguration,
                    eventListener,
                    insets,
                    customAssets,
                    tagResolver,
                    timerResolver,
                    observerModeHandler,
                    products,
                    ProductLoadingFailureCallback { error -> eventListener.onLoadingProductsFailure(error, context) },
                )
            )
        }

        val previousConfig = viewModel.dataState.value?.viewConfig
        val configChangeHandoff = viewModel.configChangeHandoffPending
        viewModel.configChangeHandoffPending = false
        if (previousConfig === flowConfiguration && viewModel.state != null) {
            pushData()
        } else {
            if (configChangeHandoff && previousConfig != null &&
                previousConfig.id == flowConfiguration.id && viewModel.state != null
            ) {
                stateHandler.adoptFlowStateForConfigChange(previousConfig, flowConfiguration)
            }
            if (stateHandler.stateOwner !== flowConfiguration) {
                viewModel.dataState.value = null
            }
            val sdkEnvJson = SDKGlobals.buildSDKEnvJson(vmArgs.metaInfoRetriever, context, flowConfiguration.mode, flowConfiguration.locale, flowConfiguration.localizationId, flowConfiguration.isRtl)
            val mode = flowConfiguration.mode
            val sdkProductsJson = if (mode is FlowMode.Live)
                SDKGlobals.buildStaticSDKProductsJson(mode.flow)
            else
                SDKGlobals.buildSDKProductsJson(emptyMap())
            stateHandler.initializeFlowStateAsync(flowConfiguration, sdkEnvJson, sdkProductsJson) {
                pushData()
            }
        }
        onDispose {
            if (context.getActivityOrNull()?.isChangingConfigurations == true) {
                viewModel.configChangeHandoffPending = true
            }
        }
    }

    if (viewModel.dataState.value != null) {
        AdaptyFlowInternal(viewModel)
    }
}