@file:OptIn(com.adapty.ui.internal.platform.InternalAdaptyUiApi::class)

package com.adapty.ui.internal.script

import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.NavigationEntry

/** No-op ActionHandler that records every call as "<method>" or "<method>:<arg>". */
internal class RecordingActionHandler : ActionHandler {
    val calls = mutableListOf<String>()
    val openedScreens = mutableListOf<NavigationEntry>()
    val jsErrors = mutableListOf<String>()

    override fun onOpenUrl(source: Message.JSCallback.OpenUrl.Source, openIn: String?) { calls += "openUrl" }
    override fun onUserCustomAction(userCustomId: String) { calls += "userCustomAction:$userCustomId" }
    override fun onPurchaseProduct(productId: String, paywallId: String?, callbackId: String?) { calls += "purchaseProduct:$productId" }
    override fun onWebPurchaseProduct(productId: String, paywallId: String?, openIn: String?, callbackId: String?) { calls += "webPurchaseProduct:$productId" }
    override fun onRestorePurchases(callbackId: String?) { calls += "restorePurchases" }
    override fun onCloseAll() { calls += "closeAll" }
    override fun onSelectProduct(productId: String, paywallId: String?) { calls += "selectProduct:$productId" }
    override fun onOpenScreen(navEntry: NavigationEntry) {
        calls += "openScreen:${navEntry.screenInstanceId}"
        openedScreens += navEntry
    }
    override fun onCloseScreen(navigatorId: String, transitionId: String) { calls += "closeScreen:$navigatorId" }
    override fun onMoveScroll(instanceId: String, kind: String, value: String) { calls += "moveScroll:$instanceId" }
    override fun onSetTimer(timerId: String, endAtMs: Long?, durationSeconds: Long?, behavior: String?) { calls += "setTimer:$timerId" }
    override fun onChangeFocus(focusId: String?) { calls += "changeFocus:$focusId" }
    override fun onSendAnalyticsEvent(name: String, params: Map<String, Any?>) { calls += "sendAnalyticsEvent:$name" }
    override fun onSendEvents(instanceId: String?, eventIds: List<String>) { calls += "sendEvents" }
    override fun onShowAppRate() { calls += "showAppRate" }
    override fun onShowAlertDialog(
        title: String?,
        message: String?,
        actions: List<Message.JSCallback.ShowAlertDialog.AlertAction>,
        callbackId: String?,
    ) { calls += "showAlertDialog" }
    override fun onShowRequestPermission(permission: String?, customArgs: Map<String, String>?, callbackId: String?) {
        calls += "showRequestPermission"
    }
    override fun onJsError(message: String) {
        calls += "jsError"
        jsErrors += message
    }
}
