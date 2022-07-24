package com.adapty

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.AdaptyInternal
import com.adapty.internal.di.Dependencies
import com.adapty.internal.di.Dependencies.inject
import com.adapty.internal.utils.Logger
import com.adapty.listeners.OnPaywallsForConfigReceivedListener
import com.adapty.listeners.OnPromoReceivedListener
import com.adapty.listeners.OnPurchaserInfoUpdatedListener
import com.adapty.listeners.VisualPaywallListener
import com.adapty.models.*
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.ProfileParameterBuilder
import java.util.concurrent.locks.ReentrantReadWriteLock

public object Adapty {

    @JvmStatic
    @JvmOverloads
    public fun activate(
        context: Context,
        appKey: String,
        customerUserId: String? = null,
        observerMode: Boolean = false,
    ) {
        Logger.logMethodCall { "activate($appKey, ${customerUserId.orEmpty()})" }

        require(appKey.isNotBlank()) { "Public SDK key must not be empty." }
        require(context.applicationContext is Application) { "Application context must be provided." }
        require(context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) { "INTERNET permission must be granted." }

        if (isActivated) {
            Logger.logError { "Adapty was already activated. If you want to provide new customerUserId, please call 'identify' function instead." }
            return
        }

        init(context, appKey, observerMode)
        adaptyInternal.activate(customerUserId, null)
    }

    @JvmStatic
    public fun identify(customerUserId: String, adaptyCallback: (error: AdaptyError?) -> Unit) {
        Logger.logMethodCall { "identify($customerUserId)" }
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.identify(customerUserId, adaptyCallback)
    }

    @JvmStatic
    public fun updateProfile(
        params: ProfileParameterBuilder,
        adaptyCallback: (error: AdaptyError?) -> Unit
    ) {
        Logger.logMethodCall { "updateProfile()" }
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.updateProfile(params, adaptyCallback)
    }

    @JvmStatic
    @JvmOverloads
    public fun getPurchaserInfo(
        forceUpdate: Boolean = false,
        adaptyCallback: (purchaserInfo: PurchaserInfoModel?, error: AdaptyError?) -> Unit
    ) {
        Logger.logMethodCall { "getPurchaserInfo(forceUpdate = $forceUpdate)" }
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(null, notInitializedError)
            return
        }
        adaptyInternal.getPurchaserInfo(forceUpdate, adaptyCallback)
    }

    @JvmStatic
    @JvmOverloads
    public fun getPaywalls(
        forceUpdate: Boolean = false,
        adaptyCallback: (paywalls: List<PaywallModel>?, products: List<ProductModel>?, error: AdaptyError?) -> Unit
    ) {
        Logger.logMethodCall { "getPaywalls(forceUpdate = $forceUpdate)" }
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(null, null, notInitializedError)
            return
        }
        adaptyInternal.getPaywalls(forceUpdate, adaptyCallback)
    }

    @JvmStatic
    public fun getPromo(
        adaptyCallback: (promo: PromoModel?, error: AdaptyError?) -> Unit
    ) {
        Logger.logMethodCall { "getPromo()" }
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(null, notInitializedError)
            return
        }
        adaptyInternal.getPromo(adaptyCallback)
    }

    @JvmStatic
    @JvmOverloads
    public fun makePurchase(
        activity: Activity,
        product: ProductModel,
        subscriptionUpdateParams: SubscriptionUpdateParamModel? = null,
        adaptyCallback: (purchaserInfo: PurchaserInfoModel?, purchaseToken: String?, googleValidationResult: GoogleValidationResult?, product: ProductModel, error: AdaptyError?) -> Unit
    ) {
        Logger.logMethodCall { "makePurchase(vendorProductId = ${product.vendorProductId}${subscriptionUpdateParams?.let { "; oldVendorProductId = ${it.oldSubVendorProductId}; prorationMode = ${it.prorationMode}" }.orEmpty()})" }
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(null, null, null, product, notInitializedError)
            return
        }
        adaptyInternal.makePurchase(activity, product, subscriptionUpdateParams, adaptyCallback)
    }

    @JvmStatic
    public fun restorePurchases(
        adaptyCallback: (purchaserInfo: PurchaserInfoModel?, googleValidationResultList: List<GoogleValidationResult>?, error: AdaptyError?) -> Unit
    ) {
        Logger.logMethodCall { "restorePurchases()" }
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(null, null, notInitializedError)
            return
        }
        adaptyInternal.restorePurchases(adaptyCallback)
    }

    @JvmStatic
    @JvmOverloads
    public fun updateAttribution(
        attribution: Any,
        source: AttributionType,
        networkUserId: String? = null,
        adaptyCallback: (error: AdaptyError?) -> Unit
    ) {
        Logger.logMethodCall { "updateAttribution(source = $source)" }
        if (BuildConfig.DEBUG && source == AttributionType.APPSFLYER) {
            require(networkUserId != null) { "networkUserId is required for AppsFlyer attribution, otherwise we won't be able to send specific events." }
        }
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.updateAttribution(attribution, source, networkUserId, adaptyCallback)
    }

    @JvmStatic
    public fun setExternalAnalyticsEnabled(
        enabled: Boolean,
        adaptyCallback: (error: AdaptyError?) -> Unit
    ) {
        Logger.logMethodCall { "setExternalAnalyticsEnabled($enabled)" }
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.setExternalAnalyticsEnabled(enabled, adaptyCallback)
    }

    @JvmStatic
    public fun setTransactionVariationId(
        transactionId: String,
        variationId: String,
        adaptyCallback: (error: AdaptyError?) -> Unit
    ) {
        Logger.logMethodCall { "setTransactionVariationId(transactionId = $transactionId, variationId = $variationId)" }
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.setTransactionVariationId(transactionId, variationId, adaptyCallback)
    }

    @JvmStatic
    public fun logout(adaptyCallback: (error: AdaptyError?) -> Unit) {
        Logger.logMethodCall { "logout()" }
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.logout(adaptyCallback)
    }

    @JvmStatic
    public fun refreshPushToken(newToken: String) {
        if (!checkActivated()) return
        adaptyInternal.refreshPushToken(newToken)
    }

    @JvmStatic
    public fun handlePromoIntent(
        intent: Intent?,
        adaptyCallback: (promo: PromoModel?, error: AdaptyError?) -> Unit
    ): Boolean {
        if (intent?.getStringExtra("source") != "adapty") {
            return false
        }
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(null, notInitializedError)
            return false
        }
        adaptyInternal.handlePromoIntent(intent, adaptyCallback)
        return true
    }

    @JvmStatic
    public fun setOnPurchaserInfoUpdatedListener(onPurchaserInfoUpdatedListener: OnPurchaserInfoUpdatedListener?) {
        if (!checkActivated()) return
        adaptyInternal.onPurchaserInfoUpdatedListener = onPurchaserInfoUpdatedListener
    }

    @JvmStatic
    public fun setOnPromoReceivedListener(onPromoReceivedListener: OnPromoReceivedListener?) {
        if (!checkActivated()) return
        adaptyInternal.onPromoReceivedListener = onPromoReceivedListener
    }

    @JvmStatic
    public fun setOnPaywallsForConfigReceivedListener(onPaywallsForConfigReceivedListener: OnPaywallsForConfigReceivedListener?) {
        if (!checkActivated()) return
        adaptyInternal.onPaywallsForConfigReceivedListener = onPaywallsForConfigReceivedListener
    }

    @JvmStatic
    public fun setVisualPaywallListener(visualPaywallListener: VisualPaywallListener?) {
        if (!checkActivated()) return
        adaptyInternal.setVisualPaywallListener(visualPaywallListener)
    }

    @JvmStatic
    public fun setLogLevel(logLevel: AdaptyLogLevel) {
        Logger.logLevel = logLevel.value
    }

    @JvmStatic
    @JvmOverloads
    public fun setFallbackPaywalls(
        paywalls: String,
        adaptyCallback: ((error: AdaptyError?) -> Unit)? = null
    ) {
        Logger.logMethodCall { "setFallbackPaywalls()" }
        adaptyCallback?.let { callback ->
            if (!checkActivated(callback)) return
        } ?: if (!checkActivated()) return
        adaptyInternal.setFallbackPaywalls(paywalls, adaptyCallback)
    }

    @JvmStatic
    public fun showVisualPaywall(
        activity: Activity,
        paywall: PaywallModel,
    ) {
        Logger.logMethodCall { "showVisualPaywall()" }
        if (!checkActivated()) return
        adaptyInternal.showVisualPaywall(activity, paywall)
    }

    @JvmStatic
    public fun closeVisualPaywall() {
        Logger.logMethodCall { "closeVisualPaywall()" }
        if (!checkActivated()) return
        adaptyInternal.closeVisualPaywall()
    }

    @JvmStatic
    public fun logShowPaywall(paywall: PaywallModel) {
        if (!checkActivated()) return
        adaptyInternal.logShowPaywall(paywall)
    }

    private val adaptyInternal: AdaptyInternal by inject()

    private val lock = ReentrantReadWriteLock()
    private var isActivated = false
        get() = try {
            lock.readLock().lock()
            field
        } finally {
            lock.readLock().unlock()
        }

    private val notInitializedError = AdaptyError(
        message = "Adapty was not initialized",
        adaptyErrorCode = AdaptyErrorCode.ADAPTY_NOT_INITIALIZED
    )

    private fun init(context: Context, appKey: String, observerMode: Boolean) {
        try {
            lock.writeLock().lock()
            Dependencies.init(context.applicationContext)
            adaptyInternal.init(appKey, observerMode)
            isActivated = true
        } finally {
            lock.writeLock().unlock()
        }
    }

    private fun checkActivated(adaptyCallback: (error: AdaptyError?) -> Unit): Boolean {
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(notInitializedError)
            return false
        }
        return true
    }

    private fun checkActivated(): Boolean {
        if (!isActivated) {
            logNotInitializedError()
            return false
        }
        return true
    }

    private fun logNotInitializedError() {
        Logger.logError { "${notInitializedError.message}" }
    }
}