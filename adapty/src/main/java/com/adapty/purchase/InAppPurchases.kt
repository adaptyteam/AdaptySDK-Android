package com.adapty.purchase

import android.app.Activity
import com.adapty.Adapty
import com.adapty.api.AdaptyCallback
import com.adapty.api.AdaptyPurchaseCallback
import com.adapty.api.AdaptyRestoreCallback
import com.adapty.api.ApiClientRepository
import com.adapty.api.entity.restore.RestoreItem
import com.adapty.api.responses.RestoreReceiptResponse
import com.adapty.api.responses.ValidateReceiptResponse
import com.android.billingclient.api.*

class InAppPurchases(
    var activity: Activity,
    var isRestore: Boolean,
    var purchaseType: String,
    chosenPurchase: String,
    var apiClientRepository: ApiClientRepository?,
    var adaptyCallback: AdaptyCallback
) {

    private lateinit var billingClient: BillingClient

    init {
        setupBilling(chosenPurchase)
    }

    private fun setupBilling(chosenPurchase: String) {
        if (!::billingClient.isInitialized) {
            billingClient =
                BillingClient.newBuilder(activity).enablePendingPurchases()
                    .setListener { billingResult, purchases ->
                        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                            for (purchase in purchases) {

                                if (purchaseType == SUBS) {
                                    val acknowledgePurchaseParams =
                                        AcknowledgePurchaseParams.newBuilder()
                                            .setPurchaseToken(purchase.purchaseToken)
                                            .build()
                                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingRes ->

                                        Adapty.validatePurchase(
                                            purchaseType,
                                            purchase.sku,
                                            purchase.purchaseToken
                                        ) { response, error ->
                                            success(purchase, response, error)
                                        }
                                    }
                                } else {
                                    val consumeParams = ConsumeParams.newBuilder()
                                        .setPurchaseToken(purchase.purchaseToken)
                                        .build()

                                    billingClient.consumeAsync(
                                        consumeParams
                                    ) { p0, p1 ->
                                        Adapty.validatePurchase(
                                            purchaseType,
                                            purchase.sku,
                                            purchase.purchaseToken
                                        ) { response, error ->
                                            success(purchase, response, error)
                                        }
                                    }
                                }
                            }
                        } else if (billingResult?.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                            fail("Purchase: USER_CANCELED")
                        } else {
                            fail("Purchase: ${billingResult?.responseCode.toString()}")
                        }
                    }
                    .build()
        }
        if (billingClient.isReady) {
            if (isRestore) queryPurchaseHistory(SUBS) else querySkuDetails(
                chosenPurchase,
                purchaseType
            )
        } else {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        if (isRestore) queryPurchaseHistory(SUBS) else querySkuDetails(
                            chosenPurchase,
                            purchaseType
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    fail("onBillingServiceDisconnected")
                }
            })
        }
    }

    fun querySkuDetails(chosenPurchase: String, type: String) {
        billingClient.querySkuDetailsAsync(
            getSkuList(
                chosenPurchase,
                type
            )?.build()
        ) { result, skuDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                for (skuDetails in skuDetailsList) {
                    val sku = skuDetails.sku
                    if (sku == chosenPurchase) {
                        val flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .build()
                        val responseCode =
                            billingClient.launchBillingFlow(activity, flowParams)
                        break
                    } else
                        fail("This product_id not found with this purchase type")
                }
            } else
                fail("Unavailable")
        }
    }

    private fun getSkuList(productId: String, type: String): SkuDetailsParams.Builder? {
        val skuList =
            arrayListOf(productId)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList)
            .setType(type)
        return params
    }

    private val historyPurchases = ArrayList<RestoreItem>()

    fun queryPurchaseHistory(type: String) {
        billingClient.queryPurchaseHistoryAsync(type) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchasesList.isNullOrEmpty()) {
                    if (type == INAPP) {
                        if (historyPurchases.isEmpty()) {
                            fail("You have no purchases")
                        } else
                            apiClientRepository?.restore(
                                historyPurchases, object : AdaptyRestoreCallback {
                                    override fun onResult(
                                        response: RestoreReceiptResponse?,
                                        error: String?
                                    ) {
                                        if (error == null) {
                                            success(null, response, error)
                                            return
                                        }

                                        fail(error)
                                    }
                                })
                    } else
                        queryPurchaseHistory(INAPP)
                } else {
                    for (purchase in purchasesList) {
                        val item = RestoreItem()
                        item.isSubscription = type == SUBS
                        item.productId = purchase.sku
                        item.purchaseToken = purchase.purchaseToken
                        historyPurchases.add(item)
                    }

                    if (type != INAPP)
                        queryPurchaseHistory(INAPP)
                    else {
                        if (historyPurchases.isEmpty())
                            fail("You have no purchases")
                        else
                            apiClientRepository?.restore(
                                historyPurchases, object : AdaptyRestoreCallback {
                                    override fun onResult(
                                        response: RestoreReceiptResponse?,
                                        error: String?
                                    ) {
                                        if (error == null) {
                                            success(null, response, error)
                                            return
                                        }

                                        fail(error)
                                    }
                                })
                    }
                }
            } else
                fail("Unavailable")
        }
    }


    private fun success(purchase: Purchase?, response: Any?, error: String?) {
        if (isRestore) {
            (adaptyCallback as AdaptyRestoreCallback).onResult(
                if (response is RestoreReceiptResponse) response else null, error
            )
        } else {
            (adaptyCallback as AdaptyPurchaseCallback).onResult(
                purchase,
                if (response is ValidateReceiptResponse) response else null,
                error
            )
        }
    }

    private fun fail(error: String) {
        if (isRestore) {
            (adaptyCallback as AdaptyRestoreCallback).onResult(null, error)
        } else {
            (adaptyCallback as AdaptyPurchaseCallback).onResult(null, null, error)
        }
    }
}