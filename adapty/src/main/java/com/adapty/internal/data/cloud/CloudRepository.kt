package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.*
import com.adapty.internal.domain.models.PurchaseableProduct
import com.adapty.internal.utils.DEFAULT_PAYWALL_LOCALE
import com.adapty.models.AdaptyProfileParameters
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CloudRepository(
    private val httpClient: HttpClient,
    private val requestFactory: RequestFactory
) {

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

    fun getPaywallVariations(id: String, locale: String, segmentId: String): Pair<Variations, Request.CurrentDataWhenSent?> {
        val request = requestFactory.getPaywallVariationsRequest(id, locale, segmentId)
        val response = httpClient.newCall<Variations>(
            request,
            Variations::class.java,
        )
        when (response) {
            is Response.Success -> return response.body to request.currentDataWhenSent
            is Response.Error -> throw response.error
        }
    }

    fun getPaywallByVariationId(id: String, locale: String, segmentId: String, variationId: String): PaywallDto {
        val response = httpClient.newCall<PaywallDto>(
            requestFactory.getPaywallByVariationIdRequest(id, locale, segmentId, variationId),
            PaywallDto::class.java,
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getPaywallVariationsFallback(id: String, locale: String): Variations {
        val response = httpClient.newCall<Variations>(
            requestFactory.getPaywallVariationsFallbackRequest(id, locale),
            Variations::class.java,
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> {
                val error = response.error
                when {
                    error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PAYWALL_LOCALE ->
                        return getPaywallVariationsFallback(id, DEFAULT_PAYWALL_LOCALE)
                    else -> throw response.error
                }
            }
        }
    }

    @JvmSynthetic
    fun getPaywallByVariationIdFallback(id: String, locale: String, variationId: String): PaywallDto {
        val response = httpClient.newCall<PaywallDto>(
            requestFactory.getPaywallByVariationIdFallbackRequest(id, locale, variationId),
            PaywallDto::class.java,
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> {
                val error = response.error
                when {
                    error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PAYWALL_LOCALE ->
                        return getPaywallByVariationIdFallback(id, DEFAULT_PAYWALL_LOCALE, variationId)
                    else -> throw response.error
                }
            }
        }
    }

    @JvmSynthetic
    fun getPaywallVariationsUntargeted(id: String, locale: String): Variations {
        val response = httpClient.newCall<Variations>(
            requestFactory.getPaywallVariationsUntargetedRequest(id, locale),
            Variations::class.java,
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> {
                val error = response.error
                when {
                    error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PAYWALL_LOCALE ->
                        return getPaywallVariationsUntargeted(id, DEFAULT_PAYWALL_LOCALE)
                    else -> throw response.error
                }
            }
        }
    }

    @JvmSynthetic
    fun getViewConfiguration(variationId: String, locale: String): Map<String, Any> {
        val response = httpClient.newCall<Map<String, Any>>(
            requestFactory.getViewConfigurationRequest(variationId, locale),
            object : TypeToken<Map<String, Any>>() {}.type
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getViewConfigurationFallback(paywallId: String, locale: String): Map<String, Any> {
        val response = httpClient.newCall<Map<String, Any>>(
            requestFactory.getViewConfigurationFallbackRequest(paywallId, locale),
            object : TypeToken<Map<String, Any>>() {}.type
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> {
                val error = response.error
                when {
                    error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PAYWALL_LOCALE ->
                        return getViewConfigurationFallback(paywallId, DEFAULT_PAYWALL_LOCALE)
                    else -> throw response.error
                }
            }
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
        purchase: Purchase,
        product: PurchaseableProduct,
    ): Pair<ProfileDto, Request.CurrentDataWhenSent?> {
        val request = requestFactory.validatePurchaseRequest(purchase, product)
        val response = httpClient.newCall<ValidationResult>(
            request,
            ValidationResult::class.java
        )
        when (response) {
            is Response.Success -> {
                val result = response.body
                val error = result.errors.firstOrNull()
                if (error != null) {
                    throw AdaptyError(
                        message = error.message.orEmpty(),
                        adaptyErrorCode = AdaptyErrorCode.BAD_REQUEST,
                    )
                }
                return result.profile to request.currentDataWhenSent
            }
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
        params: AdaptyProfileParameters? = null,
        installationMeta: InstallationMeta? = null,
        ipv4Address: String? = null,
    ): Pair<ProfileDto, Request.CurrentDataWhenSent?> {
        val request = requestFactory.updateProfileRequest(params, installationMeta, ipv4Address)
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
    fun updateAttribution(attributionData: AttributionData): Pair<ProfileDto, Request.CurrentDataWhenSent?> {
        val request = requestFactory.updateAttributionRequest(attributionData)
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
    fun setIntegrationId(key: String, value: String) {
        val request = requestFactory.setIntegrationIdRequest(key, value)
        val response = httpClient.newCall<Any>(
            request,
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

    @JvmSynthetic
    fun getCrossPlacementInfo(replacementProfileId: String?): CrossPlacementInfo {
        val response = httpClient.newCall<CrossPlacementInfo>(
            requestFactory.getCrossPlacementInfoRequest(replacementProfileId),
            CrossPlacementInfo::class.java
        )

        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun reportTransactionWithVariation(
        transactionId: String,
        variationId: String,
        purchase: Purchase,
        product: ProductDetails,
    ): Pair<ProfileDto, Request.CurrentDataWhenSent?> {
        val request = requestFactory.reportTransactionWithVariationRequest(transactionId, variationId, purchase, product)
        val response = httpClient.newCall<ValidationResult>(
            request,
            ValidationResult::class.java
        )
        when (response) {
            is Response.Success -> {
                val result = response.body
                val error = result.errors.firstOrNull()
                if (error != null) {
                    throw AdaptyError(
                        message = error.message.orEmpty(),
                        adaptyErrorCode = AdaptyErrorCode.BAD_REQUEST,
                    )
                }
                return result.profile to request.currentDataWhenSent
            }
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getIPv4Request(): IP {
        val response = httpClient.newCall<IP>(
            requestFactory.getIPv4Request(),
            IP::class.java
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    private fun processEmptyResponse(response: Response<*>) {
        when (response) {
            is Response.Success -> return
            is Response.Error -> throw response.error
        }
    }
}