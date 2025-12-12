@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal

import android.app.Activity
import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.errors.AdaptyErrorCode.LOGGING_OUT_UNIDENTIFIED_USER
import com.adapty.errors.AdaptyErrorCode.NO_PURCHASES_TO_RESTORE
import com.adapty.errors.AdaptyErrorCode.WRONG_PARAMETER
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.data.models.AnalyticsEvent.SDKMethodRequestData
import com.adapty.internal.data.models.AnalyticsEvent.SDKMethodResponseData
import com.adapty.internal.domain.AuthInteractor
import com.adapty.internal.domain.OnboardingInteractor
import com.adapty.internal.domain.PaywallInteractor
import com.adapty.internal.domain.ProfileInteractor
import com.adapty.internal.domain.PurchasesInteractor
import com.adapty.internal.domain.UserAcquisitionInteractor
import com.adapty.internal.utils.*
import com.adapty.listeners.OnInstallationDetailsListener
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
    private val paywallInteractor: PaywallInteractor,
    private val onboardingInteractor: OnboardingInteractor,
    private val userAcquisitionInteractor: UserAcquisitionInteractor,
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

    var onInstallationDetailsListener: OnInstallationDetailsListener? = null
        set(value) {
            execute {
                userAcquisitionInteractor
                    .subscribeOnInstallRegistration()
                    .catch { }
                    .onEach { result ->
                        runOnMain {
                            value?.let {
                                when (result) {
                                    is AdaptyResult.Success -> value.onInstallationDetailsSuccess(result.value)
                                    is AdaptyResult.Error -> value.onInstallationDetailsFailure(result.error)
                                }
                            }
                        }
                    }
                    .collect()
            }
            field = value
        }

    fun init(appKey: String) {
        userAcquisitionInteractor.handleFirstLaunch()
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
        obfuscatedAccountId: String?,
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
            authInteractor.prepareAuthDataToSync(customerUserId, obfuscatedAccountId)

            authInteractor
                .activateOrIdentify()
                .onSingleResult { result ->
                    analyticsTracker.trackSystemEvent(
                        SDKMethodResponseData.create(requestEvent, result.errorOrNull())
                    )
                    callback?.onResult(result.errorOrNull())
                    if (isInitialActivation) {
                        setupStartRequests()
                        handleNewSession()
                    }
                }
                .collect()
        }
        execute { paywallInteractor.getProductsOnStart().catch { }.collect() }
    }

    @JvmSynthetic
    fun identify(customerUserId: String, obfuscatedAccountId: String?, callback: ErrorCallback) {
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
            authInteractor.prepareAuthDataToSync(customerUserId, obfuscatedAccountId)

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
        if (authInteractor.getCustomerUserId() == null) {
            val requestEvent = SDKMethodRequestData.create("logout")
            val errorMessage = "Logout cannot be called for an unidentified user"
            Logger.log(ERROR) { errorMessage }
            val e = AdaptyError(
                message = errorMessage,
                adaptyErrorCode = LOGGING_OUT_UNIDENTIFIED_USER,
            )
            analyticsTracker.trackSystemEvent(
                SDKMethodResponseData.create(requestEvent, e)
            )
            callback.onResult(e)
            return
        }
        authInteractor.clearDataOnLogout()
        activate(null, null, callback, false)
    }

    @JvmSynthetic
    fun makePurchase(
        activity: Activity,
        product: AdaptyPaywallProduct,
        params: AdaptyPurchaseParameters,
        callback: ResultCallback<AdaptyPurchaseResult>
    ) {
        val requestEvent = SDKMethodRequestData.MakePurchase.create(product)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            purchasesInteractor.makePurchase(activity, product, params)
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
        fetchPolicy: AdaptyPlacementFetchPolicy,
        loadTimeout: TimeInterval,
        callback: ResultCallback<AdaptyPaywall>
    ) {
        val requestEvent = SDKMethodRequestData.GetPaywall.create(id, locale, fetchPolicy, loadTimeout.toMillis())
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            paywallInteractor
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
        fetchPolicy: AdaptyPlacementFetchPolicy,
        callback: ResultCallback<AdaptyPaywall>
    ) {
        val requestEvent = SDKMethodRequestData.GetUntargetedPaywall.create(id, locale, fetchPolicy)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            paywallInteractor
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
            paywallInteractor
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
        val requestEvent = SDKMethodRequestData.GetPaywallProducts.create(paywall.placement.id)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            paywallInteractor
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
    fun getOnboarding(
        id: String,
        locale: String,
        fetchPolicy: AdaptyPlacementFetchPolicy,
        loadTimeout: TimeInterval,
        callback: ResultCallback<AdaptyOnboarding>
    ) {
        val requestEvent = SDKMethodRequestData.GetOnboarding.create(id, locale, fetchPolicy, loadTimeout.toMillis())
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            onboardingInteractor
                .getOnboarding(id, locale, fetchPolicy, loadTimeout.coerceAtLeast(MIN_PAYWALL_TIMEOUT).toMillis())
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
    fun getOnboardingForDefaultAudience(
        id: String,
        locale: String,
        fetchPolicy: AdaptyPlacementFetchPolicy,
        callback: ResultCallback<AdaptyOnboarding>
    ) {
        val requestEvent = SDKMethodRequestData.GetUntargetedOnboarding.create(id, locale, fetchPolicy)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            onboardingInteractor
                .getOnboardingUntargeted(id, locale, fetchPolicy)
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
    fun setFallback(source: FileLocation, callback: ErrorCallback?) {
        val requestEvent = SDKMethodRequestData.create("set_fallback")
        analyticsTracker.trackSystemEvent(requestEvent)
        if (source is FileLocation.Uri) {
            val scheme = source.uri.scheme?.lowercase(Locale.ENGLISH)
            if (scheme.orEmpty() !in setOf(ContentResolver.SCHEME_ANDROID_RESOURCE, ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE)) {
                val errorMessage = "The fallback file URI has an unsupported scheme: $scheme"
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
            paywallInteractor
                .setFallback(source)
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

    fun logShowOnboardingInternal(
        onboarding: AdaptyOnboarding,
        screenName: String?,
        screenOrder: Int,
        isLastScreen: Boolean,
    ) {
        onboardingInteractor.logShowOnboardingInternal(onboarding, screenName, screenOrder, isLastScreen)
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
    fun getCurrentInstallationStatus(callback: ResultCallback<AdaptyInstallationStatus>) {
        val requestEvent = SDKMethodRequestData.create("get_current_installation_status")
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            userAcquisitionInteractor
                .getCurrentInstallationStatus()
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

    @JvmSynthetic
    fun createWebPaywallUrl(
        paywall: AdaptyPaywall,
        callback: ResultCallback<Uri>,
    ) {
        runCatching {
            val url = paywallInteractor.createWebPaywallUrl(paywall)
            callback.onResult(AdaptyResult.Success(url))
        }.getOrElse { e ->
            callback.onResult(AdaptyResult.Error(AdaptyError(originalError = e, message = e.localizedMessage ?: e.message.orEmpty(), adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED)))
        }
    }

    @JvmSynthetic
    fun createWebPaywallUrl(
        product: AdaptyPaywallProduct,
        callback: ResultCallback<Uri>,
    ) {
        runCatching {
            val url = paywallInteractor.createWebPaywallUrl(product)
            callback.onResult(AdaptyResult.Success(url))
        }.getOrElse { e ->
            callback.onResult(AdaptyResult.Error(AdaptyError(originalError = e, message = e.localizedMessage ?: e.message.orEmpty(), adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED)))
        }
    }

    @JvmSynthetic
    fun openWebPaywall(
        activity: Activity,
        paywall: AdaptyPaywall,
        presentation: AdaptyWebPresentation,
        callback: ErrorCallback,
    ) {
        runCatching {
            paywallInteractor.openWebPaywall(activity, paywall, presentation)
        }.exceptionOrNull()
            .let { e ->
                callback.onResult(
                    e?.let {
                        AdaptyError(originalError = e, message = e.localizedMessage ?: e.message.orEmpty(), adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED)
                    }
                )
            }
    }

    @JvmSynthetic
    fun openWebPaywall(
        activity: Activity,
        product: AdaptyPaywallProduct,
        presentation: AdaptyWebPresentation,
        callback: ErrorCallback,
    ) {
        runCatching {
            paywallInteractor.openWebPaywall(activity, product, presentation)
        }.exceptionOrNull()
            .let { e ->
                callback.onResult(
                    e?.let {
                        AdaptyError(originalError = e, message = e.localizedMessage ?: e.message.orEmpty(), adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED)
                    }
                )
            }
    }

    private fun handleNewSession() {
        execute {
            userAcquisitionInteractor
                .handleNewSession()
                .catch { e -> Logger.log(ERROR) { e.localizedMessage.orEmpty() } }
                .collect()
        }
    }
}