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
            responseCacheKeys = this@Builder.responseCacheKeys
            currentDataWhenSent = this@Builder.currentDataWhenSent
        }
    }

    internal enum class Method {
        GET, POST, PATCH
    }

    internal class CurrentDataWhenSent(val profileId: String)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class RequestFactory(
    private val cacheRepository: CacheRepository,
    private val responseCacheKeyProvider: ResponseCacheKeyProvider,
    private val gson: Gson
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
        endPoint = "$inappsEndpointPrefix/products-ids/"
        responseCacheKeys = responseCacheKeyProvider.forGetProductIds()
    }

    @JvmSynthetic
    fun getPaywallRequest(id: String, locale: String?) = buildRequest {
        method = GET
        endPoint = "$inappsEndpointPrefix/purchase-containers/$id/"
        addQueryParam(Pair("profile_id", cacheRepository.getProfileId()))
        if (locale != null) {
            addQueryParam("locale" to locale)
        }
        responseCacheKeys = responseCacheKeyProvider.forGetPaywall(id)
    }

    @JvmSynthetic
    fun getViewConfigurationRequest(variationId: String) = buildRequest {
        method = GET
        endPoint = "$inappsEndpointPrefix/paywall-builder/$variationId/"
    }

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