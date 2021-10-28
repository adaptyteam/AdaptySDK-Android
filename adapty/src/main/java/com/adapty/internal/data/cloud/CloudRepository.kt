package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.*
import com.adapty.internal.data.models.responses.*
import com.adapty.utils.ProfileParameterBuilder
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CloudRepository(
    private val httpClient: HttpClient,
    private val requestFactory: RequestFactory
) {

    private val isActivateAllowed = MutableStateFlow(false)

    @JvmSynthetic
    fun getPurchaserInfo(): ProfileResponseData.Attributes? {
        val response =
            httpClient.newCall(
                requestFactory.getPurchaserInfoRequest(),
                PurchaserInfoResponse::class.java
            )
        when (response) {
            is Response.Success -> return response.body.data?.attributes
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getPaywalls(): Pair<ArrayList<PaywallsResponse.Data>, ArrayList<ProductDto>> {
        val response = httpClient.newCall(
            requestFactory.getPaywallsRequest(),
            PaywallsResponse::class.java
        )
        when (response) {
            is Response.Success -> return Pair(
                response.body.data ?: arrayListOf(),
                response.body.meta?.products ?: arrayListOf()
            )
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getPromo(): PromoDto? {
        val response =
            httpClient.newCall(requestFactory.getPromoRequest(), PromoResponse::class.java)
        when (response) {
            is Response.Success -> return response.body.data?.attributes
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun createProfile(
        customerUserId: String?,
    ): Flow<ProfileResponseData.Attributes?> =
        flow {
            val response = httpClient.newCall(
                requestFactory.createProfileRequest(customerUserId),
                CreateProfileResponse::class.java
            )
            when (response) {
                is Response.Success -> emit(response.body.data?.attributes)
                is Response.Error -> throw response.error
            }
        }

    @JvmSynthetic
    fun validatePurchase(
        purchaseType: String,
        purchase: Purchase,
        product: ValidateProductInfo?,
    ): ValidateReceiptResponse {
        val response = httpClient.newCall(
            requestFactory.validatePurchaseRequest(
                purchaseType,
                purchase,
                product
            ),
            ValidateReceiptResponse::class.java
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun syncMeta(
        advertisingId: String?,
        pushToken: String?
    ): SyncMetaResponse.Data.Attributes? {
        val response = httpClient.newCall(
            requestFactory.syncMetaInstallRequest(pushToken, advertisingId),
            SyncMetaResponse::class.java
        )
        when (response) {
            is Response.Success -> return response.body.data?.attributes
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun restorePurchases(purchases: List<RestoreProductInfo>): RestoreReceiptResponse {
        val response = httpClient.newCall(
            requestFactory.restorePurchasesRequest(purchases),
            RestoreReceiptResponse::class.java
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun updateProfile(params: ProfileParameterBuilder) {
        val response =
            httpClient.newCall(
                requestFactory.updateProfileRequest(params),
                Any::class.java
            )
        processEmptyResponse(response)
    }

    @JvmSynthetic
    fun updateAttribution(attributionData: AttributionData) {
        val response = httpClient.newCall(
            requestFactory.updateAttributionRequest(attributionData),
            Any::class.java
        )
        processEmptyResponse(response)
    }

    @JvmSynthetic
    fun setTransactionVariationId(transactionId: String, variationId: String) {
        val response = httpClient.newCall(
            requestFactory.setTransactionVariationIdRequest(transactionId, variationId),
            Any::class.java
        )
        processEmptyResponse(response)
    }

    @JvmSynthetic
    fun setExternalAnalyticsEnabled(enabled: Boolean) {
        val response = httpClient.newCall(
            requestFactory.setExternalAnalyticsEnabledRequest(enabled),
            Any::class.java
        )
        processEmptyResponse(response)
    }

    private fun processEmptyResponse(response: Response<*>) {
        when (response) {
            is Response.Success -> return
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun allowActivate() {
        isActivateAllowed.value = true
    }

    @JvmSynthetic
    fun onActivateAllowed() =
        isActivateAllowed
            .filter { it }
            .take(1)
}