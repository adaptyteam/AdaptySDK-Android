@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.errors.AdaptyError
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyFlow
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.models.AdaptyPurchaseResult
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN

internal sealed interface Effect {
    data class ExecuteJSActions(val actions: List<Action>, val screen: NavigationEntry) : Effect
    data class SetJSValue(val binding: TwoWayBinding, val value: Any?, val screen: NavigationEntry) : Effect
    object RefreshStateCache : Effect

    data class LoadProducts(val flow: AdaptyFlow, val failureCallback: ProductLoadingFailureCallback) : Effect
    data class LoadRemoteImage(val remoteImage: AdaptyUI.FlowConfiguration.Asset.RemoteImage, val assetId: String) : Effect
    data class UpdateTagResolver(val resolver: AdaptyUiTagResolver) : Effect

    data class AwaitPurchaseParams(val product: AdaptyPaywallProduct) : Effect
    data class StartPurchaseFlow(val product: AdaptyPaywallProduct, val params: AdaptyPurchaseParameters) : Effect
    data class StartWebPurchase(val product: AdaptyPaywallProduct, val flow: AdaptyFlow, val openIn: String?) : Effect
    object StartRestoreFlow : Effect
    data class InitiateObserverModePurchase(val product: AdaptyPaywallProduct, val handler: AdaptyUiObserverModeHandler) : Effect
    data class InitiateObserverModeRestore(val restoreHandler: AdaptyUiObserverModeHandler.RestoreHandler) : Effect

    sealed interface NotifyListener : Effect {
        data class ActionPerformed(val action: AdaptyUI.Action) : NotifyListener
        data class ProductSelected(val product: AdaptyPaywallProduct) : NotifyListener
        data class PurchaseStarted(val product: AdaptyPaywallProduct) : NotifyListener
        data class PurchaseFinished(val result: AdaptyPurchaseResult, val product: AdaptyPaywallProduct) : NotifyListener
        data class PurchaseFailed(val error: AdaptyError, val product: AdaptyPaywallProduct) : NotifyListener
        object RestoreStarted : NotifyListener
        data class RestoreSucceeded(val profile: AdaptyProfile) : NotifyListener
        data class RestoreFailed(val error: AdaptyError) : NotifyListener
        object FlowShown : NotifyListener
        object FlowClosed : NotifyListener
        data class OnError(val error: AdaptyError) : NotifyListener
        data class AnalyticEvent(val name: String, val params: Map<String, Any?>) : NotifyListener
        object ShowAppRate : NotifyListener
        data class ShowRequestPermission(
            val permission: String?,
            val customArgs: Map<String, String>?,
            val callbackId: String?,
        ) : NotifyListener
    }

    data class LogShowFlow(val flow: AdaptyFlow) : Effect

    data class LogFlowEvent(
        val flow: AdaptyFlow,
        val viewConfigurationId: String,
        val name: String,
        val params: Map<String, Any?>,
    ) : Effect

    data class ResolveTimerCommand(
        val timerId: String,
        val durationSeconds: Long,
        val behavior: String,
        val placementId: String,
        val timerResolver: AdaptyUiTimerResolver,
    ) : Effect
    data class InvokeTimerCallback(val timerId: String) : Effect
    data class ScheduleTimerCallback(val timerId: String, val delayMs: Long) : Effect
    data class VerifyFocusLost(val focusId: String, val generation: Int) : Effect
    data class PreviewPurchase(val productId: String, val callbackId: String?) : Effect
    data class PreviewRestore(val callbackId: String?) : Effect
    data class InvokeJSPurchaseCallback(val callbackId: String, val productId: String, val result: String) : Effect
    data class InvokeJSRestoreCallback(val callbackId: String, val result: String) : Effect
    data class InvokeJSAlertCallback(val callbackId: String, val actionId: String?) : Effect
    data class InvokeJSPermissionCallback(
        val callbackId: String,
        val permission: String?,
        val customArgs: Map<String, String>?,
        val granted: Boolean,
        val detailResult: String?,
    ) : Effect

    sealed class ObserverModeWarning(val message: String, val level: AdaptyLogLevel) : Effect {
        object HandlerInFullMode : ObserverModeWarning(
            "You should not pass observerModeHandler if you're using Adapty in Full Mode", WARN)
        object MissingHandler : ObserverModeWarning(
            "In order to handle purchases in Observer Mode enabled, provide the observerModeHandler!", WARN)
        object MissingRestoreHandler : ObserverModeWarning(
            "To handle restore manually in Observer Mode, implement getRestoreHandler() in observerModeHandler", VERBOSE)
    }

    data class UpdateJSProducts(val products: Map<String, AdaptyPaywallProduct>) : Effect

    sealed class SendSDKEvent : Effect {
        object ProductsLoaded : SendSDKEvent()
        data class WillPurchase(val productId: String) : SendSDKEvent()
        data class DidPurchase(val productId: String, val result: String) : SendSDKEvent()
        object WillRestorePurchases : SendSDKEvent()
        data class DidRestorePurchases(val result: String) : SendSDKEvent()
    }

    object ClearActionHandler : Effect

    data class PublishCustomEvents(val eventIds: List<String>, val instanceId: String?, val epoch: Long?) : Effect
}
