package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.*
import com.adapty.internal.data.models.responses.*
import com.adapty.models.AdaptyProfileParameters
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CloudRepository(
    private val httpClient: HttpClient,
    private val requestFactory: RequestFactory
) {

    private val isActivateAllowed = MutableStateFlow(false)

    @JvmSynthetic
    fun getProfile(): Pair<ProfileResponseData.Attributes, Request.CurrentDataWhenSent?> {
        val request = requestFactory.getProfileRequest()
        val response =
            httpClient.newCall(
                request,
                ProfileResponse::class.java
            )
        when (response) {
            is Response.Success -> return response.body.data.attributes to request.currentDataWhenSent
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getProducts(): List<ProductDto> {
        val response = httpClient.newCall(
            requestFactory.getProductsRequest(),
            ProductsResponse::class.java
        )
        when (response) {
            is Response.Success -> return response.body.data.orEmpty()
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getProductIds(): List<String> {
        val response = httpClient.newCall(
            requestFactory.getProductIdsRequest(),
            ProductIdsResponse::class.java
        )
        when (response) {
            is Response.Success -> return response.body.data.orEmpty()
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getPaywall(id: String): PaywallDto {
        val response = httpClient.newCall(
            requestFactory.getPaywallRequest(id),
            PaywallResponse::class.java
        )
        when (response) {
            is Response.Success -> return response.body.data.attributes
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun createProfile(
        customerUserId: String?,
        installationMeta: InstallationMeta,
        params: AdaptyProfileParameters?,
    ): Flow<ProfileResponseData.Attributes> =
        flow {
            val response = httpClient.newCall(
                requestFactory.createProfileRequest(customerUserId, installationMeta, params),
                ProfileResponse::class.java
            )
            when (response) {
                is Response.Success -> emit(response.body.data.attributes)
                is Response.Error -> throw response.error
            }
        }

    @JvmSynthetic
    fun validatePurchase(
        purchaseType: String,
        purchase: Purchase,
        product: ValidateProductInfo,
    ): Pair<ProfileResponse, Request.CurrentDataWhenSent?> {
        val request = requestFactory.validatePurchaseRequest(purchaseType, purchase, product)
        val response = httpClient.newCall(
            request,
            ProfileResponse::class.java
        )
        when (response) {
            is Response.Success -> return response.body to request.currentDataWhenSent
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getAnalyticsCreds(): AnalyticsCredsResponse.Data? {
        val response = httpClient.newCall(
            requestFactory.getAnalyticsCreds(),
            AnalyticsCredsResponse::class.java
        )
        when (response) {
            is Response.Success -> return response.body.data
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun restorePurchases(purchases: List<RestoreProductInfo>): Pair<ProfileResponse, Request.CurrentDataWhenSent?> {
        val request = requestFactory.restorePurchasesRequest(purchases)
        val response = httpClient.newCall(
            request,
            ProfileResponse::class.java
        )
        when (response) {
            is Response.Success -> return response.body to request.currentDataWhenSent
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun updateProfile(
        params: AdaptyProfileParameters?,
        installationMeta: InstallationMeta?,
    ): Pair<ProfileResponseData.Attributes, Request.CurrentDataWhenSent?> {
        val request = requestFactory.updateProfileRequest(params, installationMeta)
        val response =
            httpClient.newCall(
                request,
                ProfileResponse::class.java
            )
        when (response) {
            is Response.Success -> return response.body.data.attributes to request.currentDataWhenSent
            is Response.Error -> throw response.error
        }
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
    fun setVariationId(transactionId: String, variationId: String) {
        val response = httpClient.newCall(
            requestFactory.setVariationIdRequest(transactionId, variationId),
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