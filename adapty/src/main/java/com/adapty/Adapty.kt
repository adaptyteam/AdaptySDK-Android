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
import com.adapty.listeners.OnPromoReceivedListener
import com.adapty.listeners.OnPurchaserInfoUpdatedListener
import com.adapty.listeners.VisualPaywallListener
import com.adapty.models.*
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.ProfileParameterBuilder
import java.util.concurrent.locks.ReentrantReadWriteLock

object Adapty {

    @JvmStatic
    @JvmOverloads
    fun activate(
        context: Context,
        appKey: String,
        customerUserId: String? = null,
        adaptyCallback: ((error: AdaptyError?) -> Unit)? = null
    ) {
        Logger.logVerbose { "activate($appKey, ${customerUserId ?: ""})" }

        require(appKey.isNotBlank()) { "Public SDK key must not be empty." }
        require(context.applicationContext is Application) { "Application context must be provided." }
        require(context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) { "INTERNET permission must be granted." }

        if (isActivated) {
            Logger.logVerbose { "Adapty was already activated. If you want to provide new customerUserId, please call 'identify' function instead." }
            adaptyCallback?.invoke(null)
            return
        }

        init(context, appKey)
        adaptyInternal.activate(customerUserId, adaptyCallback)
    }

    @JvmStatic
    fun identify(customerUserId: String, adaptyCallback: (error: AdaptyError?) -> Unit) {
        Logger.logVerbose { "identify($customerUserId)" }
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.identify(customerUserId, adaptyCallback)
    }

    @JvmStatic
    fun updateProfile(
        params: ProfileParameterBuilder,
        adaptyCallback: (error: AdaptyError?) -> Unit
    ) {
        Logger.logVerbose { "updateProfile()" }
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.updateProfile(params, adaptyCallback)
    }

    @JvmStatic
    fun getPurchaserInfo(
        forceUpdate: Boolean = false,
        adaptyCallback: (purchaserInfo: PurchaserInfoModel?, error: AdaptyError?) -> Unit
    ) {
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(null, notInitializedError)
            return
        }
        adaptyInternal.getPurchaserInfo(forceUpdate, adaptyCallback)
    }

    @JvmStatic
    @JvmOverloads
    fun getPaywalls(
        forceUpdate: Boolean = false,
        adaptyCallback: (paywalls: List<PaywallModel>?, products: List<ProductModel>?, error: AdaptyError?) -> Unit
    ) {
        Logger.logVerbose { "getPaywalls(forceUpdate = $forceUpdate)" }
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(null, null, notInitializedError)
            return
        }
        adaptyInternal.getPaywalls(forceUpdate, adaptyCallback)
    }

    @JvmStatic
    fun getPromo(
        adaptyCallback: (promo: PromoModel?, error: AdaptyError?) -> Unit
    ) {
        Logger.logVerbose { "getPromo()" }
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(null, notInitializedError)
            return
        }
        adaptyInternal.getPromo(adaptyCallback)
    }

    @JvmStatic
    fun makePurchase(
        activity: Activity,
        product: ProductModel,
        adaptyCallback: (purchaserInfo: PurchaserInfoModel?, purchaseToken: String?, googleValidationResult: GoogleValidationResult?, product: ProductModel, error: AdaptyError?) -> Unit
    ) {
        Logger.logVerbose { "makePurchase()" }
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(null, null, null, product, notInitializedError)
            return
        }
        adaptyInternal.makePurchase(activity, product, adaptyCallback)
    }

    @JvmStatic
    fun restorePurchases(
        adaptyCallback: (purchaserInfo: PurchaserInfoModel?, googleValidationResultList: List<GoogleValidationResult>?, error: AdaptyError?) -> Unit
    ) {
        Logger.logVerbose { "restorePurchases()" }
        if (!isActivated) {
            logNotInitializedError()
            adaptyCallback.invoke(null, null, notInitializedError)
            return
        }
        adaptyInternal.restorePurchases(adaptyCallback)
    }

    @JvmStatic
    @JvmOverloads
    fun updateAttribution(
        attribution: Any,
        source: AttributionType,
        networkUserId: String? = null,
        adaptyCallback: (error: AdaptyError?) -> Unit
    ) {
        Logger.logVerbose { "updateAttribution(source = $source)" }
        if (BuildConfig.DEBUG && source == AttributionType.APPSFLYER) {
            require(networkUserId != null) { "networkUserId is required for AppsFlyer attribution, otherwise we won't be able to send specific events." }
        }
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.updateAttribution(attribution, source, networkUserId, adaptyCallback)
    }

    @JvmStatic
    fun setExternalAnalyticsEnabled(
        enabled: Boolean,
        adaptyCallback: (error: AdaptyError?) -> Unit
    ) {
        Logger.logVerbose { "setExternalAnalyticsEnabled($enabled)" }
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.setExternalAnalyticsEnabled(enabled, adaptyCallback)
    }

    @JvmStatic
    fun setTransactionVariationId(
        transactionId: String,
        variationId: String,
        adaptyCallback: (error: AdaptyError?) -> Unit
    ) {
        Logger.logVerbose { "setTransactionVariationId(transactionId = $transactionId, variationId = $variationId)" }
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.setTransactionVariationId(transactionId, variationId, adaptyCallback)
    }

    @JvmStatic
    fun logout(adaptyCallback: (error: AdaptyError?) -> Unit) {
        if (!checkActivated(adaptyCallback)) return
        adaptyInternal.logout(adaptyCallback)
    }

    @JvmStatic
    fun refreshPushToken(newToken: String) {
        if (!checkActivated()) return
        adaptyInternal.refreshPushToken(newToken)
    }

    @JvmStatic
    fun handlePromoIntent(
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
    fun setOnPurchaserInfoUpdatedListener(onPurchaserInfoUpdatedListener: OnPurchaserInfoUpdatedListener?) {
        if (!checkActivated()) return
        adaptyInternal.onPurchaserInfoUpdatedListener = onPurchaserInfoUpdatedListener
    }

    @JvmStatic
    fun setOnPromoReceivedListener(onPromoReceivedListener: OnPromoReceivedListener?) {
        if (!checkActivated()) return
        adaptyInternal.onPromoReceivedListener = onPromoReceivedListener
    }

    @JvmStatic
    fun setVisualPaywallListener(visualPaywallListener: VisualPaywallListener?) {
        if (!checkActivated()) return
        adaptyInternal.setVisualPaywallListener(visualPaywallListener)
    }

    @JvmStatic
    fun setLogLevel(logLevel: AdaptyLogLevel) {
        Logger.logLevel = logLevel
    }

    @JvmStatic
    fun showPaywall(
        activity: Activity,
        paywall: PaywallModel,
    ) {
        Logger.logVerbose { "showPaywall()" }
        if (!checkActivated()) return
        adaptyInternal.showPaywall(activity, paywall)
    }

    @JvmStatic
    fun closePaywall() {
        Logger.logVerbose { "closePaywall()" }
        if (!checkActivated()) return
        adaptyInternal.closePaywall()
    }

    @JvmStatic
    fun logShowPaywall(paywall: PaywallModel) {
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

    private fun init(context: Context, appKey: String) {
        try {
            lock.writeLock().lock()
            Dependencies.init(context.applicationContext)
            adaptyInternal.init(appKey)
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