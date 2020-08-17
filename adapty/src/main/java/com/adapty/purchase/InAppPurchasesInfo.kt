package com.adapty.purchase

import android.app.Activity
import android.content.Context
import com.adapty.api.AdaptyPaywallsInfoCallback
import com.adapty.api.entity.containers.DataContainer
import com.adapty.api.entity.containers.Product
import com.adapty.utils.LogHelper
import com.adapty.utils.formatPrice
import com.android.billingclient.api.*
import java.util.regex.Matcher
import java.util.regex.Pattern


class InAppPurchasesInfo(
    var context: Context,
    var purchases: ArrayList<Any>,
    var callback: AdaptyPaywallsInfoCallback
) {

    private var productIterator: MutableIterator<Any> = purchases.iterator()
    private lateinit var billingClient: BillingClient

    init {
        iterator()
    }

    private fun iterator() {

        if (!productIterator.hasNext()) {
            callback.onResult(purchases, null)
            return
        }

        setupBilling(productIterator.next())
    }

    private fun setupBilling(data: Any) {
        if (!::billingClient.isInitialized) {
            billingClient =
                BillingClient.newBuilder(context).enablePendingPurchases()
                    .setListener { billingResult, mutableList ->
                        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                            fail(billingResult.debugMessage)
                        }
                    }
                    .build()
        }
        if (billingClient.isReady) {
            querySkuDetailsInApp(data)
        } else {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        querySkuDetailsInApp(data)
                    } else {
                        fail(billingResult.debugMessage)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    fail("onBillingServiceDisconnected")
                }
            })
        }
    }

    private fun querySkuDetailsInApp(data: Any) {
        billingClient.querySkuDetailsAsync(
            getSkuList(data, INAPP)?.build()
        ) { result, skuDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                fillInfo(skuDetailsList, data)
                querySkuDetailsSubs(data)
            } else
                fail("Unavailable querySkuDetailsInApp: ${result.debugMessage}")
        }
    }

    private fun querySkuDetailsSubs(data: Any) {
        billingClient.querySkuDetailsAsync(
            getSkuList(data, SUBS)?.build()
        ) { result, skuDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                fillInfo(skuDetailsList, data)
                iterator()
            } else
                fail("Unavailable querySkuDetailsSubs: ${result.debugMessage}")
        }
    }

    private fun fillInfo(skuDetailsList: MutableList<SkuDetails>, data: Any) {
        for (skuDetails in skuDetailsList) {
            val sku = skuDetails.sku
            if (data is DataContainer) {
                data.attributes?.products?.let { prods ->
                    for (p in prods) {
                        p.vendorProductId?.let { id ->
                            if (sku == id) {
                                p.setDetails(skuDetails)
                                p.variationId = data.attributes?.variationId
                            }
                        }
                    }
                }
            } else if (data is ArrayList<*>) {
                for (p in (data as ArrayList<Product>)) {
                    p.vendorProductId?.let { id ->
                        if (sku == id) {
                            p.setDetails(skuDetails)
                        }
                    }
                }
            }
        }
    }

    private fun getSkuList(data: Any, type: String): SkuDetailsParams.Builder {
        val skuList = arrayListOf<String>()
        if (data is DataContainer) {
            data.attributes?.products?.let {
                for (p in it) {
                    p.vendorProductId?.let { id ->
                        skuList.add(id)
                    }
                }
            }
        } else if (data is ArrayList<*>) {
            for (p in data) {
                (p as Product).vendorProductId?.let { id ->
                    skuList.add(id)
                }
            }
        }
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList)
            .setType(type)
        return params
    }


    private fun success(purchases: ArrayList<Any>, error: String?) {
        callback.onResult(purchases, error)
    }

    private fun fail(error: String) {
        LogHelper.logError(error)
        callback.onResult(arrayListOf(), error)
    }
}