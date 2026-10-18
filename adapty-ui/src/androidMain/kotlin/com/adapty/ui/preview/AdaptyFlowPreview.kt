@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.script.SDKGlobals
import com.adapty.ui.internal.text.MissingTagKind
import com.adapty.ui.internal.ui.AdaptyFlowInternal
import com.adapty.ui.internal.ui.FlowViewModel
import com.adapty.ui.internal.ui.FlowViewModelFactory
import com.adapty.ui.internal.ui.UserArgs
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.internal.utils.initializeFlowStateAsync
import com.adapty.ui.listeners.AdaptyFlowDefaultEventListener
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver

/**
 * Renders a preview [AdaptyUI.FlowConfiguration] (see [AdaptyUIPreview]) without an activated SDK.
 * Purchases and restores are mocked and reported to [previewEventCallback].
 *
 * @param[restoreState] `false` resets the flow's runtime (JS) state on every (re)entry.
 */
@Composable
public fun AdaptyFlowPreview(
    viewConfiguration: AdaptyUI.FlowConfiguration,
    products: List<AdaptyPaywallProduct>? = null,
    eventListener: AdaptyFlowEventListener? = null,
    previewEventCallback: PreviewEventCallback? = null,
    restoreState: Boolean = false,
) {
    AdaptyPaywallPreviewInternal(
        viewConfiguration,
        products,
        eventListener,
        previewEventCallback,
        restoreState,
        onMissingTag = null,
    )
}

/**
 * [AdaptyFlowPreview] plus [onMissingTag], which substitutes text for unresolved tags and empty variables.
 */
@Composable
public fun AdaptyPaywallPreviewInternal(
    viewConfiguration: AdaptyUI.FlowConfiguration,
    products: List<AdaptyPaywallProduct>? = null,
    eventListener: AdaptyFlowEventListener? = null,
    previewEventCallback: PreviewEventCallback? = null,
    restoreState: Boolean = false,
    onMissingTag: ((String, MissingTagKind) -> String)? = null,
) {
    val context = LocalContext.current
    val vmArgs = remember(viewConfiguration.id) {
        createPreviewViewModelArgs(context, previewEventCallback, onMissingTag)
    }
    val viewModel: FlowViewModel = viewModel(
        key = viewConfiguration.id,
        factory = FlowViewModelFactory(vmArgs),
    )

    DisposableEffect(viewConfiguration.id) {
        val stateHandler = vmArgs.stateHandler
        if (!restoreState) {
            viewConfiguration.runtimeState.clear()
            if (stateHandler.stateOwner === viewConfiguration) stateHandler.stateOwner = null
        }
        if (stateHandler.stateOwner !== viewConfiguration) {
            viewModel.dataState.value = null
        }
        val sdkEnvJson = SDKGlobals.buildPreviewSDKEnvJson(
            vmArgs.metaInfo,
            viewConfiguration.locale,
            viewConfiguration.localizationId,
            viewConfiguration.isRtl,
        )
        val sdkProductsJson = SDKGlobals.buildSDKProductsJson(products.orEmpty())
        stateHandler.initializeFlowStateAsync(viewConfiguration, sdkEnvJson, sdkProductsJson) {
            viewModel.setNewData(
                UserArgs.create(
                    viewConfiguration,
                    eventListener ?: AdaptyFlowDefaultEventListener(),
                    AdaptyFlowInsets.Unspecified,
                    AdaptyCustomAssets.Empty,
                    AdaptyUiTagResolver.Default,
                    AdaptyUiTimerResolver.Default,
                    null,
                    products,
                    ProductLoadingFailureCallback { false },
                )
            )
        }
        onDispose { }
    }

    if (viewModel.dataState.value != null) {
        AdaptyFlowInternal(viewModel)
    }
}
