@file:OptIn(InternalAdaptyApi::class)

package com.adapty

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.AdaptyInternal
import com.adapty.internal.di.Dependencies
import com.adapty.internal.di.Dependencies.inject
import com.adapty.internal.utils.DEFAULT_PLACEMENT_LOCALE
import com.adapty.internal.utils.DEFAULT_PLACEMENT_TIMEOUT
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.getCurrentProcessName
import com.adapty.internal.utils.getMainProcessName
import com.adapty.listeners.OnInstallationDetailsListener
import com.adapty.listeners.OnProfileUpdatedListener
import com.adapty.models.*
import com.adapty.utils.*
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
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
        Logger.log(VERBOSE) { "activate(customerUserId = ${config.customerUserId}, gpObfuscatedAccountId = ${config.gpObfuscatedAccountId})" }

        require(config.apiKey.isNotBlank()) { "Public SDK key must not be empty." }
        require(context.applicationContext is Application) { "Application context must be provided." }
        require(context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) { "INTERNET permission must be granted." }

        if (isActivated) {
            Logger.log(ERROR) { "Adapty was already activated. If you want to provide new customerUserId, please call 'identify' function instead." }
            return
        }

        if (!canBeActivatedInCurrentProcess(context, config))
            return

        init(context, config)
        adaptyInternal.activate(config.customerUserId, config.gpObfuscatedAccountId)
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
     * @param[gpObfuscatedAccountId] The obfuscated account identifier, [read more](https://developer.android.com/google/play/billing/developer-payload#attribute).
     *
     * @param[callback] An result containing the optional [AdaptyError].
     */
    @JvmStatic
    @JvmOverloads
    public fun identify(customerUserId: String, gpObfuscatedAccountId: String? = null, callback: ErrorCallback) {
        Logger.log(VERBOSE) { "identify($customerUserId, $gpObfuscatedAccountId)" }
        if (!checkActivated(callback)) return
        adaptyInternal.identify(customerUserId, gpObfuscatedAccountId, callback)
    }

    /**
     * You can set optional attributes such as email, phone number, etc, to the user of your app.
     * You can then use attributes to create user [segments](https://adapty.io/docs/segments)
     * or just view them in CRM.
     *
     * Should not be called before [activate]
     *
     * @param[params] Use [AdaptyProfileParameters.Builder] class to build this object.
     *
     * @param[callback] A result containing the optional [AdaptyError].
     *
     * @see <a href="https://adapty.io/docs/android-setting-user-attributes">Set user attributes</a>
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
     * Adapty allows you remotely configure a [Flow](https://adapty.io/docs/adapty-flow-builder)
     * that will be displayed in your app.
     *
     * Unlike onboardings, a flow is fetched without a locale: the locale is applied
     * later, when the view configuration is requested via `AdaptyUI.getFlowConfiguration(flow, callback)`.
     *
     * Should not be called before [activate]
     *
     * @param[placementId] The identifier of the desired placement. This is the value you specified
     * when you created the placement in the Adapty Dashboard.
     *
     * @param[fetchPolicy] By default SDK will try to load data from the server and will return
     * cached data in case of failure. Otherwise use [AdaptyPlacementFetchPolicy.ReturnCacheDataElseLoad]
     * to return cached data if it exists.
     *
     * @param[loadTimeout] This value limits the timeout for this method. If the timeout is reached,
     * cached data or local fallback will be returned. The minimum value is 1 second.
     * If a timeout is not required, you can pass [TimeInterval.INFINITE].
     *
     * @param[callback] A result containing the [AdaptyFlow] object.
     */
    @JvmStatic
    @JvmOverloads
    public fun getFlow(
        placementId: String,
        fetchPolicy: AdaptyPlacementFetchPolicy = AdaptyPlacementFetchPolicy.Default,
        loadTimeout: TimeInterval = DEFAULT_PLACEMENT_TIMEOUT,
        callback: ResultCallback<AdaptyFlow>,
    ) {
        Logger.log(VERBOSE) { "getFlow(placementId = $placementId, fetchPolicy = ${fetchPolicy}${loadTimeout.takeIf { it != TimeInterval.INFINITE }?.let { ", timeout = $it" }.orEmpty()})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getFlow(placementId, fetchPolicy, loadTimeout, callback)
    }

    /**
     * This method enables you to retrieve the flow from the Default Audience without having to
     * wait for the Adapty SDK to send all the user information required for segmentation to the server.
     *
     * Should not be called before [activate]
     *
     * @param[placementId] The identifier of the desired placement. This is the value you specified
     * when you created the placement in the Adapty Dashboard.
     *
     * @param[fetchPolicy] By default SDK will try to load data from the server and will return
     * cached data in case of failure. Otherwise use [AdaptyPlacementFetchPolicy.ReturnCacheDataElseLoad]
     * to return cached data if it exists.
     *
     * @param[callback] A result containing the [AdaptyFlow] object.
     */
    @JvmStatic
    @JvmOverloads
    public fun getFlowForDefaultAudience(
        placementId: String,
        fetchPolicy: AdaptyPlacementFetchPolicy = AdaptyPlacementFetchPolicy.Default,
        callback: ResultCallback<AdaptyFlow>,
    ) {
        Logger.log(VERBOSE) { "getFlowForDefaultAudience(placementId = $placementId, fetchPolicy = $fetchPolicy)" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getFlowForDefaultAudience(placementId, fetchPolicy, callback)
    }

    @JvmSynthetic
    internal fun <T> getFlowViewConfiguration(
        flow: AdaptyFlow,
        locale: String?,
        loadTimeout: TimeInterval,
        transform: (Map<String, Any>) -> T,
        callback: ResultCallback<T>,
    ) {
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getFlowViewConfiguration(flow, locale, loadTimeout, transform, callback)
    }

    /**
     * Whenever you show a flow to your user, call `.logShowFlow(flow)` to log the event,
     * and it will be accumulated in the flow metrics.
     *
     * Should not be called before [activate]
     *
     * @param[flow] An [AdaptyFlow] object.
     *
     * @param[callback] A result containing the optional [AdaptyError].
     */
    @JvmStatic
    @JvmOverloads
    public fun logShowFlow(flow: AdaptyFlow, callback: ErrorCallback? = null) {
        Logger.log(VERBOSE) { "logShowFlow()" }
        if (!checkActivated(callback)) return
        adaptyInternal.logShowFlow(flow, callback)
    }

    @InternalAdaptyApi
    @JvmStatic
    @JvmOverloads
    public fun logFlowEvent(
        flow: AdaptyFlow,
        viewConfigurationId: String,
        eventProperties: Map<String, Any>,
        callback: ErrorCallback? = null,
    ) {
        Logger.log(VERBOSE) { "logFlowEvent()" }
        if (!checkActivated(callback)) return
        adaptyInternal.logFlowEvent(flow, viewConfigurationId, eventProperties, callback)
    }

    /**
     * Once you have an [AdaptyFlow], fetch corresponding products list using this method.
     *
     * The order will be the same as in the flow's paywalls. Each product carries the
     * attribution (variation id, name) of its own paywall inside the flow.
     *
     * Should not be called before [activate]
     *
     * @param[flow] The [AdaptyFlow] for which you want to get products.
     *
     * @param[callback] A result containing the [AdaptyPaywallProduct] list. You can present them in your UI.
     */
    @JvmStatic
    public fun getPaywallProducts(
        flow: AdaptyFlow,
        callback: ResultCallback<List<AdaptyPaywallProduct>>,
    ) {
        Logger.log(VERBOSE) { "getPaywallProducts(placementId = ${flow.placement.id})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getPaywallProducts(flow, callback)
    }

    /**
     * Once you have an [AdaptyFlowPaywall], fetch corresponding products list using this method.
     *
     * The order will be the same as in the paywall. Each product carries the
     * attribution (variation id, name) of that paywall.
     *
     * Should not be called before [activate]
     *
     * @param[paywall] The [AdaptyFlowPaywall] for which you want to get products.
     *
     * @param[callback] A result containing the [AdaptyPaywallProduct] list. You can present them in your UI.
     */
    @JvmStatic
    public fun getPaywallProducts(
        paywall: AdaptyFlowPaywall,
        callback: ResultCallback<List<AdaptyPaywallProduct>>,
    ) {
        Logger.log(VERBOSE) { "getPaywallProducts(placementId = ${paywall.placement.id})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getPaywallProducts(paywall, callback)
    }

    @JvmStatic
    @JvmOverloads
    public fun getOnboarding(
        placementId: String,
        locale: String? = null,
        fetchPolicy: AdaptyPlacementFetchPolicy = AdaptyPlacementFetchPolicy.Default,
        loadTimeout: TimeInterval = DEFAULT_PLACEMENT_TIMEOUT,
        callback: ResultCallback<AdaptyOnboarding>,
    ) {
        Logger.log(VERBOSE) { "getOnboarding(placementId = $placementId${locale?.let { ", locale = $locale" }.orEmpty()}, fetchPolicy = ${fetchPolicy}${loadTimeout.takeIf { it != TimeInterval.INFINITE }?.let { ", timeout = $it" }.orEmpty()})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getOnboarding(placementId, locale ?: DEFAULT_PLACEMENT_LOCALE, fetchPolicy, loadTimeout, callback)
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
     * @param[params] Optional [AdaptyPurchaseParameters] used to provide additional purchase options.
     *
     * @param[callback] The result includes an [AdaptyPurchaseResult] object, which provides details about the purchase.
     * If the result is [AdaptyPurchaseResult.Success], it also includes the user's profile.
     * The profile, in turn, includes details about access levels, subscriptions, and non-subscription
     * purchases. Generally, you have to check only access level status to determine whether the user
     * has premium access to the app.
     *
     * @see <a href="https://adapty.io/docs/android-making-purchases">Make purchases in mobile app</a>
     */
    @JvmStatic
    @JvmOverloads
    public fun makePurchase(
        activity: Activity,
        product: AdaptyPaywallProduct,
        params: AdaptyPurchaseParameters = AdaptyPurchaseParameters.Empty,
        callback: ResultCallback<AdaptyPurchaseResult>,
    ) {
        Logger.log(VERBOSE) { "makePurchase(vendorProductId = ${product.vendorProductId}${product.subscriptionDetails?.let { "; basePlanId = ${it.basePlanId}${it.offerId?.let { offerId -> "; offerId = $offerId" }.orEmpty()}" }.orEmpty()}${params.subscriptionUpdateParams?.let { "; oldVendorProductId = ${it.oldSubVendorProductId}; replacementMode = ${it.replacementMode}" }.orEmpty()}${params.isOfferPersonalized.takeIf { it }?.let { "; isOfferPersonalized = $it" }.orEmpty()})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.makePurchase(activity, product, params, callback)
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
     * @see <a href="https://adapty.io/docs/android-restore-purchase">Restore purchases in mobile app</a>
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

    /**
     * To set attribution data for the profile, use this method.
     *
     * Should not be called before [activate]
     *
     * @param[attribution] A map containing attribution (conversion) data.
     *
     * @param[source] An [AdaptyAttributionSource] of the attribution.
     *
     * @param[callback] A result containing the optional [AdaptyError].
     */
    @JvmStatic
    public fun updateAttribution(
        attribution: Map<String, Any>,
        source: AdaptyAttributionSource,
        callback: ErrorCallback,
    ) {
        Logger.log(VERBOSE) { "updateAttribution(source = $source)" }
        if (!checkActivated(callback)) return
        adaptyInternal.updateAttribution(attribution, source.value, callback)
    }

    /**
     * To set attribution data for the profile, use this method.
     *
     * Should not be called before [activate]
     *
     * @param[attributionJson] A JSON string containing attribution (conversion) data.
     *
     * @param[source] An [AdaptyAttributionSource] of the attribution.
     *
     * @param[callback] A result containing the optional [AdaptyError].
     */
    @JvmStatic
    public fun updateAttribution(
        attributionJson: String,
        source: AdaptyAttributionSource,
        callback: ErrorCallback,
    ) {
        Logger.log(VERBOSE) { "updateAttribution(source = $source)" }
        if (!checkActivated(callback)) return
        adaptyInternal.updateAttribution(attributionJson, source.value, callback)
    }

    /**
     * Sets the integration identifiers associated with the current profile.
     *
     * Should not be called before [activate]
     *
     * @param[identifiers] A list of [AdaptyIntegrationIdentifier] to associate with the profile.
     *
     * @param[callback] A result containing the optional [AdaptyError].
     */
    @JvmStatic
    public fun setIntegrationIdentifier(
        identifiers: List<AdaptyIntegrationIdentifier>,
        callback: ErrorCallback,
    ) {
        Logger.log(VERBOSE) { "setIntegrationIdentifier(keys = ${identifiers.map { it.key }})" }
        if (!checkActivated(callback)) return
        adaptyInternal.setIntegrationIdentifiers(identifiers, callback)
    }

    /**
     * Sets a single integration identifier associated with the current profile.
     *
     * Should not be called before [activate]
     *
     * @param[identifier] An [AdaptyIntegrationIdentifier] to associate with the profile.
     *
     * @param[callback] A result containing the optional [AdaptyError].
     */
    @JvmStatic
    public fun setIntegrationIdentifier(
        identifier: AdaptyIntegrationIdentifier,
        callback: ErrorCallback,
    ) {
        setIntegrationIdentifier(listOf(identifier), callback)
    }

    @JvmStatic
    public fun getCurrentInstallationStatus(callback: ResultCallback<AdaptyInstallationStatus>) {
        Logger.log(VERBOSE) { "getCurrentInstallationStatus()" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getCurrentInstallationStatus(callback)
    }

    /**
     * In Observer mode, Adapty SDK doesn’t know, where the purchase was made from.
     * If you display products using our [Paywalls](https://adapty.io/docs/paywalls) or
     * [A/B Tests](https://adapty.io/docs/ab-tests), you can manually assign variation
     * to the purchase. After doing this, you’ll be able to see metrics in Adapty Dashboard.
     *
     * Should not be called before [activate]
     *
     * @param[transactionInfo] A [TransactionInfo] instance, containing either a string identifier (`purchase.getOrderId()`) of the purchase,
     * or an instance of the billing library [Purchase](https://developer.android.com/reference/com/android/billingclient/api/Purchase) class.
     *
     * @param[variationId] A string identifier of variation. You can get it using
     * [variationId][AdaptyFlow.variationId] property of [AdaptyFlow].
     *
     * @param[callback] A result containing the optional [AdaptyError].
     *
     * @see <a href="https://adapty.io/docs/report-transactions-observer-mode-android">Report transactions in Observer mode</a>
     */
    @JvmOverloads
    @JvmStatic
    public fun reportTransaction(
        transactionInfo: TransactionInfo,
        variationId: String? = null,
        callback: ResultCallback<AdaptyProfile>,
    ) {
        Logger.log(VERBOSE) { "reportTransaction(transactionInfo = $transactionInfo${variationId?.let { ", variationId = $variationId" }.orEmpty()})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.reportTransaction(transactionInfo, variationId, callback)
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


    @JvmStatic
    public fun createWebPaywallUrl(
        product: AdaptyPaywallProduct,
        callback: ResultCallback<Uri>,
    ) {
        Logger.log(VERBOSE) { "createWebPaywallUrl(variationId = ${product.variationId})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.createWebPaywallUrl(product, callback)
    }

    @JvmStatic
    @JvmOverloads
    public fun openWebPaywall(
        activity: Activity,
        product: AdaptyPaywallProduct,
        presentation: AdaptyWebPresentation = AdaptyWebPresentation.ExternalBrowser,
        callback: ErrorCallback,
    ) {
        Logger.log(VERBOSE) { "openWebPaywall(variationId = ${product.variationId}, presentation = $presentation)" }
        if (!checkActivated(callback)) return
        adaptyInternal.openWebPaywall(activity, product, presentation, callback)
    }

    @JvmStatic
    public fun createWebPaywallUrl(
        paywall: AdaptyFlowPaywall,
        callback: ResultCallback<Uri>,
    ) {
        Logger.log(VERBOSE) { "createWebPaywallUrl(variationId = ${paywall.variationId})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.createWebPaywallUrl(paywall, callback)
    }

    @JvmStatic
    @JvmOverloads
    public fun openWebPaywall(
        activity: Activity,
        paywall: AdaptyFlowPaywall,
        presentation: AdaptyWebPresentation = AdaptyWebPresentation.ExternalBrowser,
        callback: ErrorCallback,
    ) {
        Logger.log(VERBOSE) { "openWebPaywall(variationId = ${paywall.variationId}, presentation = $presentation)" }
        if (!checkActivated(callback)) return
        adaptyInternal.openWebPaywall(activity, paywall, presentation, callback)
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

    @JvmStatic
    public fun setOnInstallationDetailsListener(onInstallationDetailsListener: OnInstallationDetailsListener?) {
        if (!checkActivated()) return
        adaptyInternal.onInstallationDetailsListener = onInstallationDetailsListener
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
     * @param[location] A [location][FileLocation] of a file with JSON representation of your paywalls/products list
     * in the exact same format as provided by Adapty backend.
     *
     * @param[callback] A result containing the optional [AdaptyError].
     *
     * @see <a href="https://adapty.io/docs/android-use-fallback-paywalls">Android - Use fallback paywalls</a>
     */
    @JvmStatic
    @JvmOverloads
    public fun setFallback(location: FileLocation, callback: ErrorCallback? = null) {
        Logger.log(VERBOSE) { "setFallback()" }
        if (!checkActivated(callback)) return
        adaptyInternal.setFallback(location, callback)
    }

    internal fun logShowOnboardingInternal(
        onboarding: AdaptyOnboarding,
        screenName: String?,
        screenOrder: Int,
        isLastScreen: Boolean,
    ) {
        Logger.log(VERBOSE) { "logShowOnboardingInternal()" }
        if (!isActivated) {
            logNotInitializedError()
            return
        }
        adaptyInternal.logShowOnboardingInternal(
            onboarding,
            screenName,
            screenOrder,
            isLastScreen,
        )
    }

    @JvmStatic
    @JvmOverloads
    public fun getOnboardingForDefaultAudience(
        placementId: String,
        locale: String? = null,
        fetchPolicy: AdaptyPlacementFetchPolicy = AdaptyPlacementFetchPolicy.Default,
        callback: ResultCallback<AdaptyOnboarding>,
    ) {
        Logger.log(VERBOSE) { "getOnboardingForDefaultAudience(placementId = $placementId${locale?.let { ", locale = $locale" }.orEmpty()}, fetchPolicy = ${fetchPolicy})" }
        if (!isActivated) {
            logNotInitializedError()
            callback.onResult(AdaptyResult.Error(notInitializedError))
            return
        }
        adaptyInternal.getOnboardingForDefaultAudience(placementId, locale ?: DEFAULT_PLACEMENT_LOCALE, fetchPolicy, callback)
    }

    private val adaptyInternal: AdaptyInternal by inject()

    private val lock = ReentrantReadWriteLock()

    /**
     * Returns whether [activate] has been called.
     *
     * Can be called before [activate]
     *
     */
    @JvmStatic
    public var isActivated: Boolean = false
        get() = try {
            lock.readLock().lock()
            field
        } finally {
            lock.readLock().unlock()
        }
        private set

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

    private fun canBeActivatedInCurrentProcess(context: Context, config: AdaptyConfig): Boolean {
        val processName = getCurrentProcessName() ?: return true
        val customProcessName = config.customProcessName?.let { customProcessName ->
            if (customProcessName.startsWith(":"))
                "${context.packageName}${customProcessName}"
            else
                customProcessName
        }
        val desiredProcessName = customProcessName ?: context.getMainProcessName() ?: return true
        return (processName == desiredProcessName)
            .also {
                if (!it)
                    Logger.log(WARN) { "Adapty can only run in a single process (the main process by default). To use a different process, set it in `AdaptyConfig` via `.withProcess($processName)`." }
            }
    }
}