@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.errors.AdaptyError
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUI.FlowConfiguration.TextItem
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver

internal data class FlowState(
    val config: ConfigState,
    val products: ProductsState,
    val assets: AssetsState,
    val texts: TextsState,
    val purchase: PurchaseFlowState,
    val restore: RestoreFlowState,
    val navigation: NavigationState,
    val ui: UiState,
)

internal data class ConfigState(
    val viewConfig: AdaptyUI.FlowConfiguration,
    val isObserverMode: Boolean,
    val placementId: String,
    val observerModeHandler: AdaptyUiObserverModeHandler?,
    val eventListener: AdaptyFlowEventListener,
    val userInsets: AdaptyFlowInsets,
    val customAssets: AdaptyCustomAssets,
    val tagResolver: AdaptyUiTagResolver,
    val timerResolver: AdaptyUiTimerResolver,
    val productLoadingFailureCallback: ProductLoadingFailureCallback,
)

internal data class ProductsState(
    val items: Map<String, AdaptyPaywallProduct>,
    val loadingStatus: LoadingStatus,
)

internal sealed interface LoadingStatus {
    object Idle : LoadingStatus
    object Loading : LoadingStatus
    object Loaded : LoadingStatus
    data class Error(val error: AdaptyError) : LoadingStatus
}

internal data class AssetsState(val items: Map<String, Asset>)
internal data class TextsState(val items: Map<String, TextItem>)

internal sealed interface PurchaseFlowState {
    object Idle : PurchaseFlowState
    data class AwaitingParams(val product: AdaptyPaywallProduct, val callbackId: String? = null) : PurchaseFlowState
    data class InProgress(val product: AdaptyPaywallProduct, val callbackId: String? = null) : PurchaseFlowState
    data class ObserverModeInitiated(val product: AdaptyPaywallProduct, val callbackId: String? = null) : PurchaseFlowState
}

internal sealed interface RestoreFlowState {
    object Idle : RestoreFlowState
    data class InProgress(val callbackId: String? = null) : RestoreFlowState
    data class ObserverModeInitiated(val callbackId: String? = null) : RestoreFlowState
}

internal data class ClosingNavigator(
    val entry: NavigationEntry,
    val appearanceKey: String,
)

internal data class NavigationState(
    val entries: Map<String, NavigationEntry> = emptyMap(),
    val closingEntries: Map<String, ClosingNavigator> = emptyMap(),
    val pending: Map<String, NavigationEntry> = emptyMap(),
)
internal data class UiState(
    val isLoading: Boolean,
    val flowShown: Boolean,
    val scrollCommand: ScrollCommand? = null,
    val timerCommands: Map<String, TimerSetCommand> = emptyMap(),
    val focusCommand: FocusCommand? = null,
    val currentFocusId: String? = null,
    val focusGeneration: Int = 0,
    val alertDialog: AlertDialogState? = null,
)

internal data class AlertDialogState(
    val title: String?,
    val message: String?,
    val actions: List<Message.JSCallback.ShowAlertDialog.AlertAction>,
    val callbackId: String?,
)

internal data class ScrollCommand(
    val instanceId: String,
    val kind: String,
    val value: String,
)

internal data class TimerSetCommand(
    val endAtSeconds: Long,
)

internal data class FocusCommand(
    val focusId: String?,
)
