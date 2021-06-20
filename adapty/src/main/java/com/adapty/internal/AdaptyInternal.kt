package com.adapty.internal

import android.app.Activity
import android.content.Intent
import androidx.annotation.RestrictTo
import com.adapty.R
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cloud.KinesisManager
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.domain.AuthInteractor
import com.adapty.internal.domain.ProductsInteractor
import com.adapty.internal.domain.PurchaserInteractor
import com.adapty.internal.domain.PurchasesInteractor
import com.adapty.internal.utils.*
import com.adapty.listeners.OnPromoReceivedListener
import com.adapty.listeners.OnPurchaserInfoUpdatedListener
import com.adapty.listeners.VisualPaywallListener
import com.adapty.models.*
import com.adapty.utils.ProfileParameterBuilder
import com.adapty.visual.VisualPaywallActivity
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AdaptyInternal(
    private val authInteractor: AuthInteractor,
    private val purchaserInteractor: PurchaserInteractor,
    private val purchasesInteractor: PurchasesInteractor,
    private val productsInteractor: ProductsInteractor,
    private val storeManager: StoreManager,
    private val kinesisManager: KinesisManager,
    private val periodicRequestManager: AdaptyPeriodicRequestManager,
    private val lifecycleManager: AdaptyLifecycleManager,
    private val visualPaywallManager: VisualPaywallManager,
) {

    @get:JvmSynthetic
    @set:JvmSynthetic
    var onPurchaserInfoUpdatedListener: OnPurchaserInfoUpdatedListener? = null
        set(value) {
            execute {
                purchaserInteractor
                    .subscribeOnPurchaserInfoChanges()
                    .onEach { value?.onPurchaserInfoReceived(it) }
                    .catch { }
                    .flowOnMain()
                    .collect()
            }
            field = value
        }

    @get:JvmSynthetic
    @set:JvmSynthetic
    var onPromoReceivedListener: OnPromoReceivedListener? = null
        set(value) {
            execute {
                purchaserInteractor
                    .subscribeOnPromoChanges()
                    .onEach { value?.onPromoReceived(it) }
                    .catch { }
                    .flowOnMain()
                    .collect()
            }
            field = value
        }

    fun init(appKey: String) {
        authInteractor.saveAppKey(appKey)
        lifecycleManager.init()
    }


    @JvmSynthetic
    fun getPurchaserInfo(
        forceUpdate: Boolean,
        callback: (purchaserInfo: PurchaserInfoModel?, error: AdaptyError?) -> Unit
    ) {
        execute {
            purchaserInteractor
                .getPurchaserInfo(forceUpdate)
                .onEach { purchaserInfo -> callback(purchaserInfo, null) }
                .catch { error ->
                    callback(
                        null,
                        error.asAdaptyError()
                    )
                }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun updateProfile(
        params: ProfileParameterBuilder,
        callback: (error: AdaptyError?) -> Unit
    ) {
        execute {
            purchaserInteractor
                .updateProfile(params)
                .onEach { callback.invoke(null) }
                .catch { error -> callback.invoke(error.asAdaptyError()) }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun activate(
        customerUserId: String?,
        callback: ((AdaptyError?) -> Unit)?
    ) {
        execute {
            authInteractor
                .activate(customerUserId)
                .flatMapConcat { executeStartRequests() }
                .flowOnIO()
                .onEach {
                    authInteractor.unblockRequests()
                    callback?.invoke(null)
                }
                .catch { error ->
                    authInteractor.unblockRequests()
                    callback?.invoke(error.asAdaptyError())
                }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun identify(customerUserId: String, callback: (error: AdaptyError?) -> Unit) {
        if (customerUserId.isBlank()) {
            Logger.logError { "customerUserId should not be empty" }
            callback.invoke(
                AdaptyError(
                    message = "customerUserId should not be empty",
                    adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
                )
            )
            return
        } else if (customerUserId == authInteractor.getCustomerUserId()) {
            callback.invoke(null)
            return
        }

        execute {
            authInteractor.blockRequests()

            authInteractor
                .identify(customerUserId)
                .flatMapConcat { shouldExecuteStartRequests ->
                    if (shouldExecuteStartRequests)
                        executeStartRequests()
                    else
                        flowOf(null)
                }
                .flowOnIO()
                .onEach {
                    authInteractor.unblockRequests()
                    callback.invoke(null)
                }
                .catch { error ->
                    authInteractor.unblockRequests()
                    callback.invoke(error.asAdaptyError())
                }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun logout(callback: (error: AdaptyError?) -> Unit) {
        authInteractor.clearDataOnLogout()
        authInteractor.blockRequests()
        activate(null, callback)
    }

    @JvmSynthetic
    fun makePurchase(
        activity: Activity,
        product: ProductModel,
        callback: (purchaserInfo: PurchaserInfoModel?, purchaseToken: String?, googleValidationResult: GoogleValidationResult?, product: ProductModel, error: AdaptyError?) -> Unit
    ) {

        val productId = product.vendorProductId
        val purchaseType = product.skuDetails?.type

        if (purchaseType == null) {
            callback.invoke(
                null,
                null,
                null,
                product,
                AdaptyError(
                    message = "Product type is null",
                    adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
                )
            )
            return
        }

        storeManager.makePurchase(activity, productId, purchaseType) { purchase, error ->
            if (error != null) {
                callback.invoke(null, null, null, product, error)
            } else {
                purchase?.let { purchase ->
                    execute {
                        purchasesInteractor.validatePurchase(
                            purchaseType,
                            purchase,
                            product
                        ).onEach { (purchaserInfo, validationResult) ->
                            callback.invoke(
                                purchaserInfo,
                                purchase.purchaseToken,
                                validationResult,
                                product,
                                null
                            )
                        }.catch {
                            callback.invoke(
                                null,
                                purchase.purchaseToken,
                                null,
                                product,
                                error
                            )
                        }
                            .flowOnMain()
                            .collect()
                    }
                }
                    ?: callback.invoke(
                        null,
                        null,
                        null,
                        product,
                        AdaptyError(
                            message = "Purchase is null",
                            adaptyErrorCode = AdaptyErrorCode.UNKNOWN
                        )
                    )
            }
        }
    }

    @JvmSynthetic
    fun restorePurchases(callback: (purchaserInfo: PurchaserInfoModel?, googleValidationResultList: List<GoogleValidationResult>?, error: AdaptyError?) -> Unit) {
        execute {
            purchasesInteractor
                .restorePurchases()
                .onEach { (purchaserInfo, validationResultList) ->
                    callback.invoke(purchaserInfo, validationResultList, null)
                }
                .catch { error ->
                    callback.invoke(null, null, error.asAdaptyError())
                }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun getPaywalls(
        forceUpdate: Boolean,
        callback: (paywalls: List<PaywallModel>?, products: List<ProductModel>?, error: AdaptyError?) -> Unit
    ) {
        execute {
            productsInteractor
                .getPaywalls(forceUpdate)
                .onEach { (paywalls, products) -> callback(paywalls, products, null) }
                .catch { error ->
                    callback(null, null, error.asAdaptyError())
                }
                .flowOnMain()
                .collect()
        }
    }

    private fun executeStartRequests(): Flow<*> {
        return authInteractor.onActivateAllowed()
            .flatMapConcat {
                purchaserInteractor.syncMetaOnStart()
                    .onEach { periodicRequestManager.startPeriodicRequests() }
            }
            .flatMapConcat {
                productsInteractor.getPaywallsOnStart()
                    .zip(productsInteractor.getPromoOnStart()) { _, _ -> }
                    .zip(
                        purchasesInteractor.syncPurchasesOnStart()
                            .flatMapConcat { purchaserInteractor.getPurchaserInfoOnStart() }
                    ) { _, _ -> }
            }
            .onEach {
                purchaserInteractor.syncAttributions()
                purchasesInteractor.consumeAndAcknowledgeTheUnprocessed()
            }
    }

    @JvmSynthetic
    fun getPromo(callback: (promo: PromoModel?, error: AdaptyError?) -> Unit) {
        execute {
            productsInteractor
                .getPromo()
                .onEach { promo -> callback(promo, null) }
                .catch { error -> callback(null, error.asAdaptyError()) }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun setFallbackPaywalls(paywalls: String, callback: ((error: AdaptyError?) -> Unit)?) {
        productsInteractor.setFallbackPaywalls(paywalls).let { error -> callback?.invoke(error) }
    }

    @JvmSynthetic
    fun showPaywall(
        activity: Activity,
        paywall: PaywallModel
    ) {
        activity.runOnUiThread {
            activity.startActivity(
                Intent(activity, VisualPaywallActivity::class.java)
                    .putExtra(VisualPaywallActivity.PAYWALL_ID_EXTRA, paywall.variationId)
            )
            activity.overridePendingTransition(
                R.anim.adapty_paywall_slide_up,
                R.anim.adapty_paywall_no_anim
            )
        }
    }

    @JvmSynthetic
    fun closePaywall() {
        visualPaywallManager.closePaywall()
    }

    @JvmSynthetic
    fun setVisualPaywallListener(visualPaywallListener: VisualPaywallListener?) {
        visualPaywallManager.listener = visualPaywallListener
    }

    @JvmSynthetic
    fun handlePromoIntent(
        intent: Intent,
        adaptyCallback: (promo: PromoModel?, error: AdaptyError?) -> Unit
    ) {
        kinesisManager.trackEvent(
            "promo_push_opened",
            mapOf("promo_delivery_id" to intent.getStringExtra("promo_delivery_id"))
        )
        getPromo(adaptyCallback)
    }

    @JvmSynthetic
    fun logShowPaywall(paywall: PaywallModel) {
        kinesisManager.trackEvent(
            "paywall_showed",
            mapOf(
                "is_promo" to "${paywall.isPromo}",
                "variation_id" to paywall.variationId
            )
        )
    }

    @JvmSynthetic
    fun updateAttribution(
        attribution: Any,
        source: AttributionType,
        networkUserId: String?,
        callback: (error: AdaptyError?) -> Unit
    ) {
        execute {
            purchaserInteractor
                .updateAttribution(attribution, source, networkUserId)
                .onEach { callback.invoke(null) }
                .catch { error -> callback.invoke(error.asAdaptyError()) }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun setTransactionVariationId(
        transactionId: String,
        variationId: String,
        callback: (error: AdaptyError?) -> Unit
    ) {
        execute {
            purchasesInteractor
                .setTransactionVariationId(transactionId, variationId)
                .onEach { callback.invoke(null) }
                .catch { error -> callback.invoke(error.asAdaptyError()) }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun setExternalAnalyticsEnabled(
        enabled: Boolean,
        callback: (error: AdaptyError?) -> Unit
    ) {
        execute {
            purchaserInteractor
                .setExternalAnalyticsEnabled(enabled)
                .onEach { callback.invoke(null) }
                .catch { error -> callback.invoke(error.asAdaptyError()) }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun refreshPushToken(newToken: String) {
        execute {
            purchaserInteractor
                .refreshPushToken(newToken)
                .catch { }
                .collect()
        }
    }
}