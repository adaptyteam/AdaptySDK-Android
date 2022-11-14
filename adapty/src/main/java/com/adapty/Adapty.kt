package com.adapty

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.IntRange
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.AdaptyInternal
import com.adapty.internal.di.Dependencies
import com.adapty.internal.di.Dependencies.inject
import com.adapty.internal.utils.Logger
import com.adapty.listeners.OnProfileUpdatedListener
import com.adapty.models.*
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.AdaptyResult
import com.adapty.utils.ErrorCallback
import com.adapty.utils.ResultCallback
import java.util.concurrent.locks.ReentrantReadWriteLock

public object Adapty {

    @JvmStatic
    @JvmOverloads
    public fun activate(
        context: Context,
        appKey: String,
        observerMode: Boolean = false,
        customerUserId: String? = null,
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
    public fun identify(customerUserId: String, callback: ErrorCallback) {
        Logger.logMethodCall { "identify($customerUserId)" }
        if (!checkActivated(callback)) return
        adaptyInternal.identify(customerUserId, callback)
    }

    @JvmStatic
    public fun updateProfile(params: AdaptyProfileParameters, callback: ErrorCallback) {
        Logger.logMethodCall { "updateProfile()" }
        if (!checkActivated(callback)) return
        adaptyInternal.updateProfile(params, callback)
    }

    @JvmStatic
    public fun getProfile(callback: ResultCallback<AdaptyProfile>) {
        Logger.logMethodCall { "getProfile()" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getProfile(callback)
    }

    @JvmStatic
    public fun getPaywall(id: String, callback: ResultCallback<AdaptyPaywall>) {
        Logger.logMethodCall { "getPaywall(id = $id)" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getPaywall(id, callback)
    }

    @JvmStatic
    public fun getPaywallProducts(
        paywall: AdaptyPaywall,
        callback: ResultCallback<List<AdaptyPaywallProduct>>,
    ) {
        Logger.logMethodCall { "getPaywallProducts(id = ${paywall.id})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getPaywallProducts(paywall, callback)
    }

    @JvmStatic
    @JvmOverloads
    public fun makePurchase(
        activity: Activity,
        product: AdaptyPaywallProduct,
        subscriptionUpdateParams: AdaptySubscriptionUpdateParameters? = null,
        callback: ResultCallback<AdaptyProfile?>,
    ) {
        Logger.logMethodCall { "makePurchase(vendorProductId = ${product.vendorProductId}${subscriptionUpdateParams?.let { "; oldVendorProductId = ${it.oldSubVendorProductId}; prorationMode = ${it.prorationMode}" }.orEmpty()})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.makePurchase(activity, product, subscriptionUpdateParams, callback)
    }

    @JvmStatic
    public fun restorePurchases(callback: ResultCallback<AdaptyProfile>) {
        Logger.logMethodCall { "restorePurchases()" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.restorePurchases(callback)
    }

    @JvmStatic
    @JvmOverloads
    public fun updateAttribution(
        attribution: Any,
        source: AdaptyAttributionSource,
        networkUserId: String? = null,
        callback: ErrorCallback,
    ) {
        Logger.logMethodCall { "updateAttribution(source = $source)" }
        if (BuildConfig.DEBUG && source == AdaptyAttributionSource.APPSFLYER) {
            require(networkUserId != null) { "networkUserId is required for AppsFlyer attribution, otherwise we won't be able to send specific events." }
        }
        if (!checkActivated(callback)) return
        adaptyInternal.updateAttribution(attribution, source, networkUserId, callback)
    }

    @JvmStatic
    public fun setVariationId(
        forTransactionId: String,
        variationId: String,
        callback: ErrorCallback,
    ) {
        Logger.logMethodCall { "setVariationId(variationId = $variationId for transactionId = $forTransactionId)" }
        if (!checkActivated(callback)) return
        adaptyInternal.setVariationId(forTransactionId, variationId, callback)
    }

    @JvmStatic
    public fun logout(callback: ErrorCallback) {
        Logger.logMethodCall { "logout()" }
        if (!checkActivated(callback)) return
        adaptyInternal.logout(callback)
    }

    @JvmStatic
    public fun setOnProfileUpdatedListener(onProfileUpdatedListener: OnProfileUpdatedListener?) {
        if (!checkActivated()) return
        adaptyInternal.onProfileUpdatedListener = onProfileUpdatedListener
    }

    @JvmStatic
    public fun setLogLevel(logLevel: AdaptyLogLevel) {
        Logger.logLevel = logLevel.value
    }

    @JvmStatic
    @JvmOverloads
    public fun setFallbackPaywalls(paywalls: String, callback: ErrorCallback? = null) {
        Logger.logMethodCall { "setFallbackPaywalls()" }
        if (!checkActivated(callback)) return
        adaptyInternal.setFallbackPaywalls(paywalls, callback)
    }

    @JvmStatic
    @JvmOverloads
    public fun logShowPaywall(paywall: AdaptyPaywall, callback: ErrorCallback? = null) {
        Logger.logMethodCall { "logShowPaywall()" }
        if (!checkActivated(callback)) return
        adaptyInternal.logShowPaywall(paywall, callback)
    }

    @JvmStatic
    @JvmOverloads
    public fun logShowOnboarding(
        name: String?,
        screenName: String?,
        @IntRange(from = 1) screenOrder: Int,
        callback: ErrorCallback? = null,
    ) {
        Logger.logMethodCall { "logShowOnboarding()" }
        if (!checkActivated(callback)) return
        adaptyInternal.logShowOnboarding(name, screenName, screenOrder, callback)
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

    private fun checkActivated(callback: ErrorCallback? = null): Boolean {
        if (!isActivated) {
            logNotInitializedError()
            callback?.onResult(notInitializedError)
            return false
        }
        return true
    }

    private fun logNotInitializedError() {
        Logger.logError { "${notInitializedError.message}" }
    }
}