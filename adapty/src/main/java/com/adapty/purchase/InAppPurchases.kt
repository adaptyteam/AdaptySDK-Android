package com.adapty.purchase

import android.app.Activity
import com.adapty.R
import com.adapty.api.AdaptyCallback
import com.adapty.api.AdaptyPurchaseCallback
import com.adapty.api.AdaptyRestoreCallback
import com.android.billingclient.api.*
import java.util.concurrent.TimeUnit

class InAppPurchases(var activity: Activity, var isRestore: Boolean, var purchaseType: String, chosenPurchase: String, var adaptyCallback: AdaptyCallback) {

    private lateinit var billingClient: BillingClient

    init {
        setupBilling(chosenPurchase)
    }

    fun setupBilling(chosenPurchase: String) {
        if (!::billingClient.isInitialized) {
            billingClient =
                BillingClient.newBuilder(activity).enablePendingPurchases()
                    .setListener { billingResult, purchases ->
                        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                            for (purchase in purchases) {
                                success(purchase)
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
            if (isRestore) queryPurchaseHistory() else querySkuDetails(chosenPurchase, purchaseType)
        } else {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        if (isRestore) queryPurchaseHistory() else querySkuDetails(chosenPurchase, purchaseType)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    fail("onBillingServiceDisconnected")
                }
            })
        }
    }

    fun querySkuDetails(chosenPurchase: String, type: String) {
        billingClient.querySkuDetailsAsync(getSkuList(chosenPurchase, type)?.build()) { result, skuDetailsList ->
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
            }
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

    fun queryPurchaseHistory() {
        billingClient.queryPurchaseHistoryAsync(purchaseType) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchasesList.isNullOrEmpty())
                    fail("You have no purchases")
                else
                    success(null)
            }
        }
    }


    private fun success(purchase: Purchase?) {
        if (isRestore) {
            (adaptyCallback as AdaptyRestoreCallback).onResult(null)
        } else {
            (adaptyCallback as AdaptyPurchaseCallback).onResult(purchase, null)
        }
    }

    private fun fail(error: String) {
        if (isRestore) {
            (adaptyCallback as AdaptyRestoreCallback).onResult(error)
        } else {
            (adaptyCallback as AdaptyPurchaseCallback).onResult(null, error)
        }
    }
}