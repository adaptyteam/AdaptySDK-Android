package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.*
import com.adapty.models.AdaptyPaywallProduct.Type
import com.adapty.models.AdaptyProfileParameters
import com.android.billingclient.api.Purchase
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CloudRepository(
    private val httpClient: HttpClient,
    private val requestFactory: RequestFactory
) {

    private val isActivateAllowed = MutableStateFlow(false)

    @JvmSynthetic
    fun getProfile(): Pair<ProfileDto, Request.CurrentDataWhenSent?> {
        val request = requestFactory.getProfileRequest()
        val response =
            httpClient.newCall<ProfileDto>(
                request,
                ProfileDto::class.java
            )
        when (response) {
            is Response.Success -> return response.body to request.currentDataWhenSent
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getProducts(): List<ProductDto> {
        val response = httpClient.newCall<ArrayList<ProductDto>>(
            requestFactory.getProductsRequest(),
            object : TypeToken<ArrayList<ProductDto>>() {}.type,
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getProductIds(): List<String> {
        val response = httpClient.newCall<ArrayList<String>>(
            requestFactory.getProductIdsRequest(),
            object : TypeToken<ArrayList<String>>() {}.type,
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getPaywall(id: String, locale: String?): PaywallDto {
        val response = httpClient.newCall<PaywallDto>(
            requestFactory.getPaywallRequest(id, locale),
            PaywallDto::class.java
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getViewConfiguration(variationId: String): ViewConfigurationDto {
        val response = httpClient.newCall<ViewConfigurationDto>(
            requestFactory.getViewConfigurationRequest(variationId),
            ViewConfigurationDto::class.java
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun createProfile(
        customerUserId: String?,
        installationMeta: InstallationMeta,
        params: AdaptyProfileParameters?,
    ): Flow<ProfileDto> =
        flow {
            val response = httpClient.newCall<ProfileDto>(
                requestFactory.createProfileRequest(customerUserId, installationMeta, params),
                ProfileDto::class.java
            )
            when (response) {
                is Response.Success -> emit(response.body)
                is Response.Error -> throw response.error
            }
        }

    @JvmSynthetic
    fun validatePurchase(
        purchaseType: Type,
        purchase: Purchase,
        product: ValidateProductInfo,
    ): Pair<ProfileDto, Request.CurrentDataWhenSent?> {
        val request = requestFactory.validatePurchaseRequest(purchaseType, purchase, product)
        val response = httpClient.newCall<ProfileDto>(
            request,
            ProfileDto::class.java
        )
        when (response) {
            is Response.Success -> return response.body to request.currentDataWhenSent
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getAnalyticsCreds(): AnalyticsCreds {
        val response = httpClient.newCall<AnalyticsCreds>(
            requestFactory.getAnalyticsCreds(),
            AnalyticsCreds::class.java
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun restorePurchases(purchases: List<RestoreProductInfo>): Pair<ProfileDto, Request.CurrentDataWhenSent?> {
        val request = requestFactory.restorePurchasesRequest(purchases)
        val response = httpClient.newCall<ProfileDto>(
            request,
            ProfileDto::class.java
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
    ): Pair<ProfileDto, Request.CurrentDataWhenSent?> {
        val request = requestFactory.updateProfileRequest(params, installationMeta)
        val response =
            httpClient.newCall<ProfileDto>(
                request,
                ProfileDto::class.java
            )
        when (response) {
            is Response.Success -> return response.body to request.currentDataWhenSent
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun updateAttribution(attributionData: AttributionData) {
        val response = httpClient.newCall<Any>(
            requestFactory.updateAttributionRequest(attributionData),
            Any::class.java
        )
        processEmptyResponse(response)
    }

    @JvmSynthetic
    fun setVariationId(transactionId: String, variationId: String) {
        val response = httpClient.newCall<Any>(
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