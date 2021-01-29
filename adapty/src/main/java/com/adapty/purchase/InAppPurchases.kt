package com.adapty.purchase

import android.app.Activity
import android.content.Context
import com.adapty.api.*
import com.adapty.api.entity.paywalls.DataContainer
import com.adapty.api.entity.paywalls.ProductModel
import com.adapty.api.entity.restore.RestoreItem
import com.adapty.api.responses.RestoreReceiptResponse
import com.adapty.api.responses.ValidateReceiptResponse
import com.adapty.utils.LogHelper
import com.adapty.utils.PreferenceManager
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.android.billingclient.api.BillingClient.SkuType.SUBS
import java.lang.ref.WeakReference

class InAppPurchases(
    var context: Context,
    var activity: WeakReference<Activity?>?,
    var isRestore: Boolean,
    var preferenceManager: PreferenceManager,
    var product: ProductModel,
    var variationId: String?,
    var apiClientRepository: ApiClientRepository,
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
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                            for (purchase in purchases) {

                                if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED)
                                    continue

                                if (purchaseType == SUBS) {
                                    val acknowledgePurchaseParams =
                                        AcknowledgePurchaseParams.newBuilder()
                                            .setPurchaseToken(purchase.purchaseToken)
                                            .build()
                                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingRes ->

                                        if (!variationId.isNullOrEmpty())
                                            product.variationId

                                        apiClientRepository.validatePurchase(
                                            purchaseType!!,
                                            purchase.sku,
                                            purchase.purchaseToken,
                                            purchase.orderId,
                                            product,
                                            object : AdaptyValidateCallback {
                                                override fun onResult(
                                                    response: ValidateReceiptResponse?,
                                                    error: AdaptyError?
                                                ) {
                                                    success(purchase, response, error)
                                                }
                                            }
                                        )
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

                                        apiClientRepository.validatePurchase(
                                            purchaseType!!,
                                            purchase.sku,
                                            p1,
                                            purchase.orderId,
                                            product,
                                            object : AdaptyValidateCallback {
                                                override fun onResult(
                                                    response: ValidateReceiptResponse?,
                                                    error: AdaptyError?
                                                ) {
                                                    success(purchase, response, error)
                                                }
                                            }
                                        )
                                    }
                                } else if (purchaseType == null) {
                                    fail(AdaptyError(message = "Product type is null", adaptyErrorCode = AdaptyErrorCode.EMPTY_PARAMETER))
                                }
                            }
                        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                            fail(AdaptyError(message = "Purchase: USER_CANCELED", adaptyErrorCode = AdaptyErrorCode.USER_CANCELED))
                        } else {
                            fail(AdaptyError(message = "Purchase: ${billingResult.responseCode}, ${billingResult.debugMessage}", adaptyErrorCode = AdaptyErrorCode.fromBilling(billingResult.responseCode)))
                        }
                    }
                    .build()
        }
        if (billingClient.isReady) {
            if (isRestore) queryPurchaseHistory(SUBS) else {
                if (chosenPurchase == null) {
                    fail(AdaptyError(message = "Product ID is null", adaptyErrorCode = AdaptyErrorCode.EMPTY_PARAMETER))
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
                                fail(AdaptyError(message = "Product ID is null", adaptyErrorCode = AdaptyErrorCode.EMPTY_PARAMETER))
                                return
                            }
                            querySkuDetails(
                                chosenPurchase,
                                purchaseType
                            )
                        }
                    } else {
                        fail(AdaptyError(message = "onBillingServiceConnectError ${billingResult.responseCode} : ${billingResult.debugMessage}", adaptyErrorCode = AdaptyErrorCode.fromBilling(billingResult.responseCode)))
                    }
                }

                override fun onBillingServiceDisconnected() {
                    fail(AdaptyError(message = "onBillingServiceDisconnected", adaptyErrorCode = AdaptyErrorCode.BILLING_SERVICE_DISCONNECTED))
                }
            })
        }
    }

    fun querySkuDetails(chosenPurchase: String, type: String?) {
        if (type == null) {
            fail(AdaptyError(message = "Product type is null", adaptyErrorCode = AdaptyErrorCode.EMPTY_PARAMETER))
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
                        activity?.get()?.let {
                            billingClient.launchBillingFlow(it, flowParams)
                        }
                        break
                    } else
                        fail(AdaptyError(message = "This product_id not found with this purchase type", adaptyErrorCode = AdaptyErrorCode.PRODUCT_NOT_FOUND))
                }
            } else
                fail(AdaptyError(message = "Unavailable querySkuDetails - ${result.responseCode} : ${result.debugMessage}", adaptyErrorCode = AdaptyErrorCode.fromBilling(result.responseCode)))
        }
    }

    private fun getSkuList(productId: String, type: String): SkuDetailsParams.Builder {
        return SkuDetailsParams.newBuilder()
            .setSkusList(arrayListOf(productId))
            .setType(type)
    }

    private val historyPurchases = ArrayList<RestoreItem>()

    fun queryPurchaseHistory(type: String) {
        if (type == INAPP)
            consumeInapps()

        billingClient.queryPurchaseHistoryAsync(type) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchasesList.isNullOrEmpty()) {
                    if (type == INAPP) {
                        if (historyPurchases.isEmpty()) {
                            fail(AdaptyError(message = "You have no purchases", adaptyErrorCode = AdaptyErrorCode.NO_HISTORY_PURCHASES))
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
                        val item = RestoreItem().apply {
                            isSubscription = type == SUBS
                            productId = purchase.sku
                            purchaseToken = purchase.purchaseToken
                            transactionId = activeSubscriptions?.firstOrNull { it.purchaseToken == purchase.purchaseToken }?.orderId
                        }
                        historyPurchases.add(item)
                    }

                    if (type != INAPP)
                        queryPurchaseHistory(INAPP)
                    else {
                        if (historyPurchases.isEmpty())
                            fail(AdaptyError(message = "You have no purchases", adaptyErrorCode = AdaptyErrorCode.NO_HISTORY_PURCHASES))
                        else {
                            fillProductInfoFromCache()
                            checkPurchasesHistoryForSync(historyPurchases)
                        }
                    }
                }
            } else
                fail(AdaptyError(message = "Unavailable - error code ${billingResult.responseCode} : ${billingResult.debugMessage}", adaptyErrorCode = AdaptyErrorCode.fromBilling(billingResult.responseCode)))
        }
    }

    private fun consumeInapps() {
        billingClient.queryPurchases(INAPP).purchasesList?.let { purchases ->
            val products = preferenceManager.products

            for (purchase in purchases) {
                if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED)
                    continue

                products.firstOrNull { it.vendorProductId == purchase.sku }?.let { product ->
                    billingClient.consumeAsync(
                        ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    ) { _, purchaseToken ->
                        apiClientRepository.validatePurchase(
                            INAPP,
                            purchase.sku,
                            purchaseToken,
                            purchase.orderId,
                            product
                        )
                    }
                }
            }
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
        prods: ArrayList<ProductModel>,
        id: String
    ): ProductModel? {
        containers?.forEach { container ->
            container.attributes?.products?.forEach { product ->
                if (product.vendorProductId == id) {
                    return product
                }
            }
        }
        return prods.firstOrNull { it.vendorProductId == id }
    }

    private fun checkPurchasesHistoryForSync(historyPurchases: ArrayList<RestoreItem>) {
        val savedPurchases = preferenceManager.syncedPurchases

        if (savedPurchases.isEmpty() && historyPurchases.isNotEmpty() ||
            savedPurchases.size < historyPurchases.size
        ) {
            apiClientRepository.restore(
                historyPurchases, object : AdaptyRestoreCallback {
                    override fun onResult(
                        response: RestoreReceiptResponse?,
                        error: AdaptyError?
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
                val notSynced = historyPurchases.filterTo(arrayListOf()) { !savedPurchases.contains(it) }

                apiClientRepository.restore(
                    notSynced, object : AdaptyRestoreCallback {
                        override fun onResult(
                            response: RestoreReceiptResponse?,
                            error: AdaptyError?
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
                fail(AdaptyError(message = "No new purchases", adaptyErrorCode = AdaptyErrorCode.NO_NEW_PURCHASES))
        }
    }

    private fun success(purchase: Purchase?, response: Any?, error: AdaptyError?) {
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

    private fun fail(error: AdaptyError) {
        LogHelper.logError(error.message)
        if (isRestore) {
            (adaptyCallback as AdaptyRestoreCallback).onResult(null, error)
        } else {
            (adaptyCallback as AdaptyPurchaseCallback).onResult(null, null, error)
        }
    }
}