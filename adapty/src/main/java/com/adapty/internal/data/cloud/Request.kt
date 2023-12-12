package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cache.ResponseCacheKeyProvider
import com.adapty.internal.data.cache.ResponseCacheKeys
import com.adapty.internal.data.cloud.Request.Method.*
import com.adapty.internal.data.models.AttributionData
import com.adapty.internal.data.models.InstallationMeta
import com.adapty.internal.data.models.RestoreProductInfo
import com.adapty.internal.data.models.requests.*
import com.adapty.internal.domain.models.PurchaseableProduct
import com.adapty.internal.utils.MetaInfoRetriever
import com.adapty.internal.utils.PayloadProvider
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
    var additionalHeaders: List<Header>? = null

    @JvmSynthetic
    @JvmField
    var currentDataWhenSent: CurrentDataWhenSent? = null

    internal class Builder(private val baseRequest: Request = Request(baseUrl = "https://api.adapty.io/api/v1/sdk/")) {

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
        var additionalHeaders: List<Header>? = null

        @JvmSynthetic
        @JvmField
        var currentDataWhenSent: CurrentDataWhenSent? = null

        @JvmSynthetic
        @JvmField
        var responseCacheKeys: ResponseCacheKeys? = null

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
            additionalHeaders = this@Builder.additionalHeaders
            responseCacheKeys = this@Builder.responseCacheKeys
            currentDataWhenSent = this@Builder.currentDataWhenSent
        }
    }

    internal enum class Method {
        GET, POST, PATCH
    }

    internal class Header(val key: String, val value: String)

    internal class CurrentDataWhenSent(val profileId: String)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class RequestFactory(
    private val cacheRepository: CacheRepository,
    private val responseCacheKeyProvider: ResponseCacheKeyProvider,
    private val metaInfoRetriever: MetaInfoRetriever,
    private val payloadProvider: PayloadProvider,
    private val gson: Gson,
    private val apiKeyPrefix: String,
) {

    private val inappsEndpointPrefix = "in-apps"
    private val profilesEndpointPrefix = "analytics/profiles"

    private fun getEndpointForProfileRequests(profileId: String): String {
        return "$profilesEndpointPrefix/$profileId/"
    }

    @JvmSynthetic
    fun getProfileRequest() =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest {
                method = GET
                endPoint = getEndpointForProfileRequests(profileId)
                responseCacheKeys = responseCacheKeyProvider.forGetProfile()
                currentDataWhenSent = Request.CurrentDataWhenSent(profileId)
            }
        }

    @JvmSynthetic
    fun updateProfileRequest(params: AdaptyProfileParameters?, installationMeta: InstallationMeta?) =
        cacheRepository.getProfileId().let { profileId ->
            buildRequest {
                method = PATCH
                body = gson.toJson(
                    CreateOrUpdateProfileRequest.create(
                        profileId,
                        installationMeta,
                        params,
                    )
                )
                endPoint = getEndpointForProfileRequests(profileId)
                responseCacheKeys = responseCacheKeyProvider.forGetProfile()
                currentDataWhenSent = Request.CurrentDataWhenSent(profileId)
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
            }
        }

    @JvmSynthetic
    fun getAnalyticsCreds() = buildRequest {
        method = GET
        endPoint = "kinesis/credentials/"
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
            currentDataWhenSent = Request.CurrentDataWhenSent(profileId)
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
                currentDataWhenSent = Request.CurrentDataWhenSent(profileId)
            }
        }

    @JvmSynthetic
    fun getProductIdsRequest() = buildRequest {
        method = GET
        endPoint = "$inappsEndpointPrefix/$apiKeyPrefix/products-ids/${metaInfoRetriever.store}/"
        responseCacheKeys = responseCacheKeyProvider.forGetProductIds()
    }

    @JvmSynthetic
    fun getPaywallRequest(id: String, locale: String, segmentId: String) = buildRequest {
        method = GET
        val payloadHash = payloadProvider.getPayloadHashForPaywallRequest(locale, segmentId)
        endPoint = "$inappsEndpointPrefix/$apiKeyPrefix/paywall/$id/$payloadHash/"
        additionalHeaders = listOf(Request.Header("adapty-paywall-locale", locale))
    }

    @JvmSynthetic
    fun getPaywallFallbackRequest(id: String, locale: String) = Request.Builder(baseRequest = Request("https://fallback.adapty.io/api/v1/sdk/")).apply {
        method = GET
        val languageCode = extractLanguageCode(locale)
        endPoint = "$inappsEndpointPrefix/$apiKeyPrefix/paywall/$id/${metaInfoRetriever.store}/$languageCode/fallback.json"
        additionalHeaders = listOf(Request.Header("Content-type", "application/json"))
    }.build()

    @JvmSynthetic
    fun getViewConfigurationRequest(variationId: String, locale: String) = buildRequest {
        method = GET
        val (adaptyUiVersion, builderVersion) = metaInfoRetriever.adaptyUiAndBuilderVersion
        val payloadHash = payloadProvider.getPayloadHashForPaywallBuilderRequest(locale, builderVersion)
        endPoint = "$inappsEndpointPrefix/$apiKeyPrefix/paywall-builder/$variationId/$payloadHash/"
        additionalHeaders = listOf(
            Request.Header("adapty-paywall-builder-locale", locale),
            Request.Header("adapty-paywall-builder-version", builderVersion),
            Request.Header("adapty-ui-version", adaptyUiVersion),
        )
    }

    @JvmSynthetic
    fun getViewConfigurationFallbackRequest(paywallId: String, locale: String) = Request.Builder(baseRequest = Request("https://fallback.adapty.io/api/v1/sdk/")).apply {
        method = GET
        val (_, builderVersion) = metaInfoRetriever.adaptyUiAndBuilderVersion
        val languageCode = extractLanguageCode(locale)
        endPoint = "$inappsEndpointPrefix/$apiKeyPrefix/paywall-builder/$paywallId/$builderVersion/$languageCode/fallback.json"
        additionalHeaders = listOf(Request.Header("Content-type", "application/json"))
    }.build()

    @JvmSynthetic
    fun updateAttributionRequest(
        attributionData: AttributionData,
    ) = buildRequest {
        method = POST
        endPoint = "${getEndpointForProfileRequests(cacheRepository.getProfileId())}attribution/"
        body = gson.toJson(
            UpdateAttributionRequest.create(attributionData)
        )
    }

    @JvmSynthetic
    fun setVariationIdRequest(transactionId: String, variationId: String) =
        buildRequest {
            method = POST
            endPoint = "$inappsEndpointPrefix/transaction-variation-id/"
            body = gson.toJson(
                SetVariationIdRequest.create(
                    transactionId,
                    variationId,
                )
            )
        }

    @JvmSynthetic
    fun kinesisRequest(requestBody: Map<String, Any>) =
        Request.Builder(Request("https://kinesis.us-east-1.amazonaws.com/"))
            .apply {
                method = POST
                body = gson.toJson(requestBody).replace("\\u003d", "=")
            }
            .build()

    private inline fun buildRequest(action: Request.Builder.() -> Unit) =
        Request.Builder().apply(action).build()
}