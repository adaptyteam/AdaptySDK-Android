package com.adapty.internal.crossplatform.ui

import android.content.Context
import com.adapty.errors.AdaptyError
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchaseResult
import com.adapty.ui.AdaptyUI
import com.adapty.ui.listeners.AdaptyFlowDefaultEventListener
import java.util.concurrent.atomic.AtomicInteger

internal abstract class AdaptyUiEventListener(
    private val currentData: FlowUiData,
) : AdaptyFlowDefaultEventListener() {

    private val retryCounter = AtomicInteger()

    override fun onActionPerformed(action: AdaptyUI.Action, context: Context) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_PERFORM_ACTION,
                VIEW to currentData.view,
                ACTION to action,
            )
        )
    }

    override fun onBackPressed(context: Context): Boolean {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_PERFORM_ACTION,
                VIEW to currentData.view,
                ACTION to mapOf("type" to "system_back"),
            )
        )
        return true
    }

    override fun onProductSelected(product: AdaptyPaywallProduct, context: Context) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_SELECT_PRODUCT,
                VIEW to currentData.view,
                PRODUCT_ID to (product.subscriptionDetails?.basePlanId?.let { basePlanId -> "$basePlanId:${product.vendorProductId}" }
                    ?: product.vendorProductId),
            )
        )
    }

    override fun onPurchaseFailure(
        error: AdaptyError,
        product: AdaptyPaywallProduct,
        context: Context,
    ) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_FAIL_PURCHASE,
                VIEW to currentData.view,
                PRODUCT to product,
                ERROR to error,
            )
        )
    }

    override fun onPurchaseStarted(product: AdaptyPaywallProduct, context: Context) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_START_PURCHASE,
                VIEW to currentData.view,
                PRODUCT to product,
            )
        )
    }

    override fun onPurchaseFinished(
        purchaseResult: AdaptyPurchaseResult,
        product: AdaptyPaywallProduct,
        context: Context
    ) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_FINISH_PURCHASE,
                VIEW to currentData.view,
                PRODUCT to product,
                PURCHASE_RESULT to purchaseResult,
            )
        )
    }

    override fun onLoadingProductsFailure(error: AdaptyError, context: Context): Boolean {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_FAIL_LOADING_PRODUCTS,
                VIEW to currentData.view,
                ERROR to error,
            )
        )
        return retryCounter.incrementAndGet() <= 3
    }

    override fun onError(error: AdaptyError, context: Context) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_RECEIVE_ERROR,
                VIEW to currentData.view,
                ERROR to error,
            )
        )
    }

    override fun onRestoreFailure(error: AdaptyError, context: Context) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_FAIL_RESTORE,
                VIEW to currentData.view,
                ERROR to error,
            )
        )
    }

    override fun onRestoreStarted(context: Context) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_START_RESTORE,
                VIEW to currentData.view,
            )
        )
    }

    override fun onRestoreSuccess(profile: AdaptyProfile, context: Context) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_FINISH_RESTORE,
                VIEW to currentData.view,
                PROFILE to profile,
            )
        )
    }

    override fun onFlowClosed() {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_DISAPPEAR,
                VIEW to currentData.view,
            )
        )
    }

    override fun onFlowShown(context: Context) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_APPEAR,
                VIEW to currentData.view,
            )
        )
    }

    override fun onFinishWebPaymentNavigation(
        product: AdaptyPaywallProduct?,
        error: AdaptyError?,
        context: Context
    ) {
        val data = listOfNotNull(
            VIEW to currentData.view,
            product?.let { PRODUCT to product },
            error?.let { ERROR to error },
        ).toTypedArray()
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_FINISH_WEB_PAYMENT_NAVIGATION,
                *data,
            )
        )
    }

    override fun onShowAppRate(context: Context) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_REQUEST_APP_REVIEW,
                VIEW to currentData.view,
            )
        )
    }

    override fun onAnalyticEvent(name: String, params: Map<String, Any?>, context: Context) {
        onEvent(
            AdaptyUiEvent(
                FLOW_VIEW_DID_RECEIVE_ANALYTIC_EVENT,
                VIEW to currentData.view,
                NAME to name,
                PARAMS to params,
            )
        )
    }

    abstract fun onEvent(event: AdaptyUiEvent)

    internal companion object {
        const val VIEW = "view"
        const val ACTION = "action"
        const val PRODUCT = "product"
        const val PRODUCT_ID = "product_id"
        const val PROFILE = "profile"
        const val PURCHASE_RESULT = "purchased_result"
        const val ERROR = "error"
        const val META = "meta"
        const val EVENT = "event"
        const val ACTION_ID = "action_id"
        const val EVENT_ID = "event_id"
        const val PERMISSION = "permission"
        const val CUSTOM_ARGS = "custom_args"
        const val NAME = "name"
        const val PARAMS = "params"

        const val FLOW_VIEW_DID_PERFORM_ACTION = "flow_view_did_perform_action"
        const val FLOW_VIEW_DID_SELECT_PRODUCT = "flow_view_did_select_product"
        const val FLOW_VIEW_DID_START_PURCHASE = "flow_view_did_start_purchase"
        const val FLOW_VIEW_DID_FINISH_PURCHASE = "flow_view_did_finish_purchase"
        const val FLOW_VIEW_DID_FAIL_PURCHASE = "flow_view_did_fail_purchase"
        const val FLOW_VIEW_DID_START_RESTORE = "flow_view_did_start_restore"
        const val FLOW_VIEW_DID_FINISH_RESTORE = "flow_view_did_finish_restore"
        const val FLOW_VIEW_DID_FAIL_RESTORE = "flow_view_did_fail_restore"
        const val FLOW_VIEW_DID_RECEIVE_ERROR = "flow_view_did_receive_error"
        const val FLOW_VIEW_DID_FAIL_LOADING_PRODUCTS = "flow_view_did_fail_loading_products"
        const val FLOW_VIEW_DID_APPEAR = "flow_view_did_appear"
        const val FLOW_VIEW_DID_DISAPPEAR = "flow_view_did_disappear"
        const val FLOW_VIEW_DID_FINISH_WEB_PAYMENT_NAVIGATION = "flow_view_did_finish_web_payment_navigation"
        const val FLOW_VIEW_DID_ASK_PERMISSION = "flow_view_did_ask_permission"
        const val FLOW_VIEW_DID_REQUEST_APP_REVIEW = "flow_view_did_request_app_review"
        const val FLOW_VIEW_DID_RECEIVE_ANALYTIC_EVENT = "flow_view_did_receive_analytic_event"
        const val FLOW_VIEW_OBSERVER_DID_INITIATE_PURCHASE = "flow_view_observer_did_initiate_purchase"
        const val FLOW_VIEW_OBSERVER_DID_INITIATE_RESTORE = "flow_view_observer_did_initiate_restore"
    }
}