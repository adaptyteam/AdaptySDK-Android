package com.adapty.internal

import android.app.Activity
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode.NO_PURCHASES_TO_RESTORE
import com.adapty.errors.AdaptyErrorCode.WRONG_PARAMETER
import com.adapty.internal.data.cloud.KinesisManager
import com.adapty.internal.domain.AuthInteractor
import com.adapty.internal.domain.ProductsInteractor
import com.adapty.internal.domain.ProfileInteractor
import com.adapty.internal.domain.PurchasesInteractor
import com.adapty.internal.utils.*
import com.adapty.listeners.OnProfileUpdatedListener
import com.adapty.models.*
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyResult
import com.adapty.utils.ErrorCallback
import com.adapty.utils.ResultCallback
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AdaptyInternal(
    private val authInteractor: AuthInteractor,
    private val profileInteractor: ProfileInteractor,
    private val purchasesInteractor: PurchasesInteractor,
    private val productsInteractor: ProductsInteractor,
    private val kinesisManager: KinesisManager,
    private val lifecycleAwareRequestRunner: LifecycleAwareRequestRunner,
    private val lifecycleManager: LifecycleManager,
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

    private var isObserverMode = false

    fun init(appKey: String, observerMode: Boolean) {
        isObserverMode = observerMode
        authInteractor.handleAppKey(appKey)
        lifecycleManager.init()
    }


    @JvmSynthetic
    fun getProfile(
        callback: ResultCallback<AdaptyProfile>
    ) {
        execute {
            profileInteractor
                .getProfile()
                .catch { error -> callback.onResult(AdaptyResult.Error(error.asAdaptyError())) }
                .onEach { profile -> callback.onResult(AdaptyResult.Success(profile)) }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun updateProfile(
        params: AdaptyProfileParameters,
        callback: ErrorCallback
    ) {
        execute {
            profileInteractor
                .updateProfile(params)
                .catch { error -> callback.onResult(error.asAdaptyError()) }
                .onEach { callback.onResult(null) }
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
        execute {
            authInteractor.prepareAuthDataToSync(customerUserId)

            authInteractor
                .activateOrIdentify()
                .catch { error ->
                    callback?.onResult(error.asAdaptyError())
                    if (isInitialActivation) setupStartRequests()
                }
                .onEach {
                    callback?.onResult(null)
                    if (isInitialActivation) setupStartRequests()
                }
                .flowOnMain()
                .collect()
        }
        execute {
            profileInteractor.getAnalyticsCredsOnStart()
                .catch { }
                .flowOnMain()
                .collect()
        }
        execute { productsInteractor.getProductsOnStart().catch { }.collect() }
        execute {
            purchasesInteractor.syncPurchasesOnStart()
                .catch { error ->
                    if ((error as? AdaptyError)?.adaptyErrorCode == NO_PURCHASES_TO_RESTORE) {
                        profileInteractor.getProfileOnStart().catch { }.collect()
                    }
                }
                .collect()
        }
    }

    @JvmSynthetic
    fun identify(customerUserId: String, callback: ErrorCallback) {
        if (customerUserId.isBlank()) {
            val errorMessage = "customerUserId should not be empty"
            Logger.log(ERROR) { errorMessage }
            callback.onResult(
                AdaptyError(
                    message = errorMessage,
                    adaptyErrorCode = WRONG_PARAMETER
                )
            )
            return
        } else if (customerUserId == authInteractor.getCustomerUserId()) {
            callback.onResult(null)
            return
        }

        execute {
            authInteractor.prepareAuthDataToSync(customerUserId)

            authInteractor
                .activateOrIdentify()
                .catch { error -> callback.onResult(error.asAdaptyError()) }
                .onEach { callback.onResult(null) }
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
        callback: ResultCallback<AdaptyProfile?>
    ) {

        execute {
            purchasesInteractor.makePurchase(activity, product, subscriptionUpdateParams)
                .catch { error -> callback.onResult(AdaptyResult.Error(error.asAdaptyError())) }
                .onEach { profile -> callback.onResult(AdaptyResult.Success(profile)) }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun restorePurchases(callback: ResultCallback<AdaptyProfile>) {
        execute {
            purchasesInteractor
                .restorePurchases()
                .catch { error -> callback.onResult(AdaptyResult.Error(error.asAdaptyError())) }
                .onEach { profile -> callback.onResult(AdaptyResult.Success(profile)) }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun getPaywall(
        id: String,
        locale: String?,
        callback: ResultCallback<AdaptyPaywall>
    ) {
        execute {
            productsInteractor
                .getPaywall(id, locale)
                .catch { error -> callback.onResult(AdaptyResult.Error(error.asAdaptyError())) }
                .onEach { paywall -> callback.onResult(AdaptyResult.Success(paywall)) }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun getPaywallProducts(
        paywall: AdaptyPaywall,
        callback: ResultCallback<List<AdaptyPaywallProduct>>
    ) {
        execute {
            productsInteractor
                .getPaywallProducts(paywall)
                .catch { error -> callback.onResult(AdaptyResult.Error(error.asAdaptyError())) }
                .onEach { products -> callback.onResult(AdaptyResult.Success(products)) }
                .flowOnMain()
                .collect()
        }
    }

    @JvmSynthetic
    fun setFallbackPaywalls(paywalls: String, callback: ErrorCallback?) {
        productsInteractor.setFallbackPaywalls(paywalls).let { error -> callback?.onResult(error) }
    }

    @JvmSynthetic
    fun logShowPaywall(paywall: AdaptyPaywall, callback: ErrorCallback?) {
        kinesisManager.trackEvent(
            "paywall_showed",
            mapOf(
                "variation_id" to paywall.variationId
            ),
            callback,
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

        kinesisManager.trackEvent(
            "onboarding_screen_showed",
            hashMapOf<String, Any>("onboarding_screen_order" to screenOrder)
                .apply {
                    name?.let { put("onboarding_name", name) }
                    screenName?.let { put("onboarding_screen_name", screenName) }
                },
            callback,
        )
    }

    @JvmSynthetic
    fun updateAttribution(
        attribution: Any,
        source: AdaptyAttributionSource,
        networkUserId: String?,
        callback: ErrorCallback
    ) {
        execute {
            profileInteractor
                .updateAttribution(attribution, source, networkUserId)
                .catch { error -> callback.onResult(error.asAdaptyError()) }
                .onEach { callback.onResult(null) }
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
        execute {
            purchasesInteractor
                .setVariationId(transactionId, variationId)
                .catch { error -> callback.onResult(error.asAdaptyError()) }
                .onEach { callback.onResult(null) }
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
                .flatMapConcat { (newProfileIdDuringThisSession, _) ->
                    mutableListOf<Flow<*>>(
                        profileInteractor.syncMetaOnStart().catch { }
                    ).apply {
                        if (newProfileIdDuringThisSession) {
                            add(
                                purchasesInteractor.syncPurchasesIfNeeded()
                                    .catch { error ->
                                        if ((error as? AdaptyError)?.adaptyErrorCode == NO_PURCHASES_TO_RESTORE) {
                                            emitAll(profileInteractor.getProfileOnStart().catch { })
                                        }
                                    }
                            )
                        }

                        if (!isObserverMode)
                            add(purchasesInteractor.consumeAndAcknowledgeTheUnprocessed())
                    }.merge()
                }
                .flowOnIO()
                .catch { }
                .collect()
        }
    }
}