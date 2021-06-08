package com.adapty.internal.data.cloud

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cache.ResponseCacheKeys
import com.adapty.internal.data.cloud.Request.Method.*
import com.adapty.internal.data.models.AttributionData
import com.adapty.internal.data.models.RestoreProductInfo
import com.adapty.internal.data.models.ValidateProductInfo
import com.adapty.internal.data.models.requests.*
import com.adapty.internal.utils.getCurrentLocale
import com.adapty.utils.ProfileParameterBuilder
import com.android.billingclient.api.Purchase
import com.google.gson.Gson
import java.util.*

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

    internal class Builder(private val baseRequest: Request = Request(baseUrl = "https://api.adapty.io/api/v1/")) {

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
        var responseCacheKeys: ResponseCacheKeys? = null

        private var queryParams = arrayListOf<Pair<String, String>>()

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
            body = this@Builder.body ?: ""
            responseCacheKeys = this@Builder.responseCacheKeys
        }
    }

    internal enum class Method {
        GET, POST, PATCH
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class RequestFactory(
    private val appContext: Context,
    private val cacheRepository: CacheRepository,
    private val gson: Gson
) {

    private val inappsEndpointPrefix = "sdk/in-apps"
    private val profilesEndpointPrefix = "sdk/analytics/profiles"

    private val endpointForProfileRequests: String
        get() = "$profilesEndpointPrefix/${cacheRepository.getProfileId()}/"

    @JvmSynthetic
    fun getPurchaserInfoRequest() = buildRequest {
        method = GET
        endPoint = endpointForProfileRequests
        responseCacheKeys = ResponseCacheKeys.forGetPurchaserInfo()
    }

    @JvmSynthetic
    fun updateProfileRequest(params: ProfileParameterBuilder) = buildRequest {
        method = PATCH
        body = gson.toJson(
            UpdateProfileRequest.create(cacheRepository.getOrCreateProfileUUID(), params)
        )
        endPoint = endpointForProfileRequests
    }

    @JvmSynthetic
    fun createProfileRequest(customerUserId: String?) = buildRequest {
        method = POST
        body = gson.toJson(
            CreateProfileRequest.create(cacheRepository.getOrCreateProfileUUID(), customerUserId)
        )
        endPoint = endpointForProfileRequests
    }

    @JvmSynthetic
    fun syncMetaInstallRequest(
        pushToken: String?,
        adId: String?
    ) = buildRequest {
        val appBuild: String
        val appVersion: String
        appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            .let { packageInfo ->
                appBuild = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    "${packageInfo.longVersionCode}"
                } else {
                    "${packageInfo.versionCode}"
                }
                appVersion = packageInfo.versionName
            }

        method = POST
        body = gson.toJson(
            SyncMetaRequest.create(
                id = cacheRepository.getOrCreateMetaUUID(),
                adaptySdkVersion = com.adapty.BuildConfig.VERSION_NAME,
                adaptySdkVersionBuild = com.adapty.BuildConfig.VERSION_CODE,
                advertisingId = adId,
                appBuild = appBuild,
                appVersion = appVersion,
                device = cacheRepository.deviceName,
                deviceToken = pushToken,
                locale = getCurrentLocale(appContext)?.let { "${it.language}_${it.country}" },
                os = Build.VERSION.RELEASE,
                platform = "Android",
                timezone = TimeZone.getDefault().id
            )
        )
        endPoint =
            "${endpointForProfileRequests}installation-metas/${cacheRepository.getInstallationMetaId()}/"
    }

    @JvmSynthetic
    fun validatePurchaseRequest(
        purchaseType: String,
        purchase: Purchase,
        product: ValidateProductInfo?
    ) = cacheRepository.getOrCreateProfileUUID().let { uuid ->
        buildRequest {
            method = POST
            endPoint = "$inappsEndpointPrefix/google/token/validate/"
            body = gson.toJson(
                ValidateReceiptRequest.create(uuid, purchase, product, purchaseType)
            )
        }
    }

    @JvmSynthetic
    fun restorePurchasesRequest(purchases: List<RestoreProductInfo>) = buildRequest {
        method = POST
        body = gson.toJson(
            RestoreReceiptRequest.create(cacheRepository.getOrCreateProfileUUID(), purchases)
        )
        endPoint = "$inappsEndpointPrefix/google/token/restore/"
    }

    @JvmSynthetic
    fun getPaywallsRequest() = buildRequest {
        method = GET
        endPoint = "$inappsEndpointPrefix/purchase-containers/"
        addQueryParam(Pair("profile_id", cacheRepository.getProfileId().orEmpty()))
        addQueryParam(Pair("automatic_paywalls_screen_reporting_enabled", "false"))
        responseCacheKeys = ResponseCacheKeys.forGetPaywalls()
    }

    @JvmSynthetic
    fun updateAttributionRequest(
        attributionData: AttributionData,
    ) = buildRequest {
        method = POST
        endPoint = "${endpointForProfileRequests}attribution/"
        body = gson.toJson(
            UpdateAttributionRequest.create(attributionData)
        )
    }

    @JvmSynthetic
    fun getPromoRequest() = buildRequest {
        method = GET
        endPoint = "${endpointForProfileRequests}promo/"
        responseCacheKeys = ResponseCacheKeys.forGetPromo()
    }

    @JvmSynthetic
    fun setTransactionVariationIdRequest(transactionId: String, variationId: String) =
        buildRequest {
            method = POST
            endPoint = "$inappsEndpointPrefix/transaction-variation-id/"
            body = gson.toJson(
                TransactionVariationIdRequest.create(
                    transactionId,
                    variationId,
                    cacheRepository.getOrCreateProfileUUID()
                )
            )
        }

    @JvmSynthetic
    fun setExternalAnalyticsEnabledRequest(enabled: Boolean) = buildRequest {
        method = POST
        endPoint = "${endpointForProfileRequests}analytics-enabled/"
        body = gson.toJson(
            ExternalAnalyticsEnabledRequest.create(enabled)
        )
    }

    @JvmSynthetic
    fun kinesisRequest(requestBody: HashMap<String, Any>) =
        Request.Builder(Request("https://kinesis.us-east-1.amazonaws.com/"))
            .apply {
                method = POST
                body = gson.toJson(requestBody).replace("\\u003d", "=")
            }
            .build()

    private inline fun buildRequest(action: Request.Builder.() -> Unit) =
        Request.Builder().apply(action).build()
}