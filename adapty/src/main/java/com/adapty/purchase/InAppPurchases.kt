package com.adapty.purchase

import android.app.Activity
import com.adapty.R
import com.adapty.api.AdaptyPurchaseCallback
import com.android.billingclient.api.*
import java.util.concurrent.TimeUnit

class InAppPurchases(var activity: Activity, var purchaseType: String, chosenPurchase: String, var adaptyCallback: AdaptyPurchaseCallback) {

    private lateinit var billingClient: BillingClient

    fun setupBilling(chosenPurchase: String, isRestore: Boolean) {
        if (!::billingClient.isInitialized) {
            billingClient =
                BillingClient.newBuilder(activity).enablePendingPurchases()
                    .setListener { billingResult, purchases ->
                        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                            for (purchase in purchases) {
                                adaptyCallback.success(purchase, -1)
                            }
                        } else if (billingResult?.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                            adaptyCallback.fail("Purchase: USER_CANCELED", -1)
                        } else {
                            adaptyCallback.fail("Purchase: ${billingResult?.responseCode.toString()}", -1)
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
                    adaptyCallback.fail("onBillingServiceDisconnected", -1)
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
                    } else if ("gas" == sku) {
                    }
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
                    adaptyCallback.fail("You have no purchases", -1)
                else
                    adaptyCallback.success(null, -1)
            }
        }
    }
}