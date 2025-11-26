package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.*
import com.adapty.internal.data.models.requests.ValidateReceiptRequest
import com.adapty.internal.domain.VariationType
import com.adapty.internal.domain.models.IdentityParams
import com.adapty.internal.utils.DEFAULT_PLACEMENT_LOCALE
import com.adapty.models.AdaptyProfileParameters
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.google.gson.reflect.TypeToken

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CloudRepository(
    private val httpClient: HttpClient,
    private val mainRequestFactory: MainRequestFactory,
    private val auxRequestFactory: AuxRequestFactory,
) {

    fun getProfile(): Response<ProfileDto> =
        httpClient.newCall(
            mainRequestFactory.getProfileRequest(),
            ProfileDto::class.java
        )

    fun getProducts(): Response<ProductPALMappings> =
        httpClient.newCall(
            mainRequestFactory.getProductsRequest(),
            object : TypeToken<ProductPALMappings>() {}.type,
        )

    fun getVariations(
        id: String,
        locale: String,
        segmentId: String,
        variationType: VariationType,
    ): Response<Variations> =
        httpClient.newCall(
            when (variationType) {
                VariationType.Paywall -> mainRequestFactory.getPaywallVariationsRequest(id, locale, segmentId)
                VariationType.Onboarding -> mainRequestFactory.getOnboardingVariationsRequest(id, locale, segmentId)
            },
            Variations::class.java,
        )

    fun getVariationById(
        id: String,
        locale: String,
        segmentId: String,
        variationId: String,
        variationType: VariationType,
    ): Response<Variation> =
        httpClient.newCall(
            when (variationType) {
                VariationType.Paywall -> mainRequestFactory.getPaywallByVariationIdRequest(id, locale, segmentId, variationId)
                VariationType.Onboarding -> mainRequestFactory.getOnboardingByVariationIdRequest(id, locale, segmentId, variationId)
            },
            Variation::class.java,
        )

    fun registerInstall(
        installRegistrationData: InstallRegistrationData,
        retryAttempt: Long,
        maxRetries: Long,
    ): Response<InstallRegistrationResponseData> =
        httpClient.newCall(
            auxRequestFactory.registerInstallRequest(installRegistrationData, retryAttempt, maxRetries),
            InstallRegistrationResponseData::class.java,
        )

    fun getVariationsFallback(id: String, locale: String, variationType: VariationType): Response<Variations> =
        try {
            httpClient.newCall(
                when (variationType) {
                    VariationType.Paywall -> auxRequestFactory.getPaywallVariationsFallbackRequest(id, locale)
                    VariationType.Onboarding -> auxRequestFactory.getOnboardingVariationsFallbackRequest(id, locale)
                },
                Variations::class.java,
            )
        } catch (error: Response.Error) {
            when {
                error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PLACEMENT_LOCALE ->
                    getVariationsFallback(id, DEFAULT_PLACEMENT_LOCALE, variationType)
                else -> throw error
            }
        }

    fun getVariationByIdFallback(id: String, locale: String, variationId: String, variationType: VariationType): Response<Variation> =
        try {
            httpClient.newCall(
                when (variationType) {
                    VariationType.Paywall -> auxRequestFactory.getPaywallByVariationIdFallbackRequest(id, locale, variationId)
                    VariationType.Onboarding -> auxRequestFactory.getOnboardingByVariationIdFallbackRequest(id, locale, variationId)
                },
                Variation::class.java,
            )
        } catch (error: Response.Error) {
            when {
                error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PLACEMENT_LOCALE ->
                    getVariationByIdFallback(id, DEFAULT_PLACEMENT_LOCALE, variationId, variationType)
                else -> throw error
            }
        }

    fun getVariationsUntargeted(id: String, locale: String, variationType: VariationType): Response<Variations> =
        try {
            httpClient.newCall(
                when (variationType) {
                    VariationType.Paywall -> auxRequestFactory.getPaywallVariationsUntargetedRequest(id, locale)
                    VariationType.Onboarding -> auxRequestFactory.getOnboardingVariationsUntargetedRequest(id, locale)
                },
                Variations::class.java,
            )
        } catch (error: Response.Error) {
            when {
                error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PLACEMENT_LOCALE ->
                    getVariationsUntargeted(id, DEFAULT_PLACEMENT_LOCALE, variationType)
                else -> throw error
            }
        }

    fun getViewConfiguration(variationId: String, locale: String): Response<Map<String, Any>> =
        httpClient.newCall(
            mainRequestFactory.getViewConfigurationRequest(variationId, locale),
            object : TypeToken<Map<String, Any>>() {}.type
        )

    fun getViewConfigurationFallback(paywallId: String, locale: String): Response<Map<String, Any>> =
        try {
            httpClient.newCall(
                auxRequestFactory.getViewConfigurationFallbackRequest(paywallId, locale),
                object : TypeToken<Map<String, Any>>() {}.type
            )
        } catch (error: Response.Error) {
            when {
                error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PLACEMENT_LOCALE ->
                    getViewConfigurationFallback(paywallId, DEFAULT_PLACEMENT_LOCALE)
                else -> throw error
            }
        }

    fun createProfile(
        identityParams: IdentityParams?,
        installationMeta: InstallationMeta,
        params: AdaptyProfileParameters?,
    ): Response<ProfileDto> =
        httpClient.newCall(
            mainRequestFactory.createProfileRequest(identityParams, installationMeta, params),
            ProfileDto::class.java
        )

    fun validatePurchase(
        validateData: ValidateReceiptRequest,
        purchase: Purchase?,
    ): Response<ValidationResult> {
        val request = mainRequestFactory.validatePurchaseRequest(validateData, purchase)
        val response = httpClient.newCall<ValidationResult>(
            request,
            ValidationResult::class.java
        )
        val validationError = response.data.errors.firstOrNull()
        if (validationError != null)
            throw Response.Error(
                message = validationError.message.orEmpty(),
                adaptyErrorCode = AdaptyErrorCode.BAD_REQUEST,
                request = request,
            )
        return response
    }

    fun restorePurchases(purchases: List<RestoreProductInfo>): Response<ProfileDto> =
        httpClient.newCall(
            mainRequestFactory.restorePurchasesRequest(purchases),
            ProfileDto::class.java
        )

    fun updateProfile(
        params: AdaptyProfileParameters? = null,
        installationMeta: InstallationMeta? = null,
        ipv4Address: String? = null,
    ): Response<ProfileDto> =
        httpClient.newCall(
            mainRequestFactory.updateProfileRequest(params, installationMeta, ipv4Address),
            ProfileDto::class.java
        )

    fun updateAttribution(attributionData: AttributionData): Response<ProfileDto> =
        httpClient.newCall(
            mainRequestFactory.updateAttributionRequest(attributionData),
            ProfileDto::class.java
        )

    fun setIntegrationId(key: String, value: String) {
        httpClient.newCall<Any>(
            mainRequestFactory.setIntegrationIdRequest(key, value),
            Any::class.java
        )
    }

    fun setVariationId(transactionId: String, variationId: String) {
        httpClient.newCall<Any>(
            mainRequestFactory.setVariationIdRequest(transactionId, variationId),
            Any::class.java
        )
    }

    fun getCrossPlacementInfo(replacementProfileId: String?): Response<CrossPlacementInfo> =
        httpClient.newCall(
            mainRequestFactory.getCrossPlacementInfoRequest(replacementProfileId),
            CrossPlacementInfo::class.java
        )

    fun reportTransactionWithVariation(
        transactionId: String,
        variationId: String,
        purchase: Purchase,
        product: ProductDetails,
    ): Response<ValidationResult> {
        val request = mainRequestFactory.reportTransactionWithVariationRequest(transactionId, variationId, purchase, product)
        val response = httpClient.newCall<ValidationResult>(
            request,
            ValidationResult::class.java
        )
        val validationError = response.data.errors.firstOrNull()
        if (validationError != null)
            throw Response.Error(
                message = validationError.message.orEmpty(),
                adaptyErrorCode = AdaptyErrorCode.BAD_REQUEST,
                request = request,
            )
        return response
    }

    fun getIPv4Request(): Response<IP> =
        httpClient.newCall(
            auxRequestFactory.getIPv4Request(),
            IP::class.java
        )
}