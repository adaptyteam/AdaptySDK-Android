@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal

import android.app.Activity
import android.content.ContentResolver
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode.NO_PURCHASES_TO_RESTORE
import com.adapty.errors.AdaptyErrorCode.WRONG_PARAMETER
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.data.models.AnalyticsEvent.SDKMethodRequestData
import com.adapty.internal.data.models.AnalyticsEvent.SDKMethodResponseData
import com.adapty.internal.domain.AuthInteractor
import com.adapty.internal.domain.ProductsInteractor
import com.adapty.internal.domain.ProfileInteractor
import com.adapty.internal.domain.PurchasesInteractor
import com.adapty.internal.utils.*
import com.adapty.listeners.OnProfileUpdatedListener
import com.adapty.models.*
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyResult
import com.adapty.utils.TimeInterval
import com.adapty.utils.ErrorCallback
import com.adapty.utils.FileLocation
import com.adapty.utils.ResultCallback
import com.adapty.utils.TransactionInfo
import kotlinx.coroutines.flow.*
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AdaptyInternal(
    private val authInteractor: AuthInteractor,
    private val profileInteractor: ProfileInteractor,
    private val purchasesInteractor: PurchasesInteractor,
    private val productsInteractor: ProductsInteractor,
    private val analyticsTracker: AnalyticsTracker,
    private val lifecycleAwareRequestRunner: LifecycleAwareRequestRunner,
    private val lifecycleManager: LifecycleManager,
    private val adaptyUiAccessor: AdaptyUiAccessor,
    private val isObserverMode: Boolean,
    private val ipAddressCollectionDisabled: Boolean,
) {

    @get:JvmSynthetic
    @set:JvmSynthetic
    var onProfileUpdatedListener: OnProfileUpdatedListener? = null
        set(value) {
            execute {
                profileInteractor
                    .subscribeOnProfileChanges()
                    .catch { }
                    .onEach { runOnMain { value?.onProfileReceived(it) } }
                    .collect()
            }
            field = value
        }

    fun init(appKey: String) {
        authInteractor.handleAppKey(appKey)
        lifecycleManager.init()
    }


    @JvmSynthetic
    fun getProfile(
        callback: ResultCallback<AdaptyProfile>
    ) {
        val requestEvent = SDKMethodRequestData.create("get_profile")
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            profileInteractor
                .getProfile()
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result)
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun updateProfile(
        params: AdaptyProfileParameters,
        callback: ErrorCallback
    ) {
        val requestEvent = SDKMethodRequestData.create("update_profile")
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            profileInteractor
                .updateProfile(params)
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result.errorOrNull())
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun activate(
        customerUserId: String?,
        callback: ErrorCallback? = null,
        isInitialActivation: Boolean = true,
    ) {
        val requestEvent =
            if (isInitialActivation)
                SDKMethodRequestData.Activate.create(isObserverMode, ipAddressCollectionDisabled, customerUserId != null)
            else
                SDKMethodRequestData.create("logout")
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            authInteractor.prepareAuthDataToSync(customerUserId)

            authInteractor
                .activateOrIdentify()
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback?.onResult(result.errorOrNull())
                    if (isInitialActivation) setupStartRequests()
                }
                .collect()
        }
        execute { productsInteractor.getProductsOnStart().catch { }.collect() }
    }

    @JvmSynthetic
    fun identify(customerUserId: String, callback: ErrorCallback) {
        val requestEvent = SDKMethodRequestData.create("identify")
        analyticsTracker.trackSystemEvent(requestEvent)
        if (customerUserId.isBlank()) {
            val errorMessage = "customerUserId should not be empty"
            Logger.log(ERROR) { errorMessage }
            val e = AdaptyError(
                message = errorMessage,
                adaptyErrorCode = WRONG_PARAMETER
            )
            analyticsTracker.trackSystemEvent(
                SDKMethodResponseData.create(requestEvent, e)
            )
            callback.onResult(e)
            return
        } else if (customerUserId == authInteractor.getCustomerUserId()) {
            analyticsTracker.trackSystemEvent(
                SDKMethodResponseData.create(requestEvent, null)
            )
            callback.onResult(null)
            return
        }

        execute {
            authInteractor.prepareAuthDataToSync(customerUserId)

            authInteractor
                .activateOrIdentify()
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result.errorOrNull())
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun logout(callback: ErrorCallback) {
        authInteractor.clearDataOnLogout()
        activate(null, callback, false)
    }

    @JvmSynthetic
    fun makePurchase(
        activity: Activity,
        product: AdaptyPaywallProduct,
        subscriptionUpdateParams: AdaptySubscriptionUpdateParameters?,
        isOfferPersonalized: Boolean,
        callback: ResultCallback<AdaptyPurchaseResult>
    ) {
        val requestEvent = SDKMethodRequestData.MakePurchase.create(product)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            purchasesInteractor.makePurchase(activity, product, subscriptionUpdateParams, isOfferPersonalized)
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result)
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun restorePurchases(callback: ResultCallback<AdaptyProfile>) {
        val requestEvent = SDKMethodRequestData.create("restore_purchases")
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            purchasesInteractor
                .restorePurchases()
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result)
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun getPaywall(
        id: String,
        locale: String,
        fetchPolicy: AdaptyPaywall.FetchPolicy,
        loadTimeout: TimeInterval,
        callback: ResultCallback<AdaptyPaywall>
    ) {
        val requestEvent = SDKMethodRequestData.GetPaywall.create(id, locale, fetchPolicy, loadTimeout.toMillis())
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            productsInteractor
                .getPaywall(id, locale, fetchPolicy, loadTimeout.coerceAtLeast(MIN_PAYWALL_TIMEOUT).toMillis())
                .onSingleResult { result ->
                    if (result is AdaptyResult.Success) {
                        result.value.viewConfig?.let { config -> adaptyUiAccessor.preloadMedia(config) }
                    }
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result)
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun getPaywallForDefaultAudience(
        id: String,
        locale: String,
        fetchPolicy: AdaptyPaywall.FetchPolicy,
        callback: ResultCallback<AdaptyPaywall>
    ) {
        val requestEvent = SDKMethodRequestData.GetUntargetedPaywall.create(id, locale, fetchPolicy)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            productsInteractor
                .getPaywallUntargeted(id, locale, fetchPolicy)
                .onSingleResult { result ->
                    if (result is AdaptyResult.Success) {
                        result.value.viewConfig?.let { config -> adaptyUiAccessor.preloadMedia(config) }
                    }
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result)
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun getViewConfiguration(
        paywall: AdaptyPaywall,
        loadTimeout: TimeInterval,
        callback: ResultCallback<Map<String, Any>>
    ) {
        val requestEvent = SDKMethodRequestData.create("get_paywall_builder")
        analyticsTracker.trackSystemEvent(requestEvent)
        if (!paywall.hasViewConfiguration) {
            val errorMessage = "View configuration has not been found for the requested paywall"
            Logger.log(ERROR) { errorMessage }
            val e = AdaptyError(
                message = errorMessage,
                adaptyErrorCode = WRONG_PARAMETER
            )
            analyticsTracker.trackSystemEvent(
                SDKMethodResponseData.create(requestEvent, e)
            )
            callback.onResult(AdaptyResult.Error(e))
            return
        }
        execute {
            productsInteractor
                .getViewConfiguration(paywall, loadTimeout.coerceAtLeast(MIN_PAYWALL_TIMEOUT).toMillis())
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result)
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun getPaywallProducts(
        paywall: AdaptyPaywall,
        callback: ResultCallback<List<AdaptyPaywallProduct>>
    ) {
        val requestEvent = SDKMethodRequestData.GetPaywallProducts.create(paywall.placementId)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            productsInteractor
                .getPaywallProducts(paywall)
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result)
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun setFallbackPaywalls(source: FileLocation, callback: ErrorCallback?) {
        val requestEvent = SDKMethodRequestData.create("set_fallback_paywalls")
        analyticsTracker.trackSystemEvent(requestEvent)
        if (source is FileLocation.Uri) {
            val scheme = source.uri.scheme?.lowercase(Locale.ENGLISH)
            if (scheme.orEmpty() !in setOf(ContentResolver.SCHEME_ANDROID_RESOURCE, ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE)) {
                val errorMessage = "The fallback paywalls' URI has an unsupported scheme: $scheme"
                Logger.log(ERROR) { errorMessage }
                val e = AdaptyError(
                    message = errorMessage,
                    adaptyErrorCode = WRONG_PARAMETER
                )
                analyticsTracker.trackSystemEvent(
                    SDKMethodResponseData.create(requestEvent, e)
                )
                callback?.onResult(e)
                return
            }
        }
        execute {
            productsInteractor
                .setFallbackPaywalls(source)
                .onSingleResult { result ->
                    val error = result.errorOrNull()
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, error)
                    )
                    callback?.onResult(error)
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun logShowPaywall(paywall: AdaptyPaywall, additionalFields: Map<String, Any>?, callback: ErrorCallback?) {
        analyticsTracker.trackEvent(
            "paywall_showed",
            mutableMapOf<String, Any>(
                "variation_id" to paywall.variationId
            ).apply {
                additionalFields?.let(::putAll)
            },
            completion = callback,
        )
    }

    @JvmSynthetic
    fun logShowOnboarding(
        name: String?,
        screenName: String?,
        screenOrder: Int,
        callback: ErrorCallback?,
    ) {
        if (screenOrder < 1) {
            val errorMessage = "screenOrder must be greater than or equal to 1"
            Logger.log(ERROR) { errorMessage }
            callback?.onResult(
                AdaptyError(
                    message = errorMessage,
                    adaptyErrorCode = WRONG_PARAMETER
                )
            )
            return
        }

        analyticsTracker.trackEvent(
            "onboarding_screen_showed",
            hashMapOf<String, Any>("onboarding_screen_order" to screenOrder)
                .apply {
                    name?.let { put("onboarding_name", name) }
                    screenName?.let { put("onboarding_screen_name", screenName) }
                },
            completion = callback,
        )
    }

    @JvmSynthetic
    fun updateAttribution(
        attribution: Any,
        source: String,
        callback: ErrorCallback
    ) {
        val requestEvent = SDKMethodRequestData.UpdateAttribution.create(source)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            profileInteractor
                .updateAttribution(attribution, source)
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result.errorOrNull())
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun setIntegrationId(key: String, value: String, callback: ErrorCallback) {
        val requestEvent = SDKMethodRequestData.SetIntegrationId.create(key, value)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            profileInteractor
                .setIntegrationId(key, value)
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result.errorOrNull())
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun reportTransaction(
        transactionInfo: TransactionInfo,
        variationId: String?,
        callback: ResultCallback<AdaptyProfile>,
    ) {
        val requestEvent = SDKMethodRequestData.ReportTransaction.create(transactionInfo, variationId)
        analyticsTracker.trackSystemEvent(requestEvent)
        if (transactionInfo is TransactionInfo.Purchase && transactionInfo.purchase.orderId == null) {
            val errorMessage = "orderId in Purchase should not be null"
            Logger.log(ERROR) { errorMessage }
            val e = AdaptyError(
                message = errorMessage,
                adaptyErrorCode = WRONG_PARAMETER
            )
            analyticsTracker.trackSystemEvent(
                SDKMethodResponseData.create(requestEvent, e)
            )
            callback.onResult(AdaptyResult.Error(e))
            return
        }
        execute {
            purchasesInteractor
                .reportTransaction(transactionInfo, variationId)
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result)
                }
                .collect()
        }
    }

    private fun setupStartRequests() {
        execute {
            profileInteractor
                .subscribeOnEventsForStartRequests()
                .onEach { (newProfileIdDuringThisSession, _) ->
                    if (newProfileIdDuringThisSession)
                        runOnMain { lifecycleAwareRequestRunner.restart() }
                }
                .flatMapMerge { (newProfileIdDuringThisSession, newCustomerUserIdDuringThisSession) ->
                    mutableListOf<Flow<*>>(
                        profileInteractor.syncMetaOnStart().catch { }
                    ).apply {
                        if ((newProfileIdDuringThisSession || newCustomerUserIdDuringThisSession)) {
                            add(
                                purchasesInteractor.syncPurchasesOnStart()
                                    .catch { error ->
                                        if ((error as? AdaptyError)?.adaptyErrorCode == NO_PURCHASES_TO_RESTORE) {
                                            emitAll(profileInteractor.getProfileOnStart().catch { })
                                        }
                                    }
                            )
                        }
                    }.merge()
                }
                .catch { }
                .collect()
        }
    }
}