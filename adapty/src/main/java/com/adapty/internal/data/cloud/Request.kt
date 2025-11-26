package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cache.ResponseCacheKeyProvider
import com.adapty.internal.data.cache.ResponseCacheKeys
import com.adapty.internal.data.cloud.Request.Method.*
import com.adapty.internal.data.models.AnalyticsEvent
import com.adapty.internal.data.models.AnalyticsEvent.BackendAPIRequestData
import com.adapty.internal.data.models.AttributionData
import com.adapty.internal.data.models.InstallationMeta
import com.adapty.internal.data.models.RestoreProductInfo
import com.adapty.internal.data.models.InstallRegistrationData
import com.adapty.internal.data.models.requests.*
import com.adapty.internal.domain.models.IdentityParams
import com.adapty.internal.utils.DEFAULT_PLACEMENT_LOCALE
import com.adapty.internal.utils.ID
import com.adapty.internal.utils.MetaInfoRetriever
import com.adapty.internal.utils.PayloadProvider
import com.adapty.internal.utils.VERSION_NAME
import com.adapty.internal.utils.extractLanguageCode
import com.adapty.models.AdaptyConfig
import com.adapty.models.AdaptyProfileParameters
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.google.gson.Gson

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class Request(
    val url: String,
    val method: Method,
    val body: String?,
    val headers: Set<Header>,
    val baseUrl: String,
    val endpointTemplate: String?,
    val responseCacheKeys: ResponseCacheKeys?,
    val currentDataWhenSent: CurrentDataWhenSent?,
    val systemLog: BackendAPIRequestData?,
) {
    class Builder(private val baseUrl: String, private val method: Method) {

        var endPoint: String? = null

        var body: String? = null

        var headers = mutableSetOf<Header>()

        var currentDataWhenSent: CurrentDataWhenSent? = null

        var responseCacheKeys: ResponseCacheKeys? = null

        var systemLog: BackendAPIRequestData? = null

        var endpointTemplate: String? = null

        private val queryParams = arrayListOf<Pair<String, String>>()

        private fun queryDelimiter(index: Int) = if (index == 0) "?" else "&"

        fun addQueryParam(param: Pair<String, String>) {
            queryParams.add(param)
        }

        fun build() = Request(
            StringBuilder(baseUrl).apply {
                endPoint?.let(::append)
                queryParams.forEachIndexed { i, (key, value) ->
                    append(queryDelimiter(i))
                    append(key)
                    append("=")
                    append(value)
                }
            }.toString(),
            method,
            body,
            headers,
            baseUrl,
            endpointTemplate,
            responseCacheKeys,
            currentDataWhenSent,
            systemLog,
        )
    }

    enum class Method {
        GET, POST, PATCH
    }

    class Header(val key: String, val value: String?) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Header

            return key == other.key
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }
    }

    class CurrentDataWhenSent private constructor(profileId: ID<String>, customerUserId: ID<String?>) {

        val profileId: String = profileId.value

        val customerUserId: String? = customerUserId.value

        companion object {
            fun create(profileId: String) =
                CurrentDataWhenSent(ID(profileId), ID.UNSPECIFIED)

            fun create(profileId: String, customerUserId: String?) =
                CurrentDataWhenSent(ID(profileId), ID(customerUserId))
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class MainRequestFactory(
    private val cacheRepository: CacheRepository,
    private val responseCacheKeyProvider: ResponseCacheKeyProvider,
    private val metaInfoRetriever: MetaInfoRetriever,
    private val payloadProvider: PayloadProvider,
    private val netConfigManager: NetConfigManager,
    private val gson: Gson,
    private val adaptyConfig: AdaptyConfig,
) {

    private val sdkPrefix = "/sdk"
    private val inappsPrefix = "$sdkPrefix/in-apps"
    private val profilesPrefix = "$sdkPrefix/analytics/profiles"
    private val integrationPrefix = "$sdkPrefix/integration"
    private val attributionPrefix = "$sdkPrefix/attribution"
    private val purchasePrefix = "$sdkPrefix/purchase"
    private val eventsPrefix = "$sdkPrefix/events"

    private val apiKeyPrefix get() = adaptyConfig.apiKeyPrefix

    private val endpointPatternForProfileRequests = "$profilesPrefix/profileId/"

    private fun getEndpointForProfileRequests(profileId: String): String {
        return "$profilesPrefix/$profileId/"
    }

    fun getProfileRequest() =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest(GET) {
                endPoint = getEndpointForProfileRequests(profileId)
                endpointTemplate = endpointPatternForProfileRequests
                addResponseCacheKeys(responseCacheKeyProvider.forGetProfile())
                currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
                systemLog = BackendAPIRequestData.GetProfile.create()
            }
        }

    fun updateProfileRequest(params: AdaptyProfileParameters?, installationMeta: InstallationMeta?, ipv4Address: String?) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest(PATCH) {
                body = gson.toJson(
                    CreateOrUpdateProfileRequest.create(
                        profileId,
                        installationMeta,
                        params,
                        ipv4Address,
                    )
                )
                endPoint = getEndpointForProfileRequests(profileId)
                endpointTemplate = endpointPatternForProfileRequests
                addResponseCacheKeys(responseCacheKeyProvider.forGetProfile())
                currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
                systemLog = BackendAPIRequestData.UpdateProfile.create()
            }
        }

    fun setIntegrationIdRequest(key: String, value: String) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest(POST) {
                body = gson.toJson(
                    SetIntegrationIdRequest.create(profileId, key, value)
                )
                endPoint = "$integrationPrefix/profile/set/integration-identifiers/"
                    .also { endpointTemplate = it }
                headers += listOf(Request.Header("Content-type", "application/json"))
                systemLog = BackendAPIRequestData.SetIntegrationId.create(key, value)
            }
        }

    fun createProfileRequest(
        identityParams: IdentityParams?,
        installationMeta: InstallationMeta,
        params: AdaptyProfileParameters?,
    ) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest(POST) {
                body = gson.toJson(
                    CreateOrUpdateProfileRequest.create(
                        profileId,
                        installationMeta,
                        identityParams,
                        params,
                    )
                )
                endPoint = getEndpointForProfileRequests(profileId)
                endpointTemplate = endpointPatternForProfileRequests
                systemLog = BackendAPIRequestData.CreateProfile.create(!identityParams?.customerUserId.isNullOrEmpty())
            }
        }

    fun validatePurchaseRequest(
        validateData: ValidateReceiptRequest,
        purchase: Purchase?,
    ) = cacheRepository.getProfileId().let { profileId ->
        buildRequest(POST) {
            endPoint = "$purchasePrefix/play-store/token/v2/validate/"
                .also { endpointTemplate = it }
            body = gson.toJson(validateData)
            currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
            systemLog = BackendAPIRequestData.Validate.create(validateData, purchase)
        }
    }

    fun reportTransactionWithVariationRequest(
        transactionId: String,
        variationId: String,
        purchase: Purchase,
        product: ProductDetails,
    ) = cacheRepository.getProfileId().let { profileId ->
        buildRequest(POST) {
            endPoint = "$purchasePrefix/play-store/token/v2/validate/"
                .also { endpointTemplate = it }
            body = gson.toJson(
                ValidateReceiptRequest.create(
                    profileId,
                    variationId,
                    cacheRepository.getOnboardingVariationId(),
                    purchase,
                    product
                )
            )
            currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
            systemLog = BackendAPIRequestData.ReportTransaction.create(transactionId, variationId)
        }
    }

    fun restorePurchasesRequest(purchases: List<RestoreProductInfo>) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest(POST) {
                body = gson.toJson(
                    RestoreReceiptRequest.create(profileId, purchases)
                )
                endPoint = "$purchasePrefix/play-store/token/v2/restore/"
                    .also { endpointTemplate = it }
                currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId, cacheRepository.getCustomerUserId()?.takeIf(String::isNotBlank))
                systemLog = BackendAPIRequestData.Restore.create(purchases)
            }
        }

    fun getProductsRequest() = buildRequest(GET) {
        endPoint = "$inappsPrefix/$apiKeyPrefix/products/${metaInfoRetriever.store}/${cacheRepository.getDisableCacheQueryParamOrEmpty()}"
        endpointTemplate = "$inappsPrefix/$apiKeyPrefix/products/store/"
        addResponseCacheKeys(responseCacheKeyProvider.forGetProducts())
        systemLog = BackendAPIRequestData.GetProducts.create()
    }

    fun getPaywallVariationsRequest(id: String, locale: String, segmentId: String) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest(GET) {
                val builderVersion = metaInfoRetriever.builderVersion
                val crossPlacementEligibility =
                    cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.isEmpty() ?: false
                val payloadHash = payloadProvider.getPayloadHashForPaywallRequest(
                    locale,
                    segmentId,
                    builderVersion,
                    crossPlacementEligibility,
                )
                endPoint = "$inappsPrefix/$apiKeyPrefix/paywall/variations/$id/$payloadHash/${cacheRepository.getDisableCacheQueryParamOrEmpty()}"
                endpointTemplate = "$inappsPrefix/$apiKeyPrefix/paywall/variations/id/payloadHash/"
                headers += listOfNotNull(
                    Request.Header("adapty-paywall-locale", locale),
                    Request.Header("adapty-paywall-builder-version", builderVersion),
                    Request.Header("adapty-profile-segment-hash", segmentId),
                    Request.Header("adapty-cross-placement-eligibility", "$crossPlacementEligibility"),
                    metaInfoRetriever.adaptyUiVersionOrNull?.let { adaptyUiVersion ->
                        Request.Header("adapty-ui-version", adaptyUiVersion)
                    },
                )
                currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
                systemLog = BackendAPIRequestData.GetPaywallVariations.create(apiKeyPrefix, id, locale, segmentId, payloadHash)
            }
        }

    fun getOnboardingVariationsRequest(id: String, locale: String, segmentId: String) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest(GET) {
                val crossPlacementEligibility =
                    cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.isEmpty() ?: false
                val payloadHash = payloadProvider.getPayloadHashForOnboardingRequest(
                    locale,
                    segmentId,
                    crossPlacementEligibility,
                )
                endPoint = "$inappsPrefix/$apiKeyPrefix/onboarding/variations/$id/$payloadHash/${cacheRepository.getDisableCacheQueryParamOrEmpty()}"
                endpointTemplate = "$inappsPrefix/$apiKeyPrefix/onboarding/variations/id/payloadHash/"
                headers += listOfNotNull(
                    Request.Header("adapty-profile-segment-hash", segmentId),
                    Request.Header("adapty-cross-placement-eligibility", "$crossPlacementEligibility"),
                    Request.Header("adapty-onboarding-locale", locale),
                )
                currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
                systemLog = BackendAPIRequestData.GetOnboardingVariations.create(apiKeyPrefix, id, locale, segmentId, payloadHash)
            }
        }

    fun getPaywallByVariationIdRequest(id: String, locale: String, segmentId: String, variationId: String) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest(GET) {
                val builderVersion = metaInfoRetriever.builderVersion
                val crossPlacementEligibility =
                    cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.isEmpty() ?: false
                val payloadHash = payloadProvider.getPayloadHashForPaywallRequest(
                    locale,
                    segmentId,
                    builderVersion,
                    crossPlacementEligibility,
                )
                endPoint = "$inappsPrefix/$apiKeyPrefix/paywall/variations/$id/$payloadHash/$variationId/${cacheRepository.getDisableCacheQueryParamOrEmpty()}"
                endpointTemplate = "$inappsPrefix/$apiKeyPrefix/paywall/variations/id/payloadHash/variationId/"
                headers += listOfNotNull(
                    Request.Header("adapty-paywall-locale", locale),
                    Request.Header("adapty-paywall-builder-version", builderVersion),
                    metaInfoRetriever.adaptyUiVersionOrNull?.let { adaptyUiVersion ->
                        Request.Header("adapty-ui-version", adaptyUiVersion)
                    },
                )
                currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
                systemLog = BackendAPIRequestData.GetPaywall.create(apiKeyPrefix, id, locale, variationId)
            }
        }

    fun getOnboardingByVariationIdRequest(id: String, locale: String, segmentId: String, variationId: String) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest(GET) {
                val crossPlacementEligibility =
                    cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.isEmpty() ?: false
                val payloadHash = payloadProvider.getPayloadHashForOnboardingRequest(
                    locale,
                    segmentId,
                    crossPlacementEligibility,
                )
                endPoint = "$inappsPrefix/$apiKeyPrefix/onboarding/variations/$id/$payloadHash/$variationId/${cacheRepository.getDisableCacheQueryParamOrEmpty()}"
                endpointTemplate = "$inappsPrefix/$apiKeyPrefix/onboarding/variations/id/payloadHash/variationId/"
                headers += listOfNotNull(
                    Request.Header("adapty-paywall-locale", locale),
                )
                currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
                systemLog = BackendAPIRequestData.GetOnboarding.create(apiKeyPrefix, id, locale, variationId)
            }
        }

    fun getViewConfigurationRequest(variationId: String, locale: String) = buildRequest(GET) {
        val adaptyUiVersion = metaInfoRetriever.adaptyUiVersion
        val builderVersion = metaInfoRetriever.builderVersion
        val payloadHash = payloadProvider.getPayloadHashForPaywallBuilderRequest(locale, builderVersion)
        endPoint = "$inappsPrefix/$apiKeyPrefix/paywall-builder/$variationId/$payloadHash/${cacheRepository.getDisableCacheQueryParamOrEmpty()}"
        endpointTemplate = "$inappsPrefix/$apiKeyPrefix/paywall-builder/variationId/payloadHash/"
        headers += listOf(
            Request.Header("adapty-paywall-builder-locale", locale),
            Request.Header("adapty-paywall-builder-version", builderVersion),
            Request.Header("adapty-ui-version", adaptyUiVersion),
        )
        systemLog = BackendAPIRequestData.GetPaywallBuilder.create(variationId)
    }

    fun updateAttributionRequest(
        attributionData: AttributionData,
    ) = cacheRepository.getProfileId().let { profileId ->
        buildRequest(POST) {
            endPoint = "$attributionPrefix/profile/set/data/"
                .also { endpointTemplate = it }
            body = gson.toJson(attributionData)
            headers += listOf(Request.Header("Content-type", "application/json"))
            currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
            systemLog = BackendAPIRequestData.SetAttribution.create(attributionData)
        }
    }

    fun setVariationIdRequest(transactionId: String, variationId: String) =
        buildRequest(POST) {
            endPoint = "$purchasePrefix/transaction/variation-id/set/"
                .also { endpointTemplate = it }
            body = gson.toJson(
                SetVariationIdRequest.create(transactionId, variationId)
            )
            systemLog = BackendAPIRequestData.SetVariationId.create(transactionId, variationId)
        }

    fun getCrossPlacementInfoRequest(replacementProfileId: String?) = buildRequest(GET) {
        endPoint = "$inappsPrefix/profile/cross-placement-info/"
            .also { endpointTemplate = it }
        if (replacementProfileId != null)
            headers += listOf(Request.Header("adapty-sdk-profile-id", replacementProfileId))
        systemLog = BackendAPIRequestData.GetCrossPlacementInfo.create()
    }

    fun sendAnalyticsEventsRequest(events: List<AnalyticsEvent>) =
        buildRequest(POST) {
            endPoint = "$eventsPrefix/"
                .also { endpointTemplate = it }
            body = gson.toJson(SendEventRequest.create(events))
        }

    private inline fun buildRequest(method: Request.Method, action: Request.Builder.() -> Unit) =
        Request.Builder(netConfigManager.getBaseUrl(), method).apply {
            action()
            if (method != GET)
                headers += listOf(Request.Header("Content-type", "application/vnd.api+json"))
            addDefaultHeaders()
        }.build()

    private fun Request.Builder.addDefaultHeaders() {
        val defaultHeaders = setOfNotNull(
            Request.Header("Accept-Encoding", "gzip"),
            Request.Header("adapty-sdk-profile-id", cacheRepository.getProfileId()),
            Request.Header("adapty-sdk-platform", "Android"),
            Request.Header("adapty-sdk-version", VERSION_NAME),
            Request.Header("adapty-sdk-session", cacheRepository.getSessionId()),
            Request.Header("adapty-sdk-device-id", metaInfoRetriever.installationMetaId),
            Request.Header("adapty-sdk-observer-mode-enabled", "${adaptyConfig.observerMode}"),
            Request.Header("adapty-sdk-android-billing-new", "true"),
            Request.Header("adapty-sdk-store", metaInfoRetriever.store),
            Request.Header(AUTHORIZATION_KEY, "$API_KEY_PREFIX${adaptyConfig.apiKey}"),
            metaInfoRetriever.appBuildAndVersion.let { (_, appVersion) ->
                Request.Header("adapty-app-version", appVersion)
            },
        )

        val crossplatformHeaders = metaInfoRetriever.crossplatformNameAndVersion?.let { (name, version) ->
            setOf(
                Request.Header("adapty-sdk-crossplatform-name", name),
                Request.Header("adapty-sdk-crossplatform-version", version),
            )
        }

        headers += defaultHeaders

        if (crossplatformHeaders != null)
            headers += crossplatformHeaders
    }

    private fun Request.Builder.addResponseCacheKeys(keys: ResponseCacheKeys) {
        responseCacheKeys = keys
        headers += setOfNotNull(
            cacheRepository.getString(keys.responseHashKey)?.let { latestResponseHash ->
                Request.Header("adapty-sdk-previous-response-hash", latestResponseHash)
            }
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AuxRequestFactory(
    private val cacheRepository: CacheRepository,
    private val metaInfoRetriever: MetaInfoRetriever,
    private val gson: Gson,
    private val adaptyConfig: AdaptyConfig,
) {

    private val sdkPrefix = "/sdk"
    private val inappsPrefix = "$sdkPrefix/in-apps"

    private val apiKeyPrefix get() = adaptyConfig.apiKeyPrefix
    private val serverCluster get() = adaptyConfig.serverCluster

    fun registerInstallRequest(installRegistrationData: InstallRegistrationData, retryAttempt: Long, maxRetries: Long) = buildRequest(serverCluster.uaBaseUrl, POST) {
        body = gson.toJson(installRegistrationData)
        endPoint = "/attribution/install"
            .also { endpointTemplate = it }
        headers += listOfNotNull(
            Request.Header("Content-type", "application/json"),
            Request.Header("adapty-sdk-profile-id", cacheRepository.getProfileId()),
            Request.Header("adapty-sdk-device-id", metaInfoRetriever.installationMetaId),
            Request.Header("adapty-sdk-version", VERSION_NAME),
            Request.Header(AUTHORIZATION_KEY, "$API_KEY_PREFIX${adaptyConfig.apiKey}"),
        )
        systemLog = BackendAPIRequestData.RegisterInstall.create(retryAttempt, maxRetries)
    }

    fun getPaywallVariationsFallbackRequest(id: String, locale: String) = buildRequest(serverCluster.fallbackBaseUrl, GET) {
        val languageCode = extractLanguageCode(locale) ?: DEFAULT_PLACEMENT_LOCALE
        val builderVersion = metaInfoRetriever.builderVersion
        endPoint = "$inappsPrefix/$apiKeyPrefix/paywall/variations/$id/${metaInfoRetriever.store}/$languageCode/$builderVersion/fallback.json${cacheRepository.getDisableCacheQueryParamOrEmpty()}"
        systemLog = BackendAPIRequestData.GetFallbackPaywallVariations.create(apiKeyPrefix, id, languageCode)
    }

    fun getOnboardingVariationsFallbackRequest(id: String, locale: String) = buildRequest(serverCluster.fallbackBaseUrl, GET) {
        val languageCode = extractLanguageCode(locale) ?: DEFAULT_PLACEMENT_LOCALE
        endPoint = "$inappsPrefix/$apiKeyPrefix/onboarding/variations/$id/$languageCode/fallback.json${cacheRepository.getDisableCacheQueryParamOrEmpty()}"
        systemLog = BackendAPIRequestData.GetFallbackOnboardingVariations.create(apiKeyPrefix, id, languageCode)
    }

    fun getPaywallByVariationIdFallbackRequest(id: String, locale: String, variationId: String) = buildRequest(serverCluster.fallbackBaseUrl, GET) {
        val languageCode = extractLanguageCode(locale) ?: DEFAULT_PLACEMENT_LOCALE
        val builderVersion = metaInfoRetriever.builderVersion
        endPoint = "$inappsPrefix/$apiKeyPrefix/paywall/variations/$id/${variationId}/${metaInfoRetriever.store}/$languageCode/$builderVersion/fallback.json${cacheRepository.getDisableCacheQueryParamOrEmpty()}"
        systemLog = BackendAPIRequestData.GetFallbackPaywall.create(apiKeyPrefix, id, languageCode, variationId)
    }

    fun getOnboardingByVariationIdFallbackRequest(id: String, locale: String, variationId: String) = buildRequest(serverCluster.fallbackBaseUrl, GET) {
        val languageCode = extractLanguageCode(locale) ?: DEFAULT_PLACEMENT_LOCALE
        endPoint = "$inappsPrefix/$apiKeyPrefix/onboarding/variations/$id/${variationId}/$languageCode/fallback.json${cacheRepository.getDisableCacheQueryParamOrEmpty()}"
        systemLog = BackendAPIRequestData.GetFallbackOnboarding.create(apiKeyPrefix, id, languageCode, variationId)
    }

    fun getPaywallVariationsUntargetedRequest(id: String, locale: String) = buildRequest(serverCluster.configsCdnBaseUrl, GET) {
        val languageCode = extractLanguageCode(locale) ?: DEFAULT_PLACEMENT_LOCALE
        val builderVersion = metaInfoRetriever.builderVersion
        endPoint = "$inappsPrefix/$apiKeyPrefix/paywall/variations/$id/${metaInfoRetriever.store}/$languageCode/$builderVersion/fallback.json"
        systemLog = BackendAPIRequestData.GetUntargetedPaywallVariations.create(apiKeyPrefix, id, languageCode)
    }

    fun getOnboardingVariationsUntargetedRequest(id: String, locale: String) = buildRequest(serverCluster.configsCdnBaseUrl, GET) {
        val languageCode = extractLanguageCode(locale) ?: DEFAULT_PLACEMENT_LOCALE
        endPoint = "$inappsPrefix/$apiKeyPrefix/onboarding/variations/$id/$languageCode/fallback.json"
        systemLog = BackendAPIRequestData.GetUntargetedOnboardingVariations.create(apiKeyPrefix, id, languageCode)
    }

    fun getViewConfigurationFallbackRequest(paywallId: String, locale: String) = buildRequest(serverCluster.fallbackBaseUrl, GET) {
        val builderVersion = metaInfoRetriever.builderVersion
        val languageCode = extractLanguageCode(locale) ?: DEFAULT_PLACEMENT_LOCALE
        endPoint = "$inappsPrefix/$apiKeyPrefix/paywall-builder/$paywallId/$builderVersion/$languageCode/fallback.json${cacheRepository.getDisableCacheQueryParamOrEmpty()}"
        systemLog = BackendAPIRequestData.GetFallbackPaywallBuilder.create(apiKeyPrefix, paywallId, builderVersion, languageCode)
    }

    fun fetchNetConfigRequest() = buildRequest(serverCluster.fallbackBaseUrl, GET) {
        endPoint = "$sdkPrefix/company/${adaptyConfig.apiKeyPrefix}/app/net-config.json"
        systemLog = BackendAPIRequestData.GetNetConfig.create()
    }

    fun getIPv4Request() = Request.Builder("https://api.ipify.org?format=json", GET).build()

    private inline fun buildRequest(baseUrl: String, method: Request.Method, action: Request.Builder.() -> Unit) =
        Request.Builder(baseUrl, method).apply { action() }.build()
}

private fun CacheRepository.getDisableCacheQueryParamOrEmpty() =
    if (getProfile()?.isTestUser == true) "?disable_cache" else ""

private const val AUTHORIZATION_KEY = "Authorization"
private const val API_KEY_PREFIX = "Api-Key "
