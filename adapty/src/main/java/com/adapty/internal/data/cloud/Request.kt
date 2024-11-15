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
import com.adapty.internal.data.models.requests.*
import com.adapty.internal.domain.models.PurchaseableProduct
import com.adapty.internal.utils.DEFAULT_PAYWALL_LOCALE
import com.adapty.internal.utils.ID
import com.adapty.internal.utils.MetaInfoRetriever
import com.adapty.internal.utils.PayloadProvider
import com.adapty.internal.utils.VERSION_NAME
import com.adapty.internal.utils.extractLanguageCode
import com.adapty.models.AdaptyProfileParameters
import com.android.billingclient.api.Purchase
import com.google.gson.Gson

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class Request internal constructor(val baseUrl: String) {

    @get:JvmSynthetic
    @set:JvmSynthetic
    lateinit var method: Method

    @get:JvmSynthetic
    @set:JvmSynthetic
    lateinit var url: String

    @JvmSynthetic
    @JvmField
    var responseCacheKeys: ResponseCacheKeys? = null

    @JvmSynthetic
    @JvmField
    var body = ""

    @JvmSynthetic
    @JvmField
    var headers: Set<Header>? = null

    @JvmSynthetic
    @JvmField
    var currentDataWhenSent: CurrentDataWhenSent? = null

    @JvmSynthetic
    @JvmField
    var systemLog: BackendAPIRequestData? = null

    internal class Builder(private val baseRequest: Request) {
        constructor(baseUrl: String): this(Request("${baseUrl}api/v1/sdk/"))

        @get:JvmSynthetic
        @set:JvmSynthetic
        lateinit var method: Method

        @JvmSynthetic
        @JvmField
        var endPoint: String? = null

        @JvmSynthetic
        @JvmField
        var body: String? = null

        @JvmSynthetic
        @JvmField
        var headers = mutableSetOf<Header>()

        @JvmSynthetic
        @JvmField
        var currentDataWhenSent: CurrentDataWhenSent? = null

        @JvmSynthetic
        @JvmField
        var responseCacheKeys: ResponseCacheKeys? = null

        @JvmSynthetic
        @JvmField
        var systemLog: BackendAPIRequestData? = null

        private val queryParams = arrayListOf<Pair<String, String>>()

        private fun queryDelimiter(index: Int) = if (index == 0) "?" else "&"

        @JvmSynthetic
        fun addQueryParam(param: Pair<String, String>) {
            queryParams.add(param)
        }

        @JvmSynthetic
        fun build() = baseRequest.apply {
            method = this@Builder.method
            url = StringBuilder(baseUrl).apply {
                endPoint?.let(::append)
                queryParams.forEachIndexed { i, (key, value) ->
                    append(queryDelimiter(i))
                    append(key)
                    append("=")
                    append(value)
                }
            }.toString()
            body = this@Builder.body.orEmpty()
            headers = this@Builder.headers
            responseCacheKeys = this@Builder.responseCacheKeys
            currentDataWhenSent = this@Builder.currentDataWhenSent
            systemLog = this@Builder.systemLog
        }
    }

    internal enum class Method {
        GET, POST, PATCH
    }

    internal class Header(val key: String, val value: String?)

    internal class CurrentDataWhenSent private constructor(profileId: ID<String>, customerUserId: ID<String?>) {

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
internal class RequestFactory(
    private val cacheRepository: CacheRepository,
    private val responseCacheKeyProvider: ResponseCacheKeyProvider,
    private val metaInfoRetriever: MetaInfoRetriever,
    private val payloadProvider: PayloadProvider,
    private val gson: Gson,
    private val apiKey: String,
    private val isObserverMode: Boolean,
    private val backendBaseUrl: String,
) {

    private val inappsEndpointPrefix = "in-apps"
    private val profilesEndpointPrefix = "analytics/profiles"

    private val apiKeyPrefix = apiKey.split(".").getOrNull(0).orEmpty()

    private fun getEndpointForProfileRequests(profileId: String): String {
        return "$profilesEndpointPrefix/$profileId/"
    }

    @JvmSynthetic
    fun getProfileRequest() =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest {
                method = GET
                endPoint = getEndpointForProfileRequests(profileId)
                addResponseCacheKeys(responseCacheKeyProvider.forGetProfile())
                currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
                systemLog = BackendAPIRequestData.GetProfile.create()
            }
        }

    @JvmSynthetic
    fun updateProfileRequest(params: AdaptyProfileParameters?, installationMeta: InstallationMeta?, ipv4Address: String?) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest {
                method = PATCH
                body = gson.toJson(
                    CreateOrUpdateProfileRequest.create(
                        profileId,
                        installationMeta,
                        params,
                        ipv4Address,
                    )
                )
                endPoint = getEndpointForProfileRequests(profileId)
                addResponseCacheKeys(responseCacheKeyProvider.forGetProfile())
                currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
                systemLog = BackendAPIRequestData.UpdateProfile.create()
            }
        }

    @JvmSynthetic
    fun createProfileRequest(
        customerUserId: String?,
        installationMeta: InstallationMeta,
        params: AdaptyProfileParameters?,
    ) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest {
                method = POST
                body = gson.toJson(
                    CreateOrUpdateProfileRequest.create(
                        profileId,
                        installationMeta,
                        customerUserId,
                        params,
                    )
                )
                endPoint = getEndpointForProfileRequests(profileId)
                systemLog = BackendAPIRequestData.CreateProfile.create(!customerUserId.isNullOrEmpty())
            }
        }

    @JvmSynthetic
    fun validatePurchaseRequest(
        purchase: Purchase,
        product: PurchaseableProduct,
    ) = cacheRepository.getProfileId().let { profileId ->
        buildRequest {
            method = POST
            endPoint = "purchase/play-store/token/v2/validate/"
            body = gson.toJson(
                ValidateReceiptRequest.create(profileId, purchase, product)
            )
            currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
            systemLog = BackendAPIRequestData.Validate.create(product, purchase)
        }
    }

    @JvmSynthetic
    fun restorePurchasesRequest(purchases: List<RestoreProductInfo>) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest {
                method = POST
                body = gson.toJson(
                    RestoreReceiptRequest.create(profileId, purchases)
                )
                endPoint = "purchase/play-store/token/v2/restore/"
                currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId, cacheRepository.getCustomerUserId()?.takeIf(String::isNotBlank))
                systemLog = BackendAPIRequestData.Restore.create(purchases)
            }
        }

    @JvmSynthetic
    fun getProductIdsRequest() = buildRequest {
        method = GET
        endPoint = "$inappsEndpointPrefix/$apiKeyPrefix/products-ids/${metaInfoRetriever.store}/${getDisableCacheQueryParamOrEmpty()}"
        addResponseCacheKeys(responseCacheKeyProvider.forGetProductIds())
        systemLog = BackendAPIRequestData.GetProductIds.create()
    }

    @JvmSynthetic
    fun getPaywallVariationsRequest(id: String, locale: String, segmentId: String) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest {
                method = GET
                val builderVersion = metaInfoRetriever.builderVersion
                val payloadHash = payloadProvider.getPayloadHashForPaywallRequest(locale, segmentId, builderVersion)
                endPoint = "$inappsEndpointPrefix/$apiKeyPrefix/paywall/variations/$id/$payloadHash/${getDisableCacheQueryParamOrEmpty()}"
                headers += listOfNotNull(
                    Request.Header("adapty-paywall-locale", locale),
                    Request.Header("adapty-paywall-builder-version", builderVersion),
                    Request.Header("adapty-profile-segment-hash", segmentId),
                    metaInfoRetriever.adaptyUiVersionOrNull?.let { adaptyUiVersion ->
                        Request.Header("adapty-ui-version", adaptyUiVersion)
                    },
                )
                currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
                systemLog = BackendAPIRequestData.GetPaywall.create(apiKeyPrefix, id, locale, segmentId, payloadHash)
            }
        }

    @JvmSynthetic
    fun getPaywallVariationsFallbackRequest(id: String, locale: String) = Request.Builder(baseRequest = Request("https://fallback.adapty.io/api/v1/sdk/")).apply {
        method = GET
        val languageCode = extractLanguageCode(locale) ?: DEFAULT_PAYWALL_LOCALE
        val builderVersion = metaInfoRetriever.builderVersion
        endPoint = "$inappsEndpointPrefix/$apiKeyPrefix/paywall/variations/$id/${metaInfoRetriever.store}/$languageCode/$builderVersion/fallback.json${getDisableCacheQueryParamOrEmpty()}"
        systemLog = BackendAPIRequestData.GetFallbackPaywall.create(apiKeyPrefix, id, languageCode)
    }.build()

    @JvmSynthetic
    fun getPaywallVariationsUntargetedRequest(id: String, locale: String) = Request.Builder(baseRequest = Request("https://configs-cdn.adapty.io/api/v1/sdk/")).apply {
        method = GET
        val languageCode = extractLanguageCode(locale) ?: DEFAULT_PAYWALL_LOCALE
        val builderVersion = metaInfoRetriever.builderVersion
        endPoint = "$inappsEndpointPrefix/$apiKeyPrefix/paywall/variations/$id/${metaInfoRetriever.store}/$languageCode/$builderVersion/fallback.json"
        systemLog = BackendAPIRequestData.GetUntargetedPaywall.create(apiKeyPrefix, id, languageCode)
    }.build()

    @JvmSynthetic
    fun getViewConfigurationRequest(variationId: String, locale: String) = buildRequest {
        method = GET
        val adaptyUiVersion = metaInfoRetriever.adaptyUiVersion
        val builderVersion = metaInfoRetriever.builderVersion
        val payloadHash = payloadProvider.getPayloadHashForPaywallBuilderRequest(locale, builderVersion)
        endPoint = "$inappsEndpointPrefix/$apiKeyPrefix/paywall-builder/$variationId/$payloadHash/${getDisableCacheQueryParamOrEmpty()}"
        headers += listOf(
            Request.Header("adapty-paywall-builder-locale", locale),
            Request.Header("adapty-paywall-builder-version", builderVersion),
            Request.Header("adapty-ui-version", adaptyUiVersion),
        )
        systemLog = BackendAPIRequestData.GetPaywallBuilder.create(variationId)
    }

    @JvmSynthetic
    fun getViewConfigurationFallbackRequest(paywallId: String, locale: String) = Request.Builder(baseRequest = Request("https://fallback.adapty.io/api/v1/sdk/")).apply {
        method = GET
        val builderVersion = metaInfoRetriever.builderVersion
        val languageCode = extractLanguageCode(locale) ?: DEFAULT_PAYWALL_LOCALE
        endPoint = "$inappsEndpointPrefix/$apiKeyPrefix/paywall-builder/$paywallId/$builderVersion/$languageCode/fallback.json${getDisableCacheQueryParamOrEmpty()}"
        systemLog = BackendAPIRequestData.GetFallbackPaywallBuilder.create(apiKeyPrefix, paywallId, builderVersion, languageCode)
    }.build()

    @JvmSynthetic
    fun updateAttributionRequest(
        attributionData: AttributionData,
    ) = cacheRepository.getProfileId().let { profileId ->
        buildRequest {
            method = POST
            endPoint = "${getEndpointForProfileRequests(profileId)}attribution/"
            body = gson.toJson(
                UpdateAttributionRequest.create(attributionData)
            )
            currentDataWhenSent = Request.CurrentDataWhenSent.create(profileId)
            systemLog = BackendAPIRequestData.SetAttribution.create(attributionData)
        }
    }

    @JvmSynthetic
    fun setVariationIdRequest(transactionId: String, variationId: String) =
        buildRequest {
            method = POST
            endPoint = "purchase/transaction/variation-id/set/"
            body = gson.toJson(
                SetVariationIdRequest.create(transactionId, variationId)
            )
            systemLog = BackendAPIRequestData.SetVariationId.create(transactionId, variationId)
        }

    @JvmSynthetic
    fun getIPv4Request() = Request.Builder(baseRequest = Request("https://api.ipify.org?format=json")).apply {
        method = GET
    }.build()

    @JvmSynthetic
    fun sendAnalyticsEventsRequest(events: List<AnalyticsEvent>) =
        buildRequest {
            method = POST
            endPoint = "events/"
            body = gson.toJson(SendEventRequest.create(events))
        }

    @JvmSynthetic
    fun getAnalyticsConfig() =
        buildRequest {
            method = GET
            endPoint = "events/blacklist/"
            systemLog = BackendAPIRequestData.GetAnalyticsConfig.create()
        }

    private fun getDisableCacheQueryParamOrEmpty() =
        if (cacheRepository.getProfile()?.isTestUser == true) "?disable_cache" else ""

    private inline fun buildRequest(action: Request.Builder.() -> Unit) =
        Request.Builder(backendBaseUrl).apply {
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
            Request.Header("adapty-sdk-observer-mode-enabled", "$isObserverMode"),
            Request.Header("adapty-sdk-android-billing-new", "true"),
            Request.Header("adapty-sdk-store", metaInfoRetriever.store),
            Request.Header(AUTHORIZATION_KEY, "$API_KEY_PREFIX${apiKey}"),
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

    private companion object {
        private const val AUTHORIZATION_KEY = "Authorization"
        private const val API_KEY_PREFIX = "Api-Key "
    }
}