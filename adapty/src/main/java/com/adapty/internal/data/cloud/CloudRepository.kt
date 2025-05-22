package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.*
import com.adapty.internal.domain.VariationType
import com.adapty.internal.domain.models.PurchaseableProduct
import com.adapty.internal.utils.DEFAULT_PLACEMENT_LOCALE
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

    fun getVariations(id: String, locale: String, segmentId: String, variationType: VariationType): Pair<Variations, Request.CurrentDataWhenSent?> {
        val request = when (variationType) {
            VariationType.Paywall -> requestFactory.getPaywallVariationsRequest(id, locale, segmentId)
            VariationType.Onboarding -> requestFactory.getOnboardingVariationsRequest(id, locale, segmentId)
        }
        val response = httpClient.newCall<Variations>(
            request,
            Variations::class.java,
        )
        when (response) {
            is Response.Success -> return response.body to request.currentDataWhenSent
            is Response.Error -> throw response.error
        }
    }

    fun getVariationById(id: String, locale: String, segmentId: String, variationId: String, variationType: VariationType): Variation {
        val response = httpClient.newCall<Variation>(
            when (variationType) {
                VariationType.Paywall -> requestFactory.getPaywallByVariationIdRequest(id, locale, segmentId, variationId)
                VariationType.Onboarding -> requestFactory.getOnboardingByVariationIdRequest(id, locale, segmentId, variationId)
            },
            Variation::class.java,
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getVariationsFallback(id: String, locale: String, variationType: VariationType): Variations {
        val response = httpClient.newCall<Variations>(
            when (variationType) {
                VariationType.Paywall -> requestFactory.getPaywallVariationsFallbackRequest(id, locale)
                VariationType.Onboarding -> requestFactory.getOnboardingVariationsFallbackRequest(id, locale)
            },
            Variations::class.java,
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> {
                val error = response.error
                when {
                    error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PLACEMENT_LOCALE ->
                        return getVariationsFallback(id, DEFAULT_PLACEMENT_LOCALE, variationType)
                    else -> throw response.error
                }
            }
        }
    }

    fun getVariationByIdFallback(id: String, locale: String, variationId: String, variationType: VariationType): Variation {
        val response = httpClient.newCall<Variation>(
            when (variationType) {
                VariationType.Paywall -> requestFactory.getPaywallByVariationIdFallbackRequest(id, locale, variationId)
                VariationType.Onboarding -> requestFactory.getOnboardingByVariationIdFallbackRequest(id, locale, variationId)
            },
            Variation::class.java,
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> {
                val error = response.error
                when {
                    error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PLACEMENT_LOCALE ->
                        return getVariationByIdFallback(id, DEFAULT_PLACEMENT_LOCALE, variationId, variationType)
                    else -> throw response.error
                }
            }
        }
    }

    fun getVariationsUntargeted(id: String, locale: String, variationType: VariationType): Variations {
        val response = httpClient.newCall<Variations>(
            when (variationType) {
                VariationType.Paywall -> requestFactory.getPaywallVariationsUntargetedRequest(id, locale)
                VariationType.Onboarding -> requestFactory.getOnboardingVariationsUntargetedRequest(id, locale)
            },
            Variations::class.java,
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> {
                val error = response.error
                when {
                    error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PLACEMENT_LOCALE ->
                        return getVariationsUntargeted(id, DEFAULT_PLACEMENT_LOCALE, variationType)
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
                    error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PLACEMENT_LOCALE ->
                        return getViewConfigurationFallback(paywallId, DEFAULT_PLACEMENT_LOCALE)
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