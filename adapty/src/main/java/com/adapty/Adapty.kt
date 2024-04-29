@file:OptIn(InternalAdaptyApi::class)

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
import com.adapty.internal.utils.DEFAULT_PAYWALL_TIMEOUT_MILLIS
import com.adapty.internal.utils.INF_PAYWALL_TIMEOUT_MILLIS
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.Logger
import com.adapty.listeners.OnProfileUpdatedListener
import com.adapty.models.*
import com.adapty.utils.*
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import java.util.concurrent.locks.ReentrantReadWriteLock

public object Adapty {

    /**
     * Use this method to initialize the Adapty SDK.
     *
     * ### If your app starts multiple processes (not always by yourself) don't forget to check main process.
     *
     * @param[context] Application context.
     *
     * @param[config] An [AdaptyConfig] object.
     */
    @JvmStatic
    public fun activate(
        context: Context,
        config: AdaptyConfig,
    ) {
        Logger.log(VERBOSE) { "activate(customerUserId = ${config.customerUserId})" }

        require(config.apiKey.isNotBlank()) { "Public SDK key must not be empty." }
        require(context.applicationContext is Application) { "Application context must be provided." }
        require(context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) { "INTERNET permission must be granted." }

        if (isActivated) {
            Logger.log(ERROR) { "Adapty was already activated. If you want to provide new customerUserId, please call 'identify' function instead." }
            return
        }

        init(context, config)
        adaptyInternal.activate(config.customerUserId)
    }

    /**
     * Use this method to initialize the Adapty SDK.
     *
     * ### If your app starts multiple processes (not always by yourself) don't forget to check main process.
     *
     * @param[context] Application context.
     *
     * @param[appKey] You can find it in your app settings
     * in [Adapty Dashboard](https://app.adapty.io/) _App settings_ > _General_.
     *
     * @param[observerMode] A boolean value controlling [Observer mode](https://docs.adapty.io/v2.0.0/docs/observer-vs-full-mode).
     * Turn it on if you handle purchases and subscription status yourself and use Adapty for sending
     * subscription events and analytics.
     *
     * @param[customerUserId] User identifier in your system.
     */
    @Deprecated(
        message = "This method has been deprecated. Please use Adapty.activate(context: Context, config: AdaptyConfig) instead",
        replaceWith = ReplaceWith("Adapty.activate(context, AdaptyConfig.Builder(appKey).build())", "com.adapty.models.AdaptyConfig"),
    )
    @JvmStatic
    @JvmOverloads
    public fun activate(
        context: Context,
        appKey: String,
        observerMode: Boolean = false,
        customerUserId: String? = null,
    ) {
        activate(
            context,
            AdaptyConfig.Builder(appKey)
                .withObserverMode(observerMode)
                .withCustomerUserId(customerUserId)
                .build()
        )
    }

    /**
     * Use this method for identifying user with it’s user id in your system.
     *
     * If you don’t have a user id on SDK configuration, you can set it later at any time with
     * `.identify()` method. The most common cases are after registration/authorization when the user
     * switches from being an anonymous user to an authenticated user.
     *
     * Should not be called before [activate]
     *
     * @param[customerUserId] User identifier in your system.
     *
     * @param[callback] An result containing the optional [AdaptyError].
     */
    @JvmStatic
    public fun identify(customerUserId: String, callback: ErrorCallback) {
        Logger.log(VERBOSE) { "identify($customerUserId)" }
        if (!checkActivated(callback)) return
        adaptyInternal.identify(customerUserId, callback)
    }

    /**
     * You can set optional attributes such as email, phone number, etc, to the user of your app.
     * You can then use attributes to create user [segments](https://docs.adapty.io/docs/segments)
     * or just view them in CRM.
     *
     * Should not be called before [activate]
     *
     * @param[params] Use [AdaptyProfileParameters.Builder] class to build this object.
     *
     * @param[callback] A result containing the optional [AdaptyError].
     *
     * @see <a href="https://docs.adapty.io/docs/setting-user-attributes">Setting User Attributes</a>
     */
    @JvmStatic
    public fun updateProfile(params: AdaptyProfileParameters, callback: ErrorCallback) {
        Logger.log(VERBOSE) { "updateProfile()" }
        if (!checkActivated(callback)) return
        adaptyInternal.updateProfile(params, callback)
    }

    /**
     * The main function for getting a user profile. Allows you to define the level of access,
     * as well as other parameters.
     *
     * The `getProfile` method provides the most up-to-date result as it always tries to query the API.
     * If for some reason (e.g. no internet connection), the Adapty SDK fails to retrieve information
     * from the server, the data from cache will be returned. It is also important to note that the
     * Adapty SDK updates AdaptyProfile cache on a regular basis, in order to keep this information
     * as up-to-date as possible.
     *
     * Should not be called before [activate]
     *
     * @param[callback] A result containing the [AdaptyProfile] object. This model contains info
     * about access levels, subscriptions, and non-subscription purchases. Generally, you have to check
     * only access level status to determine whether the user has premium access to the app.
     */
    @JvmStatic
    public fun getProfile(callback: ResultCallback<AdaptyProfile>) {
        Logger.log(VERBOSE) { "getProfile()" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getProfile(callback)
    }

    /**
     * Adapty allows you remotely configure the products that will be displayed in your app.
     * This way you don’t have to hardcode the products and can dynamically change offers or run
     * A/B tests without app releases.
     *
     * Should not be called before [activate]
     *
     * @param[placementId] The identifier of the desired placement. This is the value you specified when you
     * created the placement in the Adapty Dashboard.
     *
     * @param[locale] This parameter is expected to be a language code composed of one or more subtags separated by the "-" character. The first subtag is for the language, the second one is for the region (The support for regions will be added later).
     * Example: `"en"` means English, `"en-US"` represents US English.
     * If the parameter is omitted, the paywall will be returned in the default locale.
     *
     * @param[fetchPolicy] By default SDK will try to load data from server and will return cached data in case of failure. Otherwise use [AdaptyPaywall.FetchPolicy.ReturnCacheDataElseLoad] to return cached data if it exists.
     *
     * @param[loadTimeoutMillis] This value limits the timeout for this method. If the timeout is reached,
     * cached data or local fallback will be returned. The minimum value is 1000 milliseconds.
     * If a timeout is not required, you can pass [Int.MAX_VALUE] (or use [Integer.MAX_VALUE] when calling from Java).
     *
     * @param[callback] A result containing the [AdaptyPaywall] object. This model contains the list
     * of the products ids, paywall’s identifier, custom payload, and several other properties.
     *
     * @see <a href="https://docs.adapty.io/v2.0.0/docs/displaying-products">Displaying Paywalls & Products</a>
     */
    @JvmStatic
    @JvmOverloads
    public fun getPaywall(
        placementId: String,
        locale: String? = null,
        fetchPolicy: AdaptyPaywall.FetchPolicy = AdaptyPaywall.FetchPolicy.Default,
        @IntRange(from = 1000L)
        loadTimeoutMillis: Int = DEFAULT_PAYWALL_TIMEOUT_MILLIS,
        callback: ResultCallback<AdaptyPaywall>,
    ) {
        Logger.log(VERBOSE) { "getPaywall(placementId = $placementId${locale?.let { ", locale = $locale" }.orEmpty()}, fetchPolicy = ${fetchPolicy}${loadTimeoutMillis.takeIf { it != INF_PAYWALL_TIMEOUT_MILLIS }?.let { ", timeout = $it" }.orEmpty()})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getPaywall(placementId, locale, fetchPolicy, loadTimeoutMillis, callback)
    }

    /**
     * Once you have an [AdaptyPaywall], fetch corresponding products list using this method.
     *
     * Should not be called before [activate]
     *
     * @param[paywall] The [AdaptyPaywall] for which you want to get products.
     *
     * @param[callback] A result containing the [AdaptyPaywallProduct] list. You can present them in your UI.
     *
     * @see <a href="https://docs.adapty.io/v2.0.0/docs/displaying-products">Displaying Paywalls & Products</a>
     */
    @JvmStatic
    public fun getPaywallProducts(
        paywall: AdaptyPaywall,
        callback: ResultCallback<List<AdaptyPaywallProduct>>,
    ) {
        Logger.log(VERBOSE) { "getPaywallProducts(placementId = ${paywall.placementId})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getPaywallProducts(paywall, callback)
    }

    /**
     * If you are using the [Paywall Builder](https://docs.adapty.io/docs/paywall-builder-getting-started),
     * you can use this method to get a configuration object for your paywall.
     *
     * Should not be called before [activate]
     *
     * @param[paywall] The [AdaptyPaywall] for which you want to get a configuration.
     *
     * @param[locale] This parameter is expected to be a language code composed of one or more subtags separated by the "-" character. The first subtag is for the language, the second one is for the region (The support for regions will be added later).
     * Example: `"en"` means English, `"en-US"` represents US English.
     *
     * @param[loadTimeoutMillis] This value limits the timeout for this method. The minimum value is 1000 milliseconds.
     * If a timeout is not required, you can pass [Int.MAX_VALUE] (or use [Integer.MAX_VALUE] when calling from Java).
     *
     * @param[callback] A result containing the [AdaptyViewConfiguration] object.
     * Use it with [AdaptyUI](https://search.maven.org/artifact/io.adapty/android-ui) library.
     */
    @JvmStatic
    @JvmOverloads
    public fun getViewConfiguration(
        paywall: AdaptyPaywall,
        locale: String,
        @IntRange(from = 1000L)
        loadTimeoutMillis: Int = DEFAULT_PAYWALL_TIMEOUT_MILLIS,
        callback: ResultCallback<AdaptyViewConfiguration>
    ) {
        Logger.log(VERBOSE) { "getViewConfiguration(placementId = ${paywall.placementId}, locale = $locale${loadTimeoutMillis.takeIf { it != INF_PAYWALL_TIMEOUT_MILLIS }?.let { ", timeout = $it" }.orEmpty()})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getViewConfiguration(paywall, locale, loadTimeoutMillis, callback)
    }

    /**
     * To make the purchase, you have to call this method.
     *
     * Should not be called before [activate]
     *
     * @param[activity] An [Activity] instance.
     *
     * @param[product] An [AdaptyPaywallProduct] object retrieved from the paywall.
     *
     * @param[subscriptionUpdateParams] An [AdaptySubscriptionUpdateParameters] object, used when
     * you need a subscription to be replaced with another one, [read more](https://docs.adapty.io/docs/android-making-purchases#change-subscription).
     *
     * @param[isOfferPersonalized] Indicates whether the price is personalized, [read more](https://developer.android.com/google/play/billing/integrate#personalized-price).
     *
     * @param[callback] A result containing the [AdaptyPurchasedInfo] object (is null if and only if it was
     * a subscription change with the [DEFERRED][AdaptySubscriptionUpdateParameters.ReplacementMode.DEFERRED]
     * replacement mode). This model contains information about the purchase and the user's profile.
     * The profile, in turn, includes details about access levels, subscriptions, and non-subscription
     * purchases. Generally, you have to check only access level status to determine whether the user
     * has premium access to the app.
     *
     * @see <a href="https://docs.adapty.io/docs/android-making-purchases">Android – Making Purchases</a>
     */
    @JvmStatic
    @JvmOverloads
    public fun makePurchase(
        activity: Activity,
        product: AdaptyPaywallProduct,
        subscriptionUpdateParams: AdaptySubscriptionUpdateParameters? = null,
        isOfferPersonalized: Boolean = false,
        callback: ResultCallback<AdaptyPurchasedInfo?>,
    ) {
        Logger.log(VERBOSE) { "makePurchase(vendorProductId = ${product.vendorProductId}${product.subscriptionDetails?.let { "; basePlanId = ${it.basePlanId}${it.offerId?.let { offerId -> "; offerId = $offerId" }.orEmpty()}" }.orEmpty()}${subscriptionUpdateParams?.let { "; oldVendorProductId = ${it.oldSubVendorProductId}; replacementMode = ${it.replacementMode}" }.orEmpty()})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.makePurchase(activity, product, subscriptionUpdateParams, isOfferPersonalized, callback)
    }

    /**
     * To restore purchases, you have to call this method.
     *
     * Should not be called before [activate]
     *
     * @param[callback] A result containing the [AdaptyProfile] object. This model contains info about
     * access levels, subscriptions, and non-subscription purchases. Generally, you have to check
     * only access level status to determine whether the user has premium access to the app.
     *
     * @see <a href="https://docs.adapty.io/docs/android-making-purchases#restoring-purchases">Android – Restoring purchases</a>
     */
    @JvmStatic
    public fun restorePurchases(callback: ResultCallback<AdaptyProfile>) {
        Logger.log(VERBOSE) { "restorePurchases()" }
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
        Logger.log(VERBOSE) { "updateAttribution(source = $source)" }
        if (BuildConfig.DEBUG && source == AdaptyAttributionSource.APPSFLYER) {
            require(networkUserId != null) { "networkUserId is required for AppsFlyer attribution, otherwise we won't be able to send specific events." }
        }
        if (!checkActivated(callback)) return
        adaptyInternal.updateAttribution(attribution, source, networkUserId, callback)
    }

    /**
     * In Observer mode, Adapty SDK doesn’t know, where the purchase was made from.
     * If you display products using our [Paywalls](https://docs.adapty.io/v2.0.0/docs/paywall) or
     * [A/B Tests](https://docs.adapty.io/v2.0.0/docs/ab-test), you can manually assign variation
     * to the purchase. After doing this, you’ll be able to see metrics in Adapty Dashboard.
     *
     * Should not be called before [activate]
     *
     * @param[variationId] A string identifier of variation. You can get it using
     * [variationId][AdaptyPaywall.variationId] property of [AdaptyPaywall].
     *
     * @param[forTransactionId] A string identifier (`purchase.getOrderId()`) of the purchase,
     * where the purchase is an instance of the billing library [Purchase](https://developer.android.com/reference/com/android/billingclient/api/Purchase) class.
     *
     * @param[callback] A result containing the optional [AdaptyError].
     *
     * @see <a href="https://docs.adapty.io/docs/android-observer-mode">Android - Observer Mode</a>
     */
    @JvmStatic
    public fun setVariationId(
        forTransactionId: String,
        variationId: String,
        callback: ErrorCallback,
    ) {
        Logger.log(VERBOSE) { "setVariationId(variationId = $variationId for transactionId = $forTransactionId)" }
        if (!checkActivated(callback)) return
        adaptyInternal.setVariationId(forTransactionId, variationId, callback)
    }

    /**
     * You can logout the user anytime by calling this method.
     *
     * Should not be called before [activate]
     *
     * @param[callback] A result containing the optional [AdaptyError].
     */
    @JvmStatic
    public fun logout(callback: ErrorCallback) {
        Logger.log(VERBOSE) { "logout()" }
        if (!checkActivated(callback)) return
        adaptyInternal.logout(callback)
    }

    /**
     * Implement this method to receive automatic profile updates.
     *
     * Should not be called before [activate]
     */
    @JvmStatic
    public fun setOnProfileUpdatedListener(onProfileUpdatedListener: OnProfileUpdatedListener?) {
        if (!checkActivated()) return
        adaptyInternal.onProfileUpdatedListener = onProfileUpdatedListener
    }

    /**
     * Set to the most appropriate level of logging.
     *
     * For production builds, if you don't want the console logs and don't remove them with your
     * obfuscation tool, use [NONE][AdaptyLogLevel.NONE].
     * Also you can override the default logger calling [setLogHandler] with your own logic.
     *
     * Can be called before [activate]
     *
     */
    @JvmStatic
    public var logLevel: AdaptyLogLevel
        get() = Logger.logLevel
        set(value) {
            Logger.logLevel = value
        }

    /**
     * Override the default logger behavior using this method.
     *
     * Can be called before [activate]
     *
     * @param[logHandler] The function will be called for each message with the appropriate [logLevel]
     * (e.g. when [logLevel] is set to [WARN][AdaptyLogLevel.WARN], you will receive only _WARN_
     * and _ERROR_ logs, but not _INFO_ or _VERBOSE_).
     */
    @JvmStatic
    public fun setLogHandler(logHandler: AdaptyLogHandler) {
        Logger.logHandler = logHandler
    }

    /**
     * To set fallback paywalls, use this method. You should pass exactly the same payload you’re
     * getting from Adapty backend. You can copy it from Adapty Dashboard.
     *
     * Adapty allows you to provide fallback paywalls that will be used when a user opens the app
     * and there's no connection with Adapty backend (e.g. no internet connection or in the rare case
     * when backend is down) and there's no cache on the device.
     *
     * Should not be called before [activate]
     *
     * @param[paywalls] A JSON representation of your paywalls/products list in the exact same format
     * as provided by Adapty backend.
     *
     * @param[callback] A result containing the optional [AdaptyError].
     *
     * @see <a href="https://docs.adapty.io/docs/android-displaying-products#fallback-paywalls">Android - Fallback paywalls</a>
     */
    @JvmStatic
    @JvmOverloads
    public fun setFallbackPaywalls(paywalls: String, callback: ErrorCallback? = null) {
        Logger.log(VERBOSE) { "setFallbackPaywalls()" }
        if (!checkActivated(callback)) return
        adaptyInternal.setFallbackPaywalls(paywalls, callback)
    }

    /**
     * Call this method to notify Adapty SDK, that particular paywall was shown to user.
     *
     * Adapty helps you to measure the performance of the paywalls. We automatically collect all the
     * metrics related to purchases except for paywall views. This is because only you know when the
     * paywall was shown to a customer.
     *
     * Whenever you show a paywall to your user, call `.logShowPaywall(paywall)` to log the event,
     * and it will be accumulated in the paywall metrics.
     *
     * Should not be called before [activate]
     *
     * @param[paywall] A [AdaptyPaywall] object.
     *
     * @param[callback] A result containing the optional [AdaptyError].
     *
     * @see <a href="https://docs.adapty.io/docs/android-displaying-products#paywall-analytics">Android - Paywall analytics</a>
     */
    @JvmStatic
    @JvmOverloads
    public fun logShowPaywall(paywall: AdaptyPaywall, callback: ErrorCallback? = null) {
        logShowPaywall(paywall, null, callback)
    }

    @InternalAdaptyApi
    public fun logShowPaywall(
        paywall: AdaptyPaywall,
        viewConfiguration: AdaptyViewConfiguration?,
        callback: ErrorCallback? = null,
    ) {
        Logger.log(VERBOSE) { "logShowPaywall()" }
        if (!checkActivated(callback)) return
        adaptyInternal.logShowPaywall(paywall, viewConfiguration, callback)
    }

    /**
     * Call this method to keep track of the user’s steps while onboarding.
     *
     * The onboarding stage is a very common situation in modern mobile apps. The quality of its
     * implementation, content, and number of steps can have a rather significant influence on further
     * user behavior, especially on his desire to become a subscriber or simply make some purchases.
     *
     * In order for you to be able to analyze user behavior at this critical stage without leaving
     * Adapty, we have implemented the ability to send dedicated events every time a user visits yet
     * another onboarding screen.
     *
     * Should not be called before [activate]
     *
     * @param[name] Name of your onboarding.
     *
     * @param[screenName] Readable name of a particular screen as part of onboarding.
     *
     * @param[screenOrder] An unsigned integer value representing the order of this screen in your
     * onboarding sequence (it must me greater than 0).
     *
     * @param[callback] A result containing the optional [AdaptyError].
     *
     * @see <a href="https://docs.adapty.io/docs/android-making-purchases">Android – Making Purchases</a>
     */
    @JvmStatic
    @JvmOverloads
    public fun logShowOnboarding(
        name: String?,
        screenName: String?,
        @IntRange(from = 1) screenOrder: Int,
        callback: ErrorCallback? = null,
    ) {
        Logger.log(VERBOSE) { "logShowOnboarding()" }
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

    private fun init(context: Context, config: AdaptyConfig) {
        try {
            lock.writeLock().lock()
            Dependencies.init(context.applicationContext, config)
            adaptyInternal.init(config.apiKey)
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
        Logger.log(ERROR) { "${notInitializedError.message}" }
    }
}