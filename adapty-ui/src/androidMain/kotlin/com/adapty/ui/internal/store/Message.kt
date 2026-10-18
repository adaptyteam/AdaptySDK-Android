@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.store

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.models.AdaptyPurchaseResult
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUI.FlowConfiguration.TextItem
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.ui.UserArgs
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.internal.utils.TwoWayBinding

@InternalAdaptyApi
public sealed interface Message {
    public data class ActionsRequested internal constructor(val actions: List<Action>, val screen: NavigationEntry) : Message
    public data class ToggleChanged internal constructor(val binding: TwoWayBinding, val value: Boolean, val screen: NavigationEntry) : Message
    public data class ValueChanged internal constructor(val binding: TwoWayBinding, val value: Any?, val screen: NavigationEntry) : Message
    public data class TimerCompleted internal constructor(val timerId: String?, val actions: List<Action>, val screen: NavigationEntry) : Message
    public data class TimerCommandResolved internal constructor(val timerId: String, val endAtSeconds: Long) : Message
    public data class TimerCallbackExpired internal constructor(val timerId: String) : Message

    public data class DataUpdated internal constructor(val newData: UserArgs) : Message
    public data class ProductsLoaded internal constructor(val products: Map<String, AdaptyPaywallProduct>) : Message
    public data class ProductsLoadFailed internal constructor(val error: AdaptyError) : Message
    public data class AssetLoaded internal constructor(val id: String, val asset: Asset) : Message
    public data class TextsUpdated internal constructor(val texts: Map<String, TextItem>) : Message
    public data class AssetsUpdated internal constructor(val assets: Map<String, Asset>) : Message

    public sealed interface JSCallback : Message {
        public data class OpenUrl internal constructor(val source: Source, val openIn: String?) : JSCallback {
            public sealed class Source {
                public data class Url internal constructor(val url: String) : Source()
                public data class StringId internal constructor(val stringId: String) : Source()
            }
        }
        public data class CustomAction internal constructor(val id: String) : JSCallback
        public data class PurchaseProduct internal constructor(val productId: String, val paywallId: String? = null, val callbackId: String? = null) : JSCallback
        public data class WebPurchaseProduct internal constructor(val productId: String, val paywallId: String? = null, val openIn: String?, val callbackId: String? = null) : JSCallback
        public data class RestorePurchases internal constructor(val callbackId: String? = null) : JSCallback
        public object CloseAll : JSCallback
        public data class SelectProduct internal constructor(val productId: String, val paywallId: String? = null) : JSCallback
        public data class OpenScreen internal constructor(val entry: NavigationEntry) : JSCallback
        public data class CloseScreen internal constructor(val navigatorId: String = "default", val transitionId: String = "on_disappear") : JSCallback
        public data class MoveScroll internal constructor(val instanceId: String, val kind: String, val value: String) : JSCallback
        public data class ChangeFocus internal constructor(val focusId: String?) : JSCallback
        public data class SetTimer internal constructor(
            val timerId: String,
            val endAtMs: Long?,
            val durationSeconds: Long?,
            val behavior: String?,
        ) : JSCallback
        public data class SendAnalyticsEvent internal constructor(
            val name: String,
            val params: Map<String, Any?>,
        ) : JSCallback
        public data class SendEvents internal constructor(
            val instanceId: String?,
            val eventIds: List<String>,
        ) : JSCallback
        public object ShowAppRate : JSCallback
        public data class ShowAlertDialog internal constructor(
            val title: String?,
            val message: String?,
            val actions: List<AlertAction>,
            val callbackId: String?,
        ) : JSCallback {
            public data class AlertAction internal constructor(
                val title: String?,
                val style: Style,
                val actionId: String?,
            ) {
                public enum class Style { DEFAULT, DESTRUCTIVE, CANCEL }
            }
        }
        public data class ShowRequestPermission internal constructor(
            val permission: String?,
            val customArgs: Map<String, String>?,
            val callbackId: String?,
        ) : JSCallback
    }

    public object ScrollCommandConsumed : Message
    public object FocusCommandConsumed : Message

    public object FlushPendingNavigation : Message

    public data class FocusChanged internal constructor(val focusId: String?) : Message

    public data class FocusLost internal constructor(val focusId: String) : Message
    public data class FocusLostConfirmed internal constructor(val focusId: String, val generation: Int) : Message

    public data class PurchaseParamsReceived internal constructor(val params: AdaptyPurchaseParameters, val product: AdaptyPaywallProduct) : Message
    public data class PurchaseSucceeded internal constructor(val result: AdaptyPurchaseResult, val product: AdaptyPaywallProduct) : Message
    public data class PurchaseFailed internal constructor(val error: AdaptyError, val product: AdaptyPaywallProduct) : Message
    public data class RestoreSucceeded internal constructor(val profile: AdaptyProfile) : Message
    public data class RestoreFailed internal constructor(val error: AdaptyError) : Message
    public data class ObserverPurchaseStarted internal constructor(val product: AdaptyPaywallProduct) : Message
    public data class ObserverPurchaseFinished internal constructor(val product: AdaptyPaywallProduct) : Message
    public object ObserverRestoreStarted : Message
    public object ObserverRestoreFinished : Message

    public data class NavigatorAppeared internal constructor(val navigatorId: String) : Message
    public data class NavigatorDismissed internal constructor(val navigatorId: String) : Message

    public object FlowEntered : Message
    public object FlowExited : Message

    public data class PreviewPurchaseCompleted internal constructor(val productId: String, val callbackId: String?) : Message
    public data class PreviewRestoreCompleted internal constructor(val callbackId: String?) : Message

    public data class AlertDialogResolved internal constructor(
        val callbackId: String?,
        val actionId: String?,
    ) : Message

    public data class JSAlertCallbackInvoked internal constructor(
        val callbackId: String,
        val actionId: String?,
    ) : Message

    public data class JSPermissionCallbackInvoked internal constructor(
        val callbackId: String,
        val permission: String?,
        val customArgs: Map<String, String>?,
        val granted: Boolean,
        val detailResult: String?,
    ) : Message

    public data class JSError internal constructor(val message: String) : Message

    public data class UIError internal constructor(val message: String, val code: AdaptyErrorCode) : Message
}
