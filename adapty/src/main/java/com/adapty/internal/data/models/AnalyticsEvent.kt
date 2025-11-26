package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.internal.data.models.requests.ValidateReceiptRequest
import com.adapty.internal.domain.models.PurchaseableProduct
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPlacementFetchPolicy
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.utils.TransactionInfo
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import java.util.Locale
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AnalyticsEvent(
    val eventId: String,
    val eventName: String,
    val profileId: String,
    val sessionId: String,
    val deviceId: String,
    val createdAt: String,
    val platform: String,
    val other: Map<String, Any>,
) {
    var ordinal: Long = 0L

    val isSystemLog get() = eventName == SYSTEM_LOG

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnalyticsEvent

        if (eventId != other.eventId) return false
        if (eventName != other.eventName) return false
        if (profileId != other.profileId) return false
        if (sessionId != other.sessionId) return false
        if (deviceId != other.deviceId) return false
        if (createdAt != other.createdAt) return false
        if (platform != other.platform) return false
        if (other != other.other) return false
        return ordinal == other.ordinal
    }

    override fun hashCode(): Int {
        var result = eventId.hashCode()
        result = 31 * result + eventName.hashCode()
        result = 31 * result + profileId.hashCode()
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + deviceId.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + platform.hashCode()
        result = 31 * result + other.hashCode()
        result = 31 * result + ordinal.hashCode()
        return result
    }

    sealed class CustomData(
        val eventName: String,
        flowId: String?,
    ) {

        var sdkFlowId: String? = flowId
            private set

        companion object {
            const val SDK_REQUEST_PREFIX = "sdk_request_"
            const val SDK_RESPONSE_PREFIX = "sdk_response_"
            const val API_REQUEST_PREFIX = "api_request_"
            const val API_RESPONSE_PREFIX = "api_response_"
            const val GOOGLE_REQUEST_PREFIX = "google_request_"
            const val GOOGLE_RESPONSE_PREFIX = "google_response_"
        }

        fun resetFlowId() {
            sdkFlowId = UUID.randomUUID().toString()
        }
    }

    sealed class SDKMethodRequestData private constructor(methodName: String) :
        CustomData("${SDK_REQUEST_PREFIX}${methodName}", UUID.randomUUID().toString()) {
        companion object {
            fun create(methodName: String) = Basic.create(methodName)
        }

        class Basic private constructor(methodName: String) : SDKMethodRequestData(methodName) {

            companion object {
                fun create(methodName: String) = Basic(methodName)
            }
        }

        class GetPaywall private constructor(
            val placementId: String,
            val locale: String?,
            val fetchPolicy: Map<String, Any>,
            val loadTimeout: Double,
            methodName: String
        ) : SDKMethodRequestData(methodName) {

            companion object {
                fun create(
                    placementId: String,
                    locale: String?,
                    fetchPolicy: AdaptyPlacementFetchPolicy,
                    loadTimeoutMillis: Int,
                ) =
                    GetPaywall(
                        placementId,
                        locale,
                        when (fetchPolicy) {
                            is AdaptyPlacementFetchPolicy.ReloadRevalidatingCacheData -> mapOf("type" to "reload_revalidating_cache_data")
                            is AdaptyPlacementFetchPolicy.ReturnCacheDataElseLoad -> mapOf("type" to "return_cache_data_else_load")
                            is AdaptyPlacementFetchPolicy.ReturnCacheDataIfNotExpiredElseLoad -> mapOf(
                                "type" to "return_cache_data_else_load",
                                "max_age" to fetchPolicy.maxAgeMillis / 1000.0,
                            )
                        },
                        loadTimeoutMillis / 1000.0,
                        "get_paywall",
                    )
            }
        }

        class GetPaywallProducts private constructor(
            val placementId: String,
            methodName: String
        ) : SDKMethodRequestData(methodName) {

            companion object {
                fun create(
                    placementId: String,
                ) =
                    GetPaywallProducts(
                        placementId,
                        "get_paywall_products",
                    )
            }
        }

        class GetOnboarding private constructor(
            val placementId: String,
            val locale: String?,
            val fetchPolicy: Map<String, Any>,
            val loadTimeout: Double,
            methodName: String
        ) : SDKMethodRequestData(methodName) {

            companion object {
                fun create(
                    placementId: String,
                    locale: String?,
                    fetchPolicy: AdaptyPlacementFetchPolicy,
                    loadTimeoutMillis: Int,
                ) =
                    GetOnboarding(
                        placementId,
                        locale,
                        when (fetchPolicy) {
                            is AdaptyPlacementFetchPolicy.ReloadRevalidatingCacheData -> mapOf("type" to "reload_revalidating_cache_data")
                            is AdaptyPlacementFetchPolicy.ReturnCacheDataElseLoad -> mapOf("type" to "return_cache_data_else_load")
                            is AdaptyPlacementFetchPolicy.ReturnCacheDataIfNotExpiredElseLoad -> mapOf(
                                "type" to "return_cache_data_else_load",
                                "max_age" to fetchPolicy.maxAgeMillis / 1000.0,
                            )
                        },
                        loadTimeoutMillis / 1000.0,
                        "get_onboarding",
                    )
            }
        }

        class MakePurchase private constructor(
            val paywallName: String,
            val variationId: String,
            val productId: String,
            methodName: String
        ) : SDKMethodRequestData(methodName) {

            companion object {
                fun create(
                    product: AdaptyPaywallProduct,
                ) =
                    MakePurchase(
                        product.paywallName,
                        product.variationId,
                        product.vendorProductId,
                        "make_purchase",
                    )
            }
        }

        class UpdateAttribution private constructor(
            val source: String,
            methodName: String
        ) : SDKMethodRequestData(methodName) {

            companion object {
                fun create(source: String) =
                    UpdateAttribution(
                        source,
                        "update_attribution",
                    )
            }
        }

        class SetIntegrationId private constructor(
            val data: Map<String, String>,
            methodName: String
        ) : SDKMethodRequestData(methodName) {

            companion object {
                fun create(key: String, value: String) =
                    SetIntegrationId(
                        mapOf(key to value),
                        "set_integration_identifier",
                    )
            }
        }

        class Activate private constructor(
            val observerMode: Boolean,
            val ipAddressCollectionDisabled: Boolean,
            val hasCustomerUserId: Boolean,
            methodName: String
        ) : SDKMethodRequestData(methodName) {

            companion object {
                fun create(
                    observerMode: Boolean,
                    ipAddressCollectionDisabled: Boolean,
                    hasCustomerUserId: Boolean,
                ) =
                    Activate(
                        observerMode,
                        ipAddressCollectionDisabled,
                        hasCustomerUserId,
                        "activate",
                    )
            }
        }

        class ReportTransaction private constructor(
            val transactionId: String,
            val variationId: String?,
            methodName: String
        ) : SDKMethodRequestData(methodName) {

            companion object {
                fun create(
                    transactionInfo: TransactionInfo,
                    variationId: String?,
                ) =
                    ReportTransaction(
                        when (transactionInfo) {
                            is TransactionInfo.Id -> transactionInfo.transactionId
                            is TransactionInfo.Purchase -> transactionInfo.purchase.orderId.orEmpty()
                        },
                        variationId,
                        "report_transaction",
                    )
            }
        }

        class GetUntargetedPaywall private constructor(
            val placementId: String,
            val locale: String?,
            val fetchPolicy: Map<String, Any>,
            methodName: String
        ) : SDKMethodRequestData(methodName) {

            companion object {
                fun create(
                    placementId: String,
                    locale: String?,
                    fetchPolicy: AdaptyPlacementFetchPolicy,
                ) =
                    GetUntargetedPaywall(
                        placementId,
                        locale,
                        when (fetchPolicy) {
                            is AdaptyPlacementFetchPolicy.ReloadRevalidatingCacheData -> mapOf("type" to "reload_revalidating_cache_data")
                            is AdaptyPlacementFetchPolicy.ReturnCacheDataElseLoad -> mapOf("type" to "return_cache_data_else_load")
                            is AdaptyPlacementFetchPolicy.ReturnCacheDataIfNotExpiredElseLoad -> mapOf(
                                "type" to "return_cache_data_else_load",
                                "max_age" to fetchPolicy.maxAgeMillis / 1000.0,
                            )
                        },
                        "get_untargeted_paywall",
                    )
            }
        }

        class GetUntargetedOnboarding private constructor(
            val placementId: String,
            val locale: String?,
            val fetchPolicy: Map<String, Any>,
            methodName: String
        ) : SDKMethodRequestData(methodName) {

            companion object {
                fun create(
                    placementId: String,
                    locale: String?,
                    fetchPolicy: AdaptyPlacementFetchPolicy,
                ) =
                    GetUntargetedOnboarding(
                        placementId,
                        locale,
                        when (fetchPolicy) {
                            is AdaptyPlacementFetchPolicy.ReloadRevalidatingCacheData -> mapOf("type" to "reload_revalidating_cache_data")
                            is AdaptyPlacementFetchPolicy.ReturnCacheDataElseLoad -> mapOf("type" to "return_cache_data_else_load")
                            is AdaptyPlacementFetchPolicy.ReturnCacheDataIfNotExpiredElseLoad -> mapOf(
                                "type" to "return_cache_data_else_load",
                                "max_age" to fetchPolicy.maxAgeMillis / 1000.0,
                            )
                        },
                        "get_untargeted_onboarding",
                    )
            }
        }
    }

    class SDKMethodResponseData private constructor(
        eventName: String,
        flowId: String?,
        val success: Boolean,
        val error: String?,
    ) : CustomData(eventName, flowId) {

        companion object {
            fun create(paired: SDKMethodRequestData, error: AdaptyError? = null) =
                SDKMethodResponseData(
                    paired.eventName.replace(SDK_REQUEST_PREFIX, SDK_RESPONSE_PREFIX),
                    paired.sdkFlowId,
                    error == null,
                    error?.message ?: error?.localizedMessage,
                )
        }
    }

    sealed class BackendAPIRequestData private constructor(methodName: String) :
        CustomData("${API_REQUEST_PREFIX}${methodName}", null) {

        class GetNetConfig private constructor(methodName: String) : BackendAPIRequestData(methodName) {

            companion object {
                fun create() =
                    GetNetConfig("get_net_config")
            }
        }

        class Validate private constructor(
            val productId: String,
            val basePlanId: String?,
            val offerId: String?,
            val transactionId: String?,
            val variationId: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    validateData: ValidateReceiptRequest,
                    purchase: Purchase?,
                ) =
                    Validate(
                        validateData.vendorProductId,
                        validateData.basePlanId,
                        validateData.offerId,
                        purchase?.orderId,
                        validateData.variationId,
                        "validate_transaction",
                    )
            }
        }

        class Restore private constructor(
            val productIds: List<String>,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    purchases: List<RestoreProductInfo>,
                ) =
                    Restore(
                        purchases.mapNotNull { it.productId },
                        "restore_purchases",
                    )
            }
        }

        class CreateProfile private constructor(
            val hasCustomerUserId: Boolean,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(hasCustomerUserId: Boolean) =
                    CreateProfile(
                        hasCustomerUserId,
                        "create_profile",
                    )
            }
        }

        class UpdateProfile private constructor(
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create() =
                    UpdateProfile(
                        "update_profile",
                    )
            }
        }

        class GetProfile private constructor(
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create() =
                    GetProfile(
                        "get_profile",
                    )
            }
        }

        class GetFallbackPaywall private constructor(
            val apiPrefix: String,
            val placementId: String,
            val languageCode: String,
            val variationId: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    apiPrefix: String,
                    placementId: String,
                    languageCode: String,
                    variationId: String,
                ) =
                    GetFallbackPaywall(
                        apiPrefix,
                        placementId,
                        languageCode,
                        variationId,
                        "get_fallback_paywall",
                    )
            }
        }

        class GetFallbackPaywallVariations private constructor(
            val apiPrefix: String,
            val placementId: String,
            val languageCode: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    apiPrefix: String,
                    placementId: String,
                    languageCode: String,
                ) =
                    GetFallbackPaywallVariations(
                        apiPrefix,
                        placementId,
                        languageCode,
                        "get_fallback_paywall_variations",
                    )
            }
        }

        class GetFallbackPaywallBuilder private constructor(
            val apiPrefix: String,
            val paywallInstanceId: String,
            val builderVersion: String,
            val languageCode: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    apiPrefix: String,
                    paywallInstanceId: String,
                    builderVersion: String,
                    languageCode: String,
                ) =
                    GetFallbackPaywallBuilder(
                        apiPrefix,
                        paywallInstanceId,
                        builderVersion,
                        languageCode,
                        "get_fallback_paywall_builder",
                    )
            }
        }

        class GetFallbackOnboarding private constructor(
            val apiPrefix: String,
            val placementId: String,
            val languageCode: String,
            val variationId: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    apiPrefix: String,
                    placementId: String,
                    languageCode: String,
                    variationId: String,
                ) =
                    GetFallbackOnboarding(
                        apiPrefix,
                        placementId,
                        languageCode,
                        variationId,
                        "get_fallback_onboarding",
                    )
            }
        }

        class GetFallbackOnboardingVariations private constructor(
            val apiPrefix: String,
            val placementId: String,
            val languageCode: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    apiPrefix: String,
                    placementId: String,
                    languageCode: String,
                ) =
                    GetFallbackOnboardingVariations(
                        apiPrefix,
                        placementId,
                        languageCode,
                        "get_fallback_onboarding_variations",
                    )
            }
        }

        class GetUntargetedPaywallVariations private constructor(
            val apiPrefix: String,
            val placementId: String,
            val languageCode: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    apiPrefix: String,
                    placementId: String,
                    languageCode: String,
                ) =
                    GetUntargetedPaywallVariations(
                        apiPrefix,
                        placementId,
                        languageCode,
                        "get_untargeted_paywall_variations",
                    )
            }
        }

        class GetPaywall private constructor(
            val apiPrefix: String,
            val placementId: String,
            val locale: String,
            val variationId: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    apiPrefix: String,
                    placementId: String,
                    locale: String,
                    variationId: String,
                ) =
                    GetPaywall(
                        apiPrefix,
                        placementId,
                        locale,
                        variationId,
                        "get_paywall",
                    )
            }
        }

        class GetPaywallVariations private constructor(
            val apiPrefix: String,
            val placementId: String,
            val locale: String,
            val segmentId: String,
            val md5: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    apiPrefix: String,
                    placementId: String,
                    locale: String,
                    segmentId: String,
                    md5: String,
                ) =
                    GetPaywallVariations(
                        apiPrefix,
                        placementId,
                        locale,
                        segmentId,
                        md5,
                        "get_paywall_variations",
                    )
            }
        }

        class GetPaywallBuilder private constructor(
            val variationId: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(variationId: String) =
                    GetPaywallBuilder(
                        variationId,
                        "get_paywall_builder",
                    )
            }
        }

        class GetUntargetedOnboardingVariations private constructor(
            val apiPrefix: String,
            val placementId: String,
            val languageCode: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    apiPrefix: String,
                    placementId: String,
                    languageCode: String,
                ) =
                    GetUntargetedOnboardingVariations(
                        apiPrefix,
                        placementId,
                        languageCode,
                        "get_untargeted_onboarding_variations",
                    )
            }
        }

        class GetOnboarding private constructor(
            val apiPrefix: String,
            val placementId: String,
            val locale: String,
            val variationId: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    apiPrefix: String,
                    placementId: String,
                    locale: String,
                    variationId: String,
                ) =
                    GetOnboarding(
                        apiPrefix,
                        placementId,
                        locale,
                        variationId,
                        "get_onboarding",
                    )
            }
        }

        class GetOnboardingVariations private constructor(
            val apiPrefix: String,
            val placementId: String,
            val locale: String,
            val segmentId: String,
            val md5: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    apiPrefix: String,
                    placementId: String,
                    locale: String,
                    segmentId: String,
                    md5: String,
                ) =
                    GetOnboardingVariations(
                        apiPrefix,
                        placementId,
                        locale,
                        segmentId,
                        md5,
                        "get_onboarding_variations",
                    )
            }
        }

        class GetCrossPlacementInfo private constructor(
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create() =
                    GetCrossPlacementInfo(
                        "get_cross_placement_state",
                    )
            }
        }

        class SetVariationId private constructor(
            val transaction: String,
            val variationId: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(transactionId: String, variationId: String) =
                    SetVariationId(
                        transactionId,
                        variationId,
                        "set_variation_id",
                    )
            }
        }

        class RegisterInstall private constructor(
            val retryAttempt: Long?,
            val maxRetries: Long?,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(
                    retryAttempt: Long,
                    maxRetries: Long,
                ) =
                    RegisterInstall(
                        retryAttempt.takeIf { it > 0 },
                        maxRetries.takeIf { retryAttempt > 0 },
                        "register_install",
                    )
            }
        }

        class ReportTransaction private constructor(
            val transaction: String,
            val variationId: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(transactionId: String, variationId: String) =
                    ReportTransaction(
                        transactionId,
                        variationId,
                        "report_transaction",
                    )
            }
        }

        class SetAttribution private constructor(
            val source: String,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(attributionData: AttributionData) =
                    SetAttribution(
                        attributionData.source,
                        "set_attribution",
                    )
            }
        }

        class SetIntegrationId private constructor(
            val data: Map<String, String>,
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create(key: String, value: String) =
                    SetIntegrationId(
                        mapOf(key to value),
                        "set_integration_identifier",
                    )
            }
        }

        class GetProducts private constructor(
            methodName: String,
        ) : BackendAPIRequestData(methodName) {

            companion object {
                fun create() =
                    GetProducts(
                        "get_products",
                    )
            }
        }
    }

    class BackendAPIResponseData private constructor(
        val apiRequestId: String,
        eventName: String,
        flowId: String?,
        val headers: Map<String, String>?,
        val success: Boolean,
        val error: String?,
    ) : CustomData(eventName, flowId) {

        companion object {
            fun create(
                apiRequestId: String,
                headers: Map<String?, List<String>?>?,
                paired: BackendAPIRequestData,
            ) =
                create(apiRequestId, headers, paired, null)

            fun create(
                apiRequestId: String,
                paired: BackendAPIRequestData,
                error: Throwable,
            ) =
                create(apiRequestId, null, paired, error)

            private fun create(
                apiRequestId: String,
                headers: Map<String?, List<String>?>?,
                paired: BackendAPIRequestData,
                error: Throwable? = null,
            ) =
                BackendAPIResponseData(
                    apiRequestId,
                    paired.eventName.replace(API_REQUEST_PREFIX, API_RESPONSE_PREFIX),
                    paired.sdkFlowId,
                    mutableMapOf<String, String>().apply {
                        headers?.forEach { (k, v) ->
                            val key = k?.takeIf { it.endsWith("Cache-Status", true) }
                                ?: return@forEach
                            val value = v?.firstOrNull()
                                ?: return@forEach
                            put(key, value)
                        }
                    }.takeIf { it.isNotEmpty() },
                    error == null,
                    error?.message ?: error?.localizedMessage,
                )
        }
    }

    sealed class GoogleAPIRequestData private constructor(methodName: String) :
        CustomData("${GOOGLE_REQUEST_PREFIX}${methodName}", UUID.randomUUID().toString()) {

        class QueryProductDetails private constructor(
            val productIds: List<String>,
            val googleProductType: String,
            methodName: String,
        ) : GoogleAPIRequestData(methodName) {

            companion object {
                fun create(
                    productIds: List<String>,
                    googleProductType: String,
                ) =
                    QueryProductDetails(
                        productIds,
                        googleProductType,
                        "query_product_details",
                    )
            }
        }

        class QueryActivePurchases private constructor(
            val googleProductType: String,
            methodName: String,
        ) : GoogleAPIRequestData(methodName) {

            companion object {
                fun create(
                    googleProductType: String,
                ) =
                    QueryActivePurchases(
                        googleProductType,
                        "query_active_purchases",
                    )
            }
        }

        class QueryPurchaseHistory private constructor(
            val googleProductType: String,
            methodName: String,
        ) : GoogleAPIRequestData(methodName) {

            companion object {
                fun create(
                    googleProductType: String,
                ) =
                    QueryPurchaseHistory(
                        googleProductType,
                        "query_purchase_history",
                    )
            }
        }

        class MakePurchase private constructor(
            val productId: String,
            val productType: String,
            val basePlanId: String?,
            val offerId: String?,
            val oldSubVendorProductId: String?,
            val replacementMode: String?,
            methodName: String,
        ) : GoogleAPIRequestData(methodName) {

            companion object {
                fun create(
                    purchaseableProduct: PurchaseableProduct,
                    subscriptionUpdateParams: AdaptySubscriptionUpdateParameters?,
                ) =
                    MakePurchase(
                        purchaseableProduct.vendorProductId,
                        purchaseableProduct.type,
                        purchaseableProduct.currentOfferDetails?.basePlanId,
                        purchaseableProduct.currentOfferDetails?.offerId,
                        subscriptionUpdateParams?.oldSubVendorProductId,
                        subscriptionUpdateParams?.replacementMode?.name,
                        "make_purchase",
                    )
            }
        }

        class AcknowledgePurchase private constructor(
            val productId: String,
            methodName: String,
        ) : GoogleAPIRequestData(methodName) {

            companion object {
                fun create(
                    purchase: Purchase,
                ) =
                    AcknowledgePurchase(
                        purchase.products.firstOrNull().orEmpty(),
                        "acknowledge_purchase",
                    )
            }
        }

        class ConsumePurchase private constructor(
            val productId: String,
            methodName: String,
        ) : GoogleAPIRequestData(methodName) {

            companion object {
                fun create(
                    purchase: Purchase,
                ) =
                    ConsumePurchase(
                        purchase.products.firstOrNull().orEmpty(),
                        "consume_purchase",
                    )
            }
        }
    }

    sealed class GoogleAPIResponseData private constructor(
        eventName: String,
        flowId: String?,
        val success: Boolean,
        val error: String?,
    ) : CustomData(eventName, flowId) {

        companion object {
            fun create(
                paired: GoogleAPIRequestData,
                error: AdaptyError? = null,
            ) = Basic.create(paired, error)
        }

        class Basic private constructor(
            eventName: String,
            flowId: String?,
            success: Boolean,
            error: String?,
        ) : GoogleAPIResponseData(eventName, flowId, success, error) {

            companion object {
                fun create(
                    paired: GoogleAPIRequestData,
                    error: AdaptyError?,
                ) = Basic(
                    paired.eventName.replace(GOOGLE_REQUEST_PREFIX, GOOGLE_RESPONSE_PREFIX),
                    paired.sdkFlowId,
                    error == null,
                    error?.message,
                )
            }
        }

        class QueryProductDetails private constructor(
            val productIds: List<String>?,
            eventName: String,
            flowId: String?,
            success: Boolean,
            error: String?,
        ) : GoogleAPIResponseData(eventName, flowId, success, error) {

            companion object {
                fun create(
                    error: AdaptyError,
                    paired: GoogleAPIRequestData.QueryProductDetails,
                ) =
                    QueryProductDetails(
                        null,
                        paired.eventName.replace(GOOGLE_REQUEST_PREFIX, GOOGLE_RESPONSE_PREFIX),
                        paired.sdkFlowId,
                        false,
                        error.message,
                    )

                fun create(
                    productList: List<ProductDetails>?,
                    paired: GoogleAPIRequestData.QueryProductDetails,
                ) =
                    QueryProductDetails(
                        productList.orEmpty().map { it.productId },
                        paired.eventName.replace(GOOGLE_REQUEST_PREFIX, GOOGLE_RESPONSE_PREFIX),
                        paired.sdkFlowId,
                        true,
                        null,
                    )
            }
        }

        class QueryActivePurchases private constructor(
            val productIds: List<String>?,
            eventName: String,
            flowId: String?,
            success: Boolean,
            error: String?,
        ) : GoogleAPIResponseData(eventName, flowId, success, error) {

            companion object {
                fun create(
                    error: AdaptyError,
                    paired: GoogleAPIRequestData.QueryActivePurchases,
                ) =
                    QueryActivePurchases(
                        null,
                        paired.eventName.replace(GOOGLE_REQUEST_PREFIX, GOOGLE_RESPONSE_PREFIX),
                        paired.sdkFlowId,
                        false,
                        error.message,
                    )

                fun create(
                    purchaseList: List<Purchase>?,
                    paired: GoogleAPIRequestData.QueryActivePurchases,
                ) =
                    QueryActivePurchases(
                        purchaseList.orEmpty().map { it.products.firstOrNull().orEmpty() },
                        paired.eventName.replace(GOOGLE_REQUEST_PREFIX, GOOGLE_RESPONSE_PREFIX),
                        paired.sdkFlowId,
                        true,
                        null,
                    )
            }
        }

        class QueryPurchaseHistory private constructor(
            val productIds: List<String>?,
            eventName: String,
            flowId: String?,
            success: Boolean,
            error: String?,
        ) : GoogleAPIResponseData(eventName, flowId, success, error) {

            companion object {
                fun create(
                    error: AdaptyError,
                    paired: GoogleAPIRequestData.QueryPurchaseHistory,
                ) =
                    QueryPurchaseHistory(
                        null,
                        paired.eventName.replace(GOOGLE_REQUEST_PREFIX, GOOGLE_RESPONSE_PREFIX),
                        paired.sdkFlowId,
                        false,
                        error.message,
                    )

                fun create(
                    purchaseList: List<PurchaseHistoryRecord>?,
                    paired: GoogleAPIRequestData.QueryPurchaseHistory,
                ) =
                    QueryPurchaseHistory(
                        purchaseList.orEmpty().map { it.products.firstOrNull().orEmpty() },
                        paired.eventName.replace(GOOGLE_REQUEST_PREFIX, GOOGLE_RESPONSE_PREFIX),
                        paired.sdkFlowId,
                        true,
                        null,
                    )
            }
        }

        class MakePurchase private constructor(
            val state: String,
            val productId: String?,
            eventName: String,
            flowId: String?,
            success: Boolean,
            error: String?,
        ) : GoogleAPIResponseData(eventName, flowId, success, error) {

            companion object {
                fun create(
                    paired: GoogleAPIRequestData.MakePurchase,
                    purchaseResult: PurchaseResult,
                ): MakePurchase {
                    val state: String
                    val success: Boolean
                    val errorStr: String?
                    val purchaseProductId: String?

                    when (purchaseResult) {
                        is PurchaseResult.Canceled -> {
                            state = "canceled"
                            success = true
                            errorStr = null
                            purchaseProductId = null
                        }
                        is PurchaseResult.Success -> {
                            state = purchaseResult.state.name.lowercase(Locale.ENGLISH)
                            success = true
                            errorStr = null
                            purchaseProductId = purchaseResult.productId
                        }
                        is PurchaseResult.Error -> {
                            state = "failed"
                            success = false
                            val error = purchaseResult.error
                            errorStr = error.message.takeIf { !it.isNullOrEmpty() }
                                ?: error.originalError?.localizedMessage
                            purchaseProductId = null
                        }
                    }
                    return MakePurchase(
                        state,
                        purchaseProductId,
                        paired.eventName.replace(GOOGLE_REQUEST_PREFIX, GOOGLE_RESPONSE_PREFIX),
                        paired.sdkFlowId,
                        success,
                        errorStr,
                    )
                }
            }
        }
    }

    companion object {
        const val SYSTEM_LOG = "system_log"
        const val CUSTOM_DATA = "custom_data"
        const val RETAIN_LIMIT = 500
        const val SEND_LIMIT = 500
    }
}