package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.*
import com.adapty.internal.domain.models.PurchaseableProduct
import com.adapty.internal.utils.DEFAULT_PAYWALL_LOCALE
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
    fun getPaywall(id: String, locale: String, segmentId: String): PaywallDto {
        val response = httpClient.newCall<PaywallDto>(
            requestFactory.getPaywallRequest(id, locale, segmentId),
            PaywallDto::class.java
        )
        when (response) {
            is Response.Success -> return response.body.takeIf { it.remoteConfig != null }
                ?: response.body.copy(remoteConfig = RemoteConfigDto(locale, null))
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getPaywallFallback(id: String, locale: String): PaywallDto {
        val response = httpClient.newCall<PaywallDto>(
            requestFactory.getPaywallFallbackRequest(id, locale),
            PaywallDto::class.java
        )
        when (response) {
            is Response.Success -> return response.body.takeIf { it.remoteConfig != null }
                ?: response.body.copy(remoteConfig = RemoteConfigDto(locale, null))
            is Response.Error -> {
                val error = response.error
                when {
                    error.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST && locale != DEFAULT_PAYWALL_LOCALE ->
                        return getPaywallFallback(id, DEFAULT_PAYWALL_LOCALE)
                    else -> throw response.error
                }
            }
        }
    }

    @JvmSynthetic
    fun getViewConfiguration(variationId: String, locale: String): ViewConfigurationDto {
        val response = httpClient.newCall<ViewConfigurationDto>(
            requestFactory.getViewConfigurationRequest(variationId, locale),
            ViewConfigurationDto::class.java
        )
        when (response) {
            is Response.Success -> return response.body
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getViewConfigurationFallback(paywallId: String, locale: String): ViewConfigurationDto {
        val response = httpClient.newCall<ViewConfigurationDto>(
            requestFactory.getViewConfigurationFallbackRequest(paywallId, locale),
            ViewConfigurationDto::class.java
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