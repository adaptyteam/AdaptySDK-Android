package com.adapty.internal

import android.app.Activity
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
import com.adapty.utils.ErrorCallback
import com.adapty.utils.ResultCallback
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AdaptyInternal(
    private val authInteractor: AuthInteractor,
    private val profileInteractor: ProfileInteractor,
    private val purchasesInteractor: PurchasesInteractor,
    private val productsInteractor: ProductsInteractor,
    private val analyticsTracker: AnalyticsTracker,
    private val lifecycleAwareRequestRunner: LifecycleAwareRequestRunner,
    private val lifecycleManager: LifecycleManager,
    private val isObserverMode: Boolean,
) {

    @get:JvmSynthetic
    @set:JvmSynthetic
    var onProfileUpdatedListener: OnProfileUpdatedListener? = null
        set(value) {
            execute {
                profileInteractor
                    .subscribeOnProfileChanges()
                    .catch { }
                    .onEach { value?.onProfileReceived(it) }
                    .flowOnMain()
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
                .flowOnMain()
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
                .flowOnMain()
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
                SDKMethodRequestData.Activate.create(isObserverMode, customerUserId != null)
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
                .flowOnMain()
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
                .flowOnMain()
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
        callback: ResultCallback<AdaptyPurchasedInfo?>
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
                .flowOnMain()
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
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun getPaywall(
        id: String,
        locale: String?,
        fetchPolicy: AdaptyPaywall.FetchPolicy,
        loadTimeout: Int,
        callback: ResultCallback<AdaptyPaywall>
    ) {
        val requestEvent = SDKMethodRequestData.GetPaywall.create(id, locale, fetchPolicy, loadTimeout)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            productsInteractor
                .getPaywall(id, locale, fetchPolicy, loadTimeout.coerceAtLeast(MIN_PAYWALL_TIMEOUT_MILLIS))
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result)
                }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun getViewConfiguration(
        paywall: AdaptyPaywall,
        locale: String,
        loadTimeout: Int,
        callback: ResultCallback<AdaptyViewConfiguration>
    ) {
        val requestEvent = SDKMethodRequestData.create("get_paywall_builder")
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            productsInteractor
                .getViewConfiguration(paywall, locale, loadTimeout.coerceAtLeast(MIN_PAYWALL_TIMEOUT_MILLIS))
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result)
                }
                .flowOnMain()
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
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun setFallbackPaywalls(paywalls: String, callback: ErrorCallback?) {
        val requestEvent = SDKMethodRequestData.create("set_fallback_paywalls")
        analyticsTracker.trackSystemEvent(requestEvent)
        productsInteractor.setFallbackPaywalls(paywalls).let { error ->
            analyticsTracker.trackSystemEvent(
                SDKMethodResponseData.create(requestEvent, error)
            )
            callback?.onResult(error)
        }
    }

    @JvmSynthetic
    fun logShowPaywall(paywall: AdaptyPaywall, viewConfiguration: AdaptyViewConfiguration?, callback: ErrorCallback?) {
        analyticsTracker.trackEvent(
            "paywall_showed",
            mutableMapOf(
                "variation_id" to paywall.variationId
            ).apply {
                viewConfiguration?.id?.let { id -> put("paywall_builder_id", id) }
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
        source: AdaptyAttributionSource,
        networkUserId: String?,
        callback: ErrorCallback
    ) {
        val requestEvent = SDKMethodRequestData.UpdateAttribution.create(source.toString(), networkUserId)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            profileInteractor
                .updateAttribution(attribution, source, networkUserId)
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result.errorOrNull())
                }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun setVariationId(
        transactionId: String,
        variationId: String,
        callback: ErrorCallback
    ) {
        val requestEvent = SDKMethodRequestData.SetVariationId.create(transactionId, variationId)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            purchasesInteractor
                .setVariationId(transactionId, variationId)
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback.onResult(result.errorOrNull())
                }
                .flowOnMain()
                .collect()
        }
    }

    private fun setupStartRequests() {
        execute {
            profileInteractor
                .subscribeOnEventsForStartRequests()
                .onEach { (newProfileIdDuringThisSession, _) ->
                    if (newProfileIdDuringThisSession)
                        lifecycleAwareRequestRunner.restart()
                }
                .flowOnMain()
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
                .flowOnIO()
                .catch { }
                .collect()
        }
    }
}