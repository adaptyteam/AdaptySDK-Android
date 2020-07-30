package com.adapty.purchase

import android.app.Activity
import android.content.Context
import com.adapty.Adapty
import com.adapty.api.AdaptyCallback
import com.adapty.api.AdaptyPurchaseCallback
import com.adapty.api.AdaptyRestoreCallback
import com.adapty.api.ApiClientRepository
import com.adapty.api.entity.containers.DataContainer
import com.adapty.api.entity.containers.Product
import com.adapty.api.entity.restore.RestoreItem
import com.adapty.api.responses.RestoreReceiptResponse
import com.adapty.api.responses.ValidateReceiptResponse
import com.adapty.utils.LogHelper
import com.adapty.utils.PreferenceManager
import com.android.billingclient.api.*

class InAppPurchases(
    var context: Context,
    var activity: Activity?,
    var isRestore: Boolean,
    var preferenceManager: PreferenceManager,
    var product: Product,
    var variationId: String?,
    var apiClientRepository: ApiClientRepository?,
    var adaptyCallback: AdaptyCallback
) {

    private lateinit var billingClient: BillingClient
    private var purchaseType = product.skuDetails?.type

    init {
        setupBilling(product.vendorProductId)
    }

    private fun setupBilling(chosenPurchase: String?) {
        if (!::billingClient.isInitialized) {
            billingClient =
                BillingClient.newBuilder(context).enablePendingPurchases()
                    .setListener { billingResult, purchases ->
                        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                            for (purchase in purchases) {

                                if (purchaseType == SUBS) {
                                    val acknowledgePurchaseParams =
                                        AcknowledgePurchaseParams.newBuilder()
                                            .setPurchaseToken(purchase.purchaseToken)
                                            .build()
                                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingRes ->

                                        if (!variationId.isNullOrEmpty())
                                            product.variationId

                                        Adapty.validatePurchase(
                                            purchaseType!!,
                                            purchase.sku,
                                            purchase.purchaseToken,
                                            purchase.orderId,
                                            product
                                        ) { response, error ->
                                            success(purchase, response, error)
                                        }
                                    }
                                } else if (purchaseType == INAPP) {
                                    val consumeParams = ConsumeParams.newBuilder()
                                        .setPurchaseToken(purchase.purchaseToken)
                                        .build()

                                    billingClient.consumeAsync(
                                        consumeParams
                                    ) { p0, p1 ->

                                        if (!variationId.isNullOrEmpty())
                                            product.variationId

                                        Adapty.validatePurchase(
                                            purchaseType!!,
                                            purchase.sku,
                                            p1,
                                            purchase.orderId,
                                            product
                                        ) { response, error ->
                                            success(purchase, response, error)
                                        }
                                    }
                                } else if (purchaseType == null) {
                                    fail("Product type is null")
                                }
                            }
                        } else if (billingResult?.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                            fail("Purchase: USER_CANCELED")
                        } else {
                            fail("Purchase: ${billingResult?.responseCode.toString()}, ${billingResult.debugMessage}")
                        }
                    }
                    .build()
        }
        if (billingClient.isReady) {
            if (isRestore) queryPurchaseHistory(SUBS) else {
                if (chosenPurchase == null) {
                    fail("Product Id is null")
                    return
                }
                querySkuDetails(
                    chosenPurchase,
                    purchaseType
                )
            }
        } else {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        if (isRestore) queryPurchaseHistory(SUBS) else {
                            if (chosenPurchase == null) {
                                fail("Product Id is null")
                                return
                            }
                            querySkuDetails(
                                chosenPurchase,
                                purchaseType
                            )
                        }
                    } else {
                        fail("onBillingServiceConnectError ${billingResult.responseCode} : ${billingResult.debugMessage ?: ""}")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    fail("onBillingServiceDisconnected")
                }
            })
        }
    }

    fun querySkuDetails(chosenPurchase: String, type: String?) {
        if (type == null) {
            fail("Product type is null")
            return
        }
        billingClient.querySkuDetailsAsync(
            getSkuList(
                chosenPurchase,
                type
            ).build()
        ) { result, skuDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                for (skuDetails in skuDetailsList) {
                    val sku = skuDetails.sku
                    if (sku == chosenPurchase) {
                        val flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .build()
                        activity?.let {
                            billingClient.launchBillingFlow(it, flowParams)
                        }
                        break
                    } else
                        fail("This product_id not found with this purchase type")
                }
            } else
                fail("Unavailable querySkuDetails - ${result.responseCode} : ${result.debugMessage ?: ""}")
        }
    }

    private fun getSkuList(productId: String, type: String): SkuDetailsParams.Builder {
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
                        } else {
                            fillProductInfoFromCache()
                            checkPurchasesHistoryForSync(historyPurchases)
                        }
                    } else
                        queryPurchaseHistory(INAPP)
                } else {
                    var activeSubscriptions: List<Purchase>? = null
                    if (type == SUBS) {
                        activeSubscriptions = billingClient.queryPurchases(SUBS).purchasesList
                    }
                    for (purchase in purchasesList) {
                        val item = RestoreItem()
                        item.isSubscription = type == SUBS
                        item.productId = purchase.sku
                        item.purchaseToken = purchase.purchaseToken
                        activeSubscriptions?.forEach { activeSubscription ->
                            if (activeSubscription.purchaseToken == purchase.purchaseToken) {
                                item.transactionId = activeSubscription.orderId
                                return@forEach
                            }
                        }
                        historyPurchases.add(item)
                    }

                    if (type != INAPP)
                        queryPurchaseHistory(INAPP)
                    else {
                        if (historyPurchases.isEmpty())
                            fail("You have no purchases")
                        else {
                            fillProductInfoFromCache()
                            checkPurchasesHistoryForSync(historyPurchases)
                        }
                    }
                }
            } else
                fail("Unavailable - error code ${billingResult.responseCode} : ${billingResult.debugMessage ?: ""}")
        }
    }

    private fun fillProductInfoFromCache() {
        for (i in 0 until historyPurchases.size) {
            historyPurchases[i].productId?.let { productId ->
                val containers = preferenceManager.containers
                val products = preferenceManager.products
                val product = getElementFromContainers(containers, products, productId)

                product?.let { p ->
                    historyPurchases[i].setDetails(product.skuDetails)
                    historyPurchases[i].localizedTitle = product.localizedTitle
                }
            }
        }
    }

    private fun getElementFromContainers(
        containers: ArrayList<DataContainer>?,
        prods: ArrayList<Product>,
        id: String
    ): Product? {
        containers?.let {
            for (i in 0 until containers.size) {
                containers[i].attributes?.products?.let { products ->
                    for (p in products) {
                        if (p.vendorProductId.equals(id)) {
                            return p
                        }
                    }
                }
            }
        }
        for (i in 0 until prods.size) {
            if (prods[i].vendorProductId.equals(id))
                return prods[i]
        }
        return null
    }

    private fun checkPurchasesHistoryForSync(historyPurchases: ArrayList<RestoreItem>) {
        val savedPurchases = preferenceManager.syncedPurchases

        if (savedPurchases.isEmpty() && historyPurchases.isNotEmpty() ||
            savedPurchases.size < historyPurchases.size
        ) {
            apiClientRepository?.restore(
                historyPurchases, object : AdaptyRestoreCallback {
                    override fun onResult(
                        response: RestoreReceiptResponse?,
                        error: String?
                    ) {
                        if (error == null) {
                            preferenceManager.syncedPurchases = historyPurchases
                            success(null, response, error)
                            return
                        }

                        fail(error)
                    }
                })
        } else {
            if (savedPurchases != historyPurchases) {
                val notSynced = arrayListOf<RestoreItem>()
                for (hp in historyPurchases) {
                    if (!savedPurchases.contains(hp))
                        notSynced.add(hp)
                }

                apiClientRepository?.restore(
                    notSynced, object : AdaptyRestoreCallback {
                        override fun onResult(
                            response: RestoreReceiptResponse?,
                            error: String?
                        ) {
                            if (error == null) {
                                preferenceManager.syncedPurchases = historyPurchases
                                success(null, response, error)
                                return
                            }

                            fail(error)
                        }
                    })
            } else
                fail("No new purchases")
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
        LogHelper.logError(error)
        if (isRestore) {
            (adaptyCallback as AdaptyRestoreCallback).onResult(null, error)
        } else {
            (adaptyCallback as AdaptyPurchaseCallback).onResult(null, null, error)
        }
    }
}