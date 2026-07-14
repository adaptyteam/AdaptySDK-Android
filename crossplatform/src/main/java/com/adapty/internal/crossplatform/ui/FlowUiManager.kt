@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform.ui

import android.content.Context
import android.view.View
import androidx.core.view.doOnAttach
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.adapty.Adapty
import com.adapty.internal.crossplatform.SerializationHelper
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.EVENT_ID
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.FLOW_VIEW_OBSERVER_DID_INITIATE_PURCHASE
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.FLOW_VIEW_OBSERVER_DID_INITIATE_RESTORE
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.PRODUCT
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.VIEW
import com.adapty.internal.crossplatform.ui.CrossplatformUiHelper.Callback
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.log
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.ui.AdaptyFlowView
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.ui.FlowViewModel
import com.adapty.ui.listeners.AdaptyFlowEventListener.PermissionCallback
import com.adapty.ui.listeners.AdaptyFlowEventListener.PurchaseParamsCallback
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.AdaptyResult
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class FlowUiManager(
    private val serializationHelper: SerializationHelper,
) {
    private val cachedFlowUiData = ConcurrentHashMap<String, FlowUiData>()
    private val pendingDismissCallbacks = ConcurrentHashMap<String, MutableList<Callback<Unit>>>()
    private val permissionCallbacks = ConcurrentHashMap<String, PermissionCallback>()
    private val observerPurchaseCallbacks =
        ConcurrentHashMap<String, Pair<AdaptyUiObserverModeHandler.PurchaseStartCallback, AdaptyUiObserverModeHandler.PurchaseFinishCallback>>()
    private val observerRestoreCallbacks =
        ConcurrentHashMap<String, Pair<AdaptyUiObserverModeHandler.RestoreStartCallback, AdaptyUiObserverModeHandler.RestoreFinishCallback>>()

    var isObserverMode = false

    private fun registerPermissionRequest(callback: PermissionCallback): String {
        val eventId = UUID.randomUUID().toString()
        permissionCallbacks[eventId] = callback
        return eventId
    }

    fun resolvePermission(eventId: String, granted: Boolean, detail: String?) {
        permissionCallbacks.remove(eventId)?.invoke(granted, detail)
    }

    private fun registerObserverPurchase(
        onStartPurchase: AdaptyUiObserverModeHandler.PurchaseStartCallback,
        onFinishPurchase: AdaptyUiObserverModeHandler.PurchaseFinishCallback,
    ): String {
        val eventId = UUID.randomUUID().toString()
        observerPurchaseCallbacks[eventId] = onStartPurchase to onFinishPurchase
        return eventId
    }

    private fun registerObserverRestore(
        onStartRestore: AdaptyUiObserverModeHandler.RestoreStartCallback,
        onFinishRestore: AdaptyUiObserverModeHandler.RestoreFinishCallback,
    ): String {
        val eventId = UUID.randomUUID().toString()
        observerRestoreCallbacks[eventId] = onStartRestore to onFinishRestore
        return eventId
    }

    fun resolveObserverPurchaseStart(eventId: String) {
        observerPurchaseCallbacks[eventId]?.first?.invoke()
    }

    fun resolveObserverPurchaseFinish(eventId: String) {
        observerPurchaseCallbacks.remove(eventId)?.second?.invoke()
    }

    fun resolveObserverRestoreStart(eventId: String) {
        observerRestoreCallbacks[eventId]?.first?.invoke()
    }

    fun resolveObserverRestoreFinish(eventId: String) {
        observerRestoreCallbacks.remove(eventId)?.second?.invoke()
    }

    fun addPendingDismissCallback(key: String, callback: Callback<Unit>) {
        val callbacks = pendingDismissCallbacks.getOrPut(key) { mutableListOf() }
        synchronized(callbacks) {
            callbacks.add(callback)
        }
    }

    fun removePendingDismissCallback(key: String, callback: Callback<Unit>) {
        pendingDismissCallbacks[key]?.let { callbacks ->
            synchronized(callbacks) {
                callbacks.remove(callback)
            }
        }
    }

    private fun invokePendingDismissCallbacks(key: String) {
        val callbacks = pendingDismissCallbacks.remove(key) ?: return
        synchronized(callbacks) {
            callbacks.forEach { callback -> callback.invoke(Unit) }
        }
    }

    fun getData(key: String): FlowUiData? = cachedFlowUiData[key]

    fun putData(key: String, data: FlowUiData) {
        cachedFlowUiData[key] = data
    }

    fun removeData(key: String) {
        cachedFlowUiData.remove(key)
    }

    fun hasData(key: String) = cachedFlowUiData[key] != null

    var isShown = false

    var uiEventsObserver: ((event: AdaptyUiEvent) -> Unit)? = null

    var onDialogActionListener: AdaptyUiDialog.Listener? = null

    private var flowView: WeakReference<AdaptyFlowView>? = null

    fun getCurrentView(): AdaptyFlowView? = flowView?.get()

    fun setCurrentView(view: AdaptyFlowView) {
        flowView = WeakReference(view)
    }

    fun clearCurrentView() {
        flowView?.clear()
    }

    fun setupFlowView(
        flowView: FlowView,
        viewModelStoreOwner: ViewModelStoreOwner?,
        args: Any?,
        id: String,
        platformViewOnEvent: ((viewId: String, eventId: String, eventData: String) -> Unit)? = null,
    ) {
        setupFlowView(flowView.flowView, viewModelStoreOwner, args, id, platformViewOnEvent)
    }

    fun setupFlowView(
        flowView: AdaptyFlowView,
        viewModelStoreOwner: ViewModelStoreOwner?,
        args: Any?,
        id: String,
        platformViewOnEvent: ((viewId: String, eventId: String, eventData: String) -> Unit)? = null,
    ) {
        if (flowView.id == View.NO_ID) flowView.id = View.generateViewId()
        flowView.doOnAttach {
            if (flowView.findViewTreeViewModelStoreOwner() == null)
                flowView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            val createFlowViewArgs = (args as? String)?.let { serializationHelper.parseJsonArgument<CreateFlowViewArgs>(it) } ?: kotlin.run {
                log(AdaptyLogLevel.ERROR, { "could not parse args as CreateFlowViewArgs ($args)" })
                return@doOnAttach
            }
            val flow = createFlowViewArgs.flow

            AdaptyUI.getFlowConfiguration(
                flow,
            ) { viewConfigResult ->
                when (viewConfigResult) {
                    is AdaptyResult.Success -> {
                        val viewConfig = viewConfigResult.value

                        if (createFlowViewArgs.preloadProducts) {
                            Adapty.getPaywallProducts(flow) { productsResult ->
                                when (productsResult) {
                                    is AdaptyResult.Success -> {
                                        showFlowWithConfig(
                                            flowView,
                                            viewConfig,
                                            productsResult.value,
                                            createFlowViewArgs,
                                            id,
                                            platformViewOnEvent,
                                        )
                                    }
                                    is AdaptyResult.Error -> {
                                        showFlowWithConfig(
                                            flowView,
                                            viewConfig,
                                            null,
                                            createFlowViewArgs,
                                            id,
                                            platformViewOnEvent,
                                        )
                                    }
                                }
                            }
                        } else {
                            showFlowWithConfig(
                                flowView,
                                viewConfig,
                                null,
                                createFlowViewArgs,
                                id,
                                platformViewOnEvent,
                            )
                        }
                    }
                    is AdaptyResult.Error -> {
                        log(AdaptyLogLevel.ERROR, { "Failed to get view configuration: ${viewConfigResult.error.message}" })
                    }
                }
            }
        }
    }

    private fun showFlowWithConfig(
        flowView: AdaptyFlowView,
        viewConfig: AdaptyUI.FlowConfiguration,
        products: List<AdaptyPaywallProduct>?,
        createFlowViewArgs: CreateFlowViewArgs,
        id: String,
        platformViewOnEvent: ((viewId: String, eventId: String, eventData: String) -> Unit)? = null,
    ) {
        val uiData = FlowUiData(
            viewConfig,
            products,
            createFlowViewArgs,
            AdaptyUiFlowView(createFlowViewArgs.flow, id),
        )
        val eventListener = newFlowEventListener(
            uiData,
            platformViewOnEvent?.let { id to it },
        )

        flowView.show(
            viewConfig = viewConfig,
            products = products,
            eventListener = eventListener,
            customAssets = createFlowViewArgs.customAssets,
            customTags = createFlowViewArgs.customTags,
            customTimers = createFlowViewArgs.customTimers,
            enableSafeAreaPaddings = createFlowViewArgs.enableSafeAreaPaddings,
            observerModeHandler = observerModeHandlerOrNull(uiData, eventListener),
        )
    }

    fun observerModeHandlerOrNull(
        currentData: FlowUiData,
        eventListener: AdaptyUiEventListener,
    ): AdaptyUiObserverModeHandler? {
        if (!isObserverMode) return null
        return object : AdaptyUiObserverModeHandler {
            override fun onPurchaseInitiated(
                product: AdaptyPaywallProduct,
                onStartPurchase: AdaptyUiObserverModeHandler.PurchaseStartCallback,
                onFinishPurchase: AdaptyUiObserverModeHandler.PurchaseFinishCallback,
            ) {
                val eventId = registerObserverPurchase(onStartPurchase, onFinishPurchase)
                eventListener.onEvent(
                    AdaptyUiEvent(
                        FLOW_VIEW_OBSERVER_DID_INITIATE_PURCHASE,
                        VIEW to currentData.view,
                        EVENT_ID to eventId,
                        PRODUCT to product,
                    )
                )
            }

            override fun getRestoreHandler() =
                AdaptyUiObserverModeHandler.RestoreHandler { onStartRestore, onFinishRestore ->
                    val eventId = registerObserverRestore(onStartRestore, onFinishRestore)
                    eventListener.onEvent(
                        AdaptyUiEvent(
                            FLOW_VIEW_OBSERVER_DID_INITIATE_RESTORE,
                            VIEW to currentData.view,
                            EVENT_ID to eventId,
                        )
                    )
                }
        }
    }

    fun clearFlowView(flowView: FlowView) {
        clearFlowView(flowView.flowView)
    }

    fun clearFlowView(flowView: AdaptyFlowView) {
        runCatching {
            val method = flowView::class.java.getDeclaredMethod("getViewModel")
            method.isAccessible = true
            (method.invoke(flowView) as? FlowViewModel)
                ?.dataState?.value = null
        }
        flowView.setViewTreeViewModelStoreOwner(null)
    }

    fun newFlowEventListener(
        currentData: FlowUiData,
        platformViewOnEvent: Pair<String, (viewId: String, eventId: String, eventData: String) -> Unit>? = null,
    ) =
        object : AdaptyUiEventListener(currentData) {
            override fun onEvent(event: AdaptyUiEvent) {
                if (platformViewOnEvent != null) {
                    val (viewId, onEvent) = platformViewOnEvent
                    onEvent(viewId, event.id, serializationHelper.toJson(event.data))
                } else {
                    uiEventsObserver?.invoke(event)
                }
            }

            override fun onFlowClosed() {
                super.onFlowClosed()
                invokePendingDismissCallbacks(currentData.view.id)
            }

            override fun onAwaitingPurchaseParams(
                product: AdaptyPaywallProduct,
                context: Context,
                onPurchaseParamsReceived: PurchaseParamsCallback
            ): PurchaseParamsCallback.IveBeenInvoked {
                val key = product.payloadData.adaptyProductId
                onPurchaseParamsReceived(currentData.productPurchaseParams?.get(key) ?: AdaptyPurchaseParameters.Empty)
                return PurchaseParamsCallback.IveBeenInvoked
            }

            override fun onShowRequestPermission(
                permission: String?,
                customArgs: Map<String, String>?,
                callback: PermissionCallback,
                context: Context,
            ) {
                val eventId = registerPermissionRequest(callback)
                val data = listOfNotNull(
                    VIEW to currentData.view,
                    EVENT_ID to eventId,
                    PERMISSION to (permission ?: ""),
                    customArgs?.let { CUSTOM_ARGS to it },
                ).toTypedArray()
                onEvent(AdaptyUiEvent(FLOW_VIEW_DID_ASK_PERMISSION, *data))
            }
        }

}