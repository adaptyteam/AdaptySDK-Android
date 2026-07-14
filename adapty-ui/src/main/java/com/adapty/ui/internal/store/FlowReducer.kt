@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.store

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPurchaseResult
import com.adapty.models.AdaptyWebPresentation
import com.adapty.ui.AdaptyCustomImageAsset
import com.adapty.ui.AdaptyCustomVideoAsset
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.internal.utils.CUSTOM_ASSET_SUFFIX
import com.adapty.ui.internal.utils.DARK_THEME_ASSET_SUFFIX
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.isLive
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.WARN

internal fun reduce(state: FlowState, message: Message): Pair<FlowState, List<Effect>> {
    return when (message) {

    is Message.ActionsRequested -> state to listOf(Effect.ExecuteJSActions(message.actions, message.screen))
    is Message.ToggleChanged -> state to listOf(Effect.SetJSValue(message.binding, message.value, message.screen))
    is Message.ValueChanged -> state to listOf(Effect.SetJSValue(message.binding, message.value, message.screen))
    is Message.TimerCompleted -> {
        val effects = mutableListOf<Effect>(Effect.ExecuteJSActions(message.actions, message.screen))
        if (message.timerId != null) {
            effects.add(Effect.InvokeTimerCallback(message.timerId))
        }
        state to effects
    }

    is Message.JSCallback.PurchaseProduct -> {
        if (state.purchase !is PurchaseFlowState.Idle) return state to emptyList()
        val product = state.products.items[message.productId] ?: return state to emptyList()
        if (!state.config.viewConfig.mode.isLive()) {
            return state.copy(ui = state.ui.copy(isLoading = state.config.viewConfig.showPurchaseLoader)) to listOf(
                Effect.SendSDKEvent.WillPurchase(message.productId),
                Effect.PreviewPurchase(message.productId, message.callbackId),
            )
        }
        val showLoader = state.config.viewConfig.showPurchaseLoader
        val cbId = message.callbackId
        val willPurchaseEvent = Effect.SendSDKEvent.WillPurchase(message.productId)
        when {
            state.config.isObserverMode && state.config.observerModeHandler != null ->
                state.copy(purchase = PurchaseFlowState.ObserverModeInitiated(product, cbId)) to
                    listOf(willPurchaseEvent, Effect.InitiateObserverModePurchase(product, state.config.observerModeHandler))
            state.config.isObserverMode && state.config.observerModeHandler == null ->
                state.copy(purchase = PurchaseFlowState.AwaitingParams(product, cbId), ui = state.ui.copy(isLoading = showLoader)) to
                    listOf(willPurchaseEvent, Effect.ObserverModeWarning.MissingHandler,
                           Effect.AwaitPurchaseParams(product))
            else -> {
                val effects = mutableListOf<Effect>(willPurchaseEvent)
                if (state.config.observerModeHandler != null)
                    effects.add(Effect.ObserverModeWarning.HandlerInFullMode)
                effects.add(Effect.AwaitPurchaseParams(product))
                state.copy(purchase = PurchaseFlowState.AwaitingParams(product, cbId), ui = state.ui.copy(isLoading = showLoader)) to effects
            }
        }
    }
    is Message.PurchaseParamsReceived -> {
        val cbId = (state.purchase as? PurchaseFlowState.AwaitingParams)?.callbackId
        state.copy(purchase = PurchaseFlowState.InProgress(message.product, cbId)) to
            listOf(Effect.NotifyListener.PurchaseStarted(message.product), Effect.StartPurchaseFlow(message.product, message.params))
    }
    is Message.PurchaseSucceeded -> {
        val productId = findFlowProductId(state.products.items, message.product)
        val resultStr = when (message.result) {
            is AdaptyPurchaseResult.Success -> "success"
            is AdaptyPurchaseResult.UserCanceled -> "userCanceled"
            is AdaptyPurchaseResult.Pending -> "pending"
        }
        val cbId = (state.purchase as? PurchaseFlowState.InProgress)?.callbackId
        val effects = mutableListOf<Effect>(
            Effect.SendSDKEvent.DidPurchase(productId, resultStr),
            Effect.NotifyListener.PurchaseFinished(message.result, message.product),
        )
        if (cbId != null) effects.add(Effect.InvokeJSPurchaseCallback(cbId, productId, resultStr))
        state.copy(purchase = PurchaseFlowState.Idle, ui = state.ui.copy(isLoading = false)) to effects
    }
    is Message.PurchaseFailed -> {
        val productId = findFlowProductId(state.products.items, message.product)
        val cbId = (state.purchase as? PurchaseFlowState.InProgress)?.callbackId
        val effects = mutableListOf<Effect>(
            Effect.SendSDKEvent.DidPurchase(productId, "fail"),
            Effect.NotifyListener.PurchaseFailed(message.error, message.product),
        )
        if (cbId != null) effects.add(Effect.InvokeJSPurchaseCallback(cbId, productId, "fail"))
        state.copy(purchase = PurchaseFlowState.Idle, ui = state.ui.copy(isLoading = false)) to effects
    }

    is Message.JSCallback.WebPurchaseProduct -> {
        if (!state.config.viewConfig.mode.isLive()) {
            return state.copy(ui = state.ui.copy(isLoading = state.config.viewConfig.showPurchaseLoader)) to listOf(
                Effect.SendSDKEvent.WillPurchase(message.productId),
                Effect.PreviewPurchase(message.productId, message.callbackId),
            )
        }
        val product = state.products.items[message.productId] ?: return state to emptyList()
        state to listOf(Effect.StartWebPurchase(product, state.config.viewConfig.mode.flow, message.openIn))
    }

    is Message.JSCallback.RestorePurchases -> {
        if (state.restore !is RestoreFlowState.Idle) return state to emptyList()
        if (!state.config.viewConfig.mode.isLive()) {
            return state.copy(ui = state.ui.copy(isLoading = state.config.viewConfig.showRestoreLoader)) to listOf(
                Effect.SendSDKEvent.WillRestorePurchases,
                Effect.PreviewRestore(message.callbackId),
            )
        }
        val showLoader = state.config.viewConfig.showRestoreLoader
        val cbId = message.callbackId
        val willRestoreEvent = Effect.SendSDKEvent.WillRestorePurchases
        when {
            state.config.isObserverMode && state.config.observerModeHandler != null -> {
                val restoreHandler = state.config.observerModeHandler.getRestoreHandler()
                if (restoreHandler != null)
                    state.copy(restore = RestoreFlowState.ObserverModeInitiated(cbId)) to
                        listOf(willRestoreEvent, Effect.InitiateObserverModeRestore(restoreHandler))
                else
                    state.copy(restore = RestoreFlowState.InProgress(cbId), ui = state.ui.copy(isLoading = showLoader)) to
                        listOf(willRestoreEvent, Effect.ObserverModeWarning.MissingRestoreHandler,
                               Effect.NotifyListener.RestoreStarted, Effect.StartRestoreFlow)
            }
            state.config.isObserverMode && state.config.observerModeHandler == null ->
                state.copy(restore = RestoreFlowState.InProgress(cbId), ui = state.ui.copy(isLoading = showLoader)) to
                    listOf(willRestoreEvent, Effect.ObserverModeWarning.MissingRestoreHandler,
                           Effect.NotifyListener.RestoreStarted, Effect.StartRestoreFlow)
            else -> {
                val effects = mutableListOf<Effect>(willRestoreEvent)
                if (state.config.observerModeHandler != null)
                    effects.add(Effect.ObserverModeWarning.HandlerInFullMode)
                effects.add(Effect.NotifyListener.RestoreStarted)
                effects.add(Effect.StartRestoreFlow)
                state.copy(restore = RestoreFlowState.InProgress(cbId), ui = state.ui.copy(isLoading = showLoader)) to effects
            }
        }
    }
    is Message.RestoreSucceeded -> {
        val cbId = (state.restore as? RestoreFlowState.InProgress)?.callbackId
        val effects = mutableListOf<Effect>(
            Effect.SendSDKEvent.DidRestorePurchases("success"),
            Effect.NotifyListener.RestoreSucceeded(message.profile),
        )
        if (cbId != null) effects.add(Effect.InvokeJSRestoreCallback(cbId, "success"))
        state.copy(restore = RestoreFlowState.Idle, ui = state.ui.copy(isLoading = false)) to effects
    }
    is Message.RestoreFailed -> {
        val cbId = (state.restore as? RestoreFlowState.InProgress)?.callbackId
        val effects = mutableListOf<Effect>(
            Effect.SendSDKEvent.DidRestorePurchases("fail"),
            Effect.NotifyListener.RestoreFailed(message.error),
        )
        if (cbId != null) effects.add(Effect.InvokeJSRestoreCallback(cbId, "fail"))
        state.copy(restore = RestoreFlowState.Idle, ui = state.ui.copy(isLoading = false)) to effects
    }

    is Message.ObserverPurchaseStarted ->
        state.copy(ui = state.ui.copy(isLoading = state.config.viewConfig.showPurchaseLoader)) to emptyList()
    is Message.ObserverPurchaseFinished -> {
        val cbId = (state.purchase as? PurchaseFlowState.ObserverModeInitiated)?.callbackId
        val productId = findFlowProductId(state.products.items, message.product)
        val effects = mutableListOf<Effect>()
        if (cbId != null) effects.add(Effect.InvokeJSPurchaseCallback(cbId, productId, "success"))
        state.copy(purchase = PurchaseFlowState.Idle, ui = state.ui.copy(isLoading = false)) to effects
    }
    is Message.ObserverRestoreStarted ->
        state.copy(ui = state.ui.copy(isLoading = state.config.viewConfig.showRestoreLoader)) to emptyList()
    is Message.ObserverRestoreFinished -> {
        val cbId = (state.restore as? RestoreFlowState.ObserverModeInitiated)?.callbackId
        val effects = mutableListOf<Effect>()
        if (cbId != null) effects.add(Effect.InvokeJSRestoreCallback(cbId, "success"))
        state.copy(restore = RestoreFlowState.Idle, ui = state.ui.copy(isLoading = false)) to effects
    }

    is Message.PreviewPurchaseCompleted -> {
        val effects = mutableListOf<Effect>(
            Effect.SendSDKEvent.DidPurchase(message.productId, "success"),
        )
        if (message.callbackId != null) effects.add(Effect.InvokeJSPurchaseCallback(message.callbackId, message.productId, "success"))
        state.copy(ui = state.ui.copy(isLoading = false)) to effects
    }
    is Message.PreviewRestoreCompleted -> {
        val effects = mutableListOf<Effect>(
            Effect.SendSDKEvent.DidRestorePurchases("success"),
        )
        if (message.callbackId != null) effects.add(Effect.InvokeJSRestoreCallback(message.callbackId, "success"))
        state.copy(ui = state.ui.copy(isLoading = false)) to effects
    }

    is Message.JSCallback.OpenUrl -> {
        val resolvedUrl = when (val source = message.source) {
            is Message.JSCallback.OpenUrl.Source.Url -> source.url
            is Message.JSCallback.OpenUrl.Source.StringId -> resolveTextParam(source.stringId, state.texts.items)
                ?: return state to listOf(Effect.NotifyListener.OnError(adaptyError(
                    message = "open-url action string id '${source.stringId}' did not resolve to a URL",
                    adaptyErrorCode = AdaptyErrorCode.INVALID_ACTION_URL,
                )))
        }
        val presentation = when (message.openIn) {
            "browser_in_app" -> AdaptyWebPresentation.InAppBrowser
            else -> AdaptyWebPresentation.ExternalBrowser
        }
        state to listOf(Effect.NotifyListener.ActionPerformed(AdaptyUI.Action.OpenUrl(resolvedUrl, presentation)))
    }
    is Message.JSCallback.CustomAction ->
        state to listOf(Effect.NotifyListener.ActionPerformed(AdaptyUI.Action.Custom(message.id)))
    is Message.JSCallback.CloseAll ->
        state to listOf(Effect.NotifyListener.ActionPerformed(AdaptyUI.Action.Close))
    is Message.JSCallback.SelectProduct -> {
        val product = state.products.items[message.productId]
        val isLegacy = state.config.viewConfig.isLegacyFormat
        when {
            product != null && (!isLegacy || state.ui.flowShown) ->
                state to listOf(Effect.NotifyListener.ProductSelected(product))
            isLegacy ->
                state.copy(
                    products = state.products.copy(
                        pendingSelectedProductIds = state.products.pendingSelectedProductIds + message.productId,
                    )
                ) to emptyList()
            else -> state to emptyList()
        }
    }
    is Message.JSCallback.OpenScreen -> {
        val navigators = state.config.viewConfig.navigators
        val requestedId = message.entry.navigatorId
        val navigatorId = when {
            requestedId in navigators -> requestedId
            "default" in navigators -> "default"
            else -> null
        }
        if (navigatorId == null) {
            log(ERROR) { "$LOG_PREFIX Unknown navigatorId: $requestedId" }
            state to listOf(Effect.NotifyListener.OnError(adaptyError(
                message = "navigator '$requestedId' was not found in the view configuration and no 'default' navigator is available",
                adaptyErrorCode = AdaptyErrorCode.NAVIGATOR_NOT_FOUND,
            )))
        } else {
            val entry =
                if (navigatorId == requestedId) message.entry
                else message.entry.copy(navigatorId = navigatorId)
            state.copy(
                navigation = state.navigation.copy(
                    pending = state.navigation.pending + (navigatorId to entry)
                )
            ) to listOf(Effect.RefreshStateCache)
        }
    }
    is Message.FlushPendingNavigation -> {
        val pending = state.navigation.pending
        if (pending.isEmpty()) {
            state to emptyList()
        } else {
            state.copy(
                navigation = NavigationState(entries = state.navigation.entries + pending)
            ) to emptyList()
        }
    }
    is Message.JSCallback.MoveScroll ->
        state.copy(ui = state.ui.copy(scrollCommand = ScrollCommand(message.instanceId, message.kind, message.value))) to emptyList()

    is Message.JSCallback.SetTimer -> {
        if (message.endAtMs != null) {
            val command = TimerSetCommand(message.endAtMs / 1000L)
            val delayMs = (message.endAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
            state.copy(
                ui = state.ui.copy(
                    timerCommands = state.ui.timerCommands + (message.timerId to command)
                )
            ) to listOf(Effect.ScheduleTimerCallback(message.timerId, delayMs))
        } else if (message.durationSeconds != null) {
            val behavior = message.behavior ?: "restart"
            if (behavior == "continue") {
                val now = System.currentTimeMillis() / 1000L
                val existing = state.ui.timerCommands[message.timerId]
                if (existing != null && existing.endAtSeconds > now) {
                    return state to emptyList()
                }
            }
            state to listOf(Effect.ResolveTimerCommand(
                timerId = message.timerId,
                durationSeconds = message.durationSeconds,
                behavior = behavior,
                placementId = state.config.placementId,
                timerResolver = state.config.timerResolver,
            ))
        } else {
            state to emptyList()
        }
    }

    is Message.TimerCommandResolved -> {
        val command = TimerSetCommand(message.endAtSeconds)
        val delayMs = (message.endAtSeconds * 1000L - System.currentTimeMillis()).coerceAtLeast(0L)
        state.copy(
            ui = state.ui.copy(
                timerCommands = state.ui.timerCommands + (message.timerId to command)
            )
        ) to listOf(Effect.ScheduleTimerCallback(message.timerId, delayMs))
    }

    is Message.TimerCallbackExpired -> {
        state to listOf(Effect.InvokeTimerCallback(message.timerId))
    }

    is Message.JSCallback.ChangeFocus -> {
        val effects = focusChangeEffects(state, message.focusId)
        state.copy(ui = state.ui.copy(
            focusCommand = FocusCommand(message.focusId),
            currentFocusId = message.focusId,
        )) to effects
    }

    is Message.FocusChanged -> {
        if (state.ui.currentFocusId == message.focusId) {
            state.copy(ui = state.ui.copy(focusGeneration = state.ui.focusGeneration + 1)) to emptyList()
        } else {
            val effects = focusChangeEffects(state, message.focusId)
            state.copy(ui = state.ui.copy(
                currentFocusId = message.focusId,
                focusGeneration = state.ui.focusGeneration + 1,
            )) to effects
        }
    }

    is Message.FocusLost -> {
        if (state.ui.currentFocusId != message.focusId) state to emptyList()
        else state to listOf(Effect.VerifyFocusLost(message.focusId, state.ui.focusGeneration))
    }

    is Message.FocusLostConfirmed -> {
        if (state.ui.currentFocusId == message.focusId && state.ui.focusGeneration == message.generation) {
            val effects = focusChangeEffects(state, null)
            state.copy(ui = state.ui.copy(currentFocusId = null)) to effects
        } else {
            state to emptyList()
        }
    }

    is Message.ScrollCommandConsumed ->
        state.copy(ui = state.ui.copy(scrollCommand = null)) to emptyList()

    is Message.FocusCommandConsumed ->
        state.copy(ui = state.ui.copy(focusCommand = null)) to emptyList()

    is Message.JSCallback.SendAnalyticsEvent -> {
        val isBackend = message.params["isBackendEvent"] as? Boolean ?: true
        val isCustomer = message.params["isCustomerEvent"] as? Boolean ?: false
        val effects = mutableListOf<Effect>()
        if (isBackend && state.config.viewConfig.mode.isLive())
            effects.add(
                Effect.LogFlowEvent(
                    state.config.viewConfig.mode.flow,
                    state.config.viewConfig.id,
                    message.name,
                    message.params,
                )
            )
        if (isCustomer)
            effects.add(Effect.NotifyListener.AnalyticEvent(message.name, message.params))
        state to effects
    }

    is Message.JSCallback.SendEvents -> {
        val epoch = state.navigation.entries.values
            .firstOrNull { it.screenInstanceId == message.instanceId }?.epoch
        if (message.instanceId != null && epoch == null) {
            state to emptyList()
        } else {
            state to listOf(Effect.PublishCustomEvents(message.eventIds, message.instanceId, epoch))
        }
    }

    is Message.JSCallback.ShowAppRate ->
        state to listOf(Effect.NotifyListener.ShowAppRate)

    is Message.JSCallback.ShowAlertDialog -> {
        if (state.ui.alertDialog != null) {
            log(WARN) { "$LOG_PREFIX showAlertDialog ignored: alert already presenting" }
            state to emptyList()
        } else {
            state.copy(ui = state.ui.copy(
                alertDialog = AlertDialogState(message.title, message.message, message.actions, message.callbackId)
            )) to emptyList()
        }
    }

    is Message.JSCallback.ShowRequestPermission ->
        state to listOf(Effect.NotifyListener.ShowRequestPermission(message.permission, message.customArgs, message.callbackId))

    is Message.JSCallback.CloseScreen -> {
        val entry = state.navigation.entries[message.navigatorId]
            ?: return state to emptyList()
        state.copy(
            navigation = state.navigation.copy(
                entries = state.navigation.entries - message.navigatorId,
                closingEntries = state.navigation.closingEntries + (message.navigatorId to ClosingNavigator(entry, message.transitionId)),
            )
        ) to emptyList()
    }

    is Message.DataUpdated -> {
        val newData = message.newData
        val viewConfig = newData.viewConfig
        val mode = viewConfig.mode

        val newConfig = ConfigState(
            viewConfig = viewConfig,
            isObserverMode = state.config.isObserverMode,
            placementId = mode.placementId,
            observerModeHandler = newData.observerModeHandler,
            eventListener = newData.eventListener,
            userInsets = newData.userInsets,
            customAssets = newData.customAssets,
            tagResolver = newData.tagResolver,
            timerResolver = newData.timerResolver,
            productLoadingFailureCallback = newData.productLoadingFailureCallback,
        )

        val newProductsFromArgs = if (mode.isLive()) {
            associateProductsToIds(newData.products, mode.flow)
        } else {
            newData.products.associateBy { resolveProductKey(it) }
        }
        val mergedProducts = state.products.items + newProductsFromArgs

        val localAssets = buildLocalAssetsMap(viewConfig, newData.customAssets)
        val remoteImageIds = viewConfig.assets.filterValues { it is Asset.RemoteImage }.keys
        val mergedAssets = (state.assets.items + localAssets).toMutableMap()
        remoteImageIds.forEach { id ->
            state.assets.items[id]?.let { mergedAssets[id] = it }
            val customId = customAssetId(id)
            state.assets.items[customId]?.let { mergedAssets[customId] = it }
        }

        val (resolvedProductsState, selectionEffects) = state.products.copy(
            items = mergedProducts,
            loadingStatus = if (mergedProducts.isNotEmpty()) LoadingStatus.Loaded else state.products.loadingStatus,
        ).resolvePendingSelections(state.ui.flowShown)

        val newState = state.copy(
            config = newConfig,
            products = resolvedProductsState,
            assets = AssetsState(mergedAssets),
            texts = TextsState(viewConfig.texts),
        )

        val effects = mutableListOf<Effect>()
        effects.addAll(selectionEffects)
        val customAssets = newData.customAssets

        if (mode.isLive() && mergedProducts.isEmpty()) {
            effects.add(Effect.LoadProducts(mode.flow, newData.productLoadingFailureCallback))
        }
        if (mergedProducts.isNotEmpty() && mergedProducts != state.products.items) {
            effects.add(Effect.UpdateJSProducts(mergedProducts))
        }

        viewConfig.assets.forEach { (id, asset) ->
            if (asset is Asset.RemoteImage) {
                effects.add(Effect.LoadRemoteImage(asset, id))

                when (val customAsset = asset.customId?.let(customAssets::getImage)) {
                    is AdaptyCustomImageAsset.Remote -> {
                        val customAssetId = customAssetId(id)
                        effects.add(Effect.LoadRemoteImage(customAsset.value, customAssetId))
                    }
                    else -> Unit
                }
            }

            if (asset is Asset.Image) {
                when (val customAsset = asset.customId?.let(customAssets::getImage)) {
                    is AdaptyCustomImageAsset.Remote -> {
                        val customAssetId = customAssetId(id)
                        effects.add(Effect.LoadRemoteImage(customAsset.value, customAssetId))
                    }
                    else -> Unit
                }
            }

            if (asset is Asset.Video) {
                when (val customAsset = asset.customId?.let(customAssets::getVideo)) {
                    is AdaptyCustomVideoAsset -> {
                        when (val customPreviewAsset = customAsset.preview) {
                            is AdaptyCustomImageAsset.Remote -> {
                                val customAssetId = customAssetId(id)
                                effects.add(Effect.LoadRemoteImage(customPreviewAsset.value, customAssetId))
                            }
                            else -> Unit
                        }
                    }
                    else -> Unit
                }
            }
        }

        effects.add(Effect.UpdateTagResolver(newConfig.tagResolver))

        newState to effects
    }
    is Message.ProductsLoaded -> {
        val mergedProducts = state.products.items + message.products
        val (productsState, selectionEffects) = state.products
            .copy(items = mergedProducts, loadingStatus = LoadingStatus.Loaded)
            .resolvePendingSelections(state.ui.flowShown)
        state.copy(products = productsState, ui = state.ui.copy(isLoading = false)) to
            (listOf(Effect.UpdateJSProducts(mergedProducts), Effect.SendSDKEvent.ProductsLoaded) + selectionEffects)
    }
    is Message.ProductsLoadFailed -> {
        state.copy(products = state.products.copy(loadingStatus = LoadingStatus.Error(message.error)),
            ui = state.ui.copy(isLoading = false)) to emptyList()
    }
    is Message.AssetLoaded ->
        state.copy(assets = AssetsState(state.assets.items + (message.id to message.asset))) to emptyList()
    is Message.TextsUpdated ->
        state.copy(texts = TextsState(state.texts.items + message.texts)) to emptyList()
    is Message.AssetsUpdated ->
        state.copy(assets = AssetsState(state.assets.items + message.assets)) to emptyList()

    is Message.NavigatorAppeared -> state to emptyList()
    is Message.NavigatorDismissed ->
        state.copy(
            navigation = state.navigation.copy(
                closingEntries = state.navigation.closingEntries - message.navigatorId,
            )
        ) to emptyList()

    is Message.FlowEntered -> {
        val effects = mutableListOf<Effect>(Effect.NotifyListener.FlowShown)
        if (state.config.viewConfig.mode.isLive() && state.config.viewConfig.isLegacyFormat)
            effects.add(0, Effect.LogShowFlow(state.config.viewConfig.mode.flow))
        val (productsState, selectionEffects) = state.products.resolvePendingSelections(flowShown = true)
        state.copy(products = productsState, ui = state.ui.copy(flowShown = true)) to (effects + selectionEffects)
    }
    is Message.FlowExited ->
        state.copy(navigation = NavigationState(), ui = state.ui.copy(flowShown = false)) to
            listOf(Effect.ClearActionHandler, Effect.NotifyListener.FlowClosed)

    is Message.AlertDialogResolved -> {
        val effects = if (message.callbackId != null)
            listOf<Effect>(Effect.InvokeJSAlertCallback(message.callbackId, message.actionId))
        else emptyList()
        state.copy(ui = state.ui.copy(alertDialog = null)) to effects
    }

    is Message.JSAlertCallbackInvoked ->
        state to listOf(Effect.InvokeJSAlertCallback(message.callbackId, message.actionId))

    is Message.JSPermissionCallbackInvoked ->
        state to listOf(Effect.InvokeJSPermissionCallback(
            callbackId = message.callbackId,
            permission = message.permission,
            customArgs = message.customArgs,
            granted = message.granted,
            detailResult = message.detailResult,
        ))

    is Message.JSError ->
        state to listOf(Effect.NotifyListener.OnError(
            adaptyError(message = message.message, adaptyErrorCode = AdaptyErrorCode.JS_EXCEPTION)
        ))

    is Message.UIError ->
        state to listOf(Effect.NotifyListener.OnError(
            adaptyError(message = message.message, adaptyErrorCode = message.code)
        ))
}
}

private fun focusChangeEffects(state: FlowState, newFocusId: String?): List<Effect> {
    val focusParams = mapOf<String, Any?>("focusId" to newFocusId, "oldFocusId" to state.ui.currentFocusId)
    val effects = mutableListOf<Effect>()
    for ((navId, entry) in state.navigation.entries) {
        val navConfig = state.config.viewConfig.navigators[navId] ?: continue
        val screen = state.config.viewConfig.screens.screens[entry.screenType]
        val actions = screen?.onFocusChange ?: navConfig.onFocusChange ?: continue
        val enhanced = actions.map { a -> Action(a.func, a.params + focusParams, a.scope) }
        effects.add(Effect.ExecuteJSActions(enhanced, entry))
    }
    return effects
}

private fun resolveTextParam(value: String, texts: Map<String, AdaptyUI.FlowConfiguration.TextItem>): String? {
    val textItem = texts[value] ?: return null
    return extractPlainText(textItem.value) ?: textItem.fallback?.let(::extractPlainText)
}

private fun extractPlainText(richText: AdaptyUI.FlowConfiguration.RichText): String? {
    return richText.items
        .filterIsInstance<AdaptyUI.FlowConfiguration.RichText.Item.Text>()
        .joinToString("") { it.text }
        .takeIf { it.isNotEmpty() }
}

private fun ProductsState.resolvePendingSelections(flowShown: Boolean): Pair<ProductsState, List<Effect>> {
    if (!flowShown || pendingSelectedProductIds.isEmpty()) return this to emptyList()
    val resolvedIds = pendingSelectedProductIds.filter { it in items }
    if (resolvedIds.isEmpty()) return this to emptyList()
    val effects = resolvedIds.map { Effect.NotifyListener.ProductSelected(items.getValue(it)) }
    return copy(pendingSelectedProductIds = pendingSelectedProductIds - resolvedIds.toSet()) to effects
}

private fun findFlowProductId(
    items: Map<String, AdaptyPaywallProduct>,
    product: AdaptyPaywallProduct,
): String {
    return items.entries.firstOrNull { it.value === product }?.key
        ?: product.vendorProductId
}

private fun customAssetId(id: String): String =
    if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
        "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
    else
        "${id}${CUSTOM_ASSET_SUFFIX}"
