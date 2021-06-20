package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.*
import com.adapty.internal.data.models.responses.*
import com.adapty.internal.data.models.responses.UpdateProfileResponse.Data.Attributes
import com.adapty.internal.utils.PurchaserInfoMapper
import com.adapty.internal.utils.flowOnIO
import com.adapty.internal.utils.retryIfNecessary
import com.adapty.models.PurchaserInfoModel
import com.adapty.utils.ProfileParameterBuilder
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CloudRepository(
    private val httpClient: HttpClient,
    private val requestFactory: RequestFactory
) {

    private val isActivateAllowed = MutableStateFlow(false)

    private val areRequestsAllowed = MutableStateFlow(false)

    @JvmSynthetic
    fun blockRequests() {
        areRequestsAllowed.compareAndSet(expect = true, update = false)
    }

    @JvmSynthetic
    fun unblockRequests() {
        areRequestsAllowed.compareAndSet(expect = false, update = true)
    }

    @get:JvmSynthetic
    val arePaywallRequestsAllowed = MutableStateFlow(false)

    @JvmSynthetic
    fun getPurchaserInfo(): Flow<PurchaserInfoModel?> {
        return runWhenAllowed { getPurchaserInfoForced() }
    }

    @JvmSynthetic
    fun getPurchaserInfoForced(): PurchaserInfoModel? {
        val response =
            httpClient.newCall(
                requestFactory.getPurchaserInfoRequest(),
                PurchaserInfoResponse::class.java
            )
        when (response) {
            is Response.Success -> return response.body.let(PurchaserInfoMapper::map)
            is Response.Error -> throw response.error
        }
    }

    @JvmSynthetic
    fun getPaywalls(): Flow<Pair<ArrayList<PaywallsResponse.Data>, ArrayList<ProductDto>>> {
        return runWhenAllowed { getPaywallsForced() }
    }

    @JvmSynthetic
    fun getPaywallsForced(): Pair<ArrayList<PaywallsResponse.Data>, ArrayList<ProductDto>> {
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
    fun getPromo(): Flow<PromoDto?> {
        return runWhenAllowed { getPromoForced() }
    }

    @JvmSynthetic
    fun getPromoForced(): PromoDto? {
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
            .retryIfNecessary()
            .flowOnIO()

    @JvmSynthetic
    fun validatePurchase(
        purchaseType: String,
        purchase: Purchase,
        product: ValidateProductInfo?,
    ): Flow<ValidateReceiptResponse> =
        flow {
            val response = httpClient.newCall(
                requestFactory.validatePurchaseRequest(
                    purchaseType,
                    purchase,
                    product
                ),
                ValidateReceiptResponse::class.java
            )
            when (response) {
                is Response.Success -> emit(response.body)
                is Response.Error -> throw response.error
            }
        }
            .retryIfNecessary()
            .flowOnIO()

    @JvmSynthetic
    fun syncMeta(
        advertisingId: String?,
        pushToken: String?
    ): Flow<SyncMetaResponse.Data.Attributes?> {
        return runWhenAllowed { syncMetaForced(advertisingId, pushToken) }
    }

    @JvmSynthetic
    fun syncMetaForced(
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
    fun restorePurchases(purchases: List<RestoreProductInfo>): Flow<RestoreReceiptResponse> {
        return runWhenAllowed { restorePurchasesForced(purchases) }
    }

    @JvmSynthetic
    fun restorePurchasesForced(purchases: List<RestoreProductInfo>): RestoreReceiptResponse {
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
    fun updateProfile(params: ProfileParameterBuilder): Flow<Attributes?> {
        return runWhenAllowed {
            val response =
                httpClient.newCall(
                    requestFactory.updateProfileRequest(params),
                    UpdateProfileResponse::class.java
                )
            when (response) {
                is Response.Success -> response.body.data?.attributes
                is Response.Error -> throw response.error
            }
        }
    }

    @JvmSynthetic
    fun updateAttribution(
        attributionData: AttributionData,
    ): Flow<Unit> {
        return runWhenAllowed {
            val response = httpClient.newCall(
                requestFactory.updateAttributionRequest(attributionData),
                Any::class.java
            )
            processEmptyResponse(response)
        }
    }

    @JvmSynthetic
    fun setTransactionVariationId(
        transactionId: String,
        variationId: String,
    ): Flow<Unit> {
        return runWhenAllowed {
            val response = httpClient.newCall(
                requestFactory.setTransactionVariationIdRequest(transactionId, variationId),
                Any::class.java
            )
            processEmptyResponse(response)
        }
    }

    @JvmSynthetic
    fun setExternalAnalyticsEnabled(
        enabled: Boolean,
    ): Flow<Unit> {
        return runWhenAllowed {
            val response = httpClient.newCall(
                requestFactory.setExternalAnalyticsEnabledRequest(enabled),
                Any::class.java
            )
            processEmptyResponse(response)
        }
    }

    private fun processEmptyResponse(response: Response<*>) {
        when (response) {
            is Response.Success -> return
            is Response.Error -> throw response.error
        }
    }

    private fun <T> runWhenAllowed(call: suspend () -> T): Flow<T> {
        return areRequestsAllowed
            .filter { it }
            .take(1)
            .mapLatest { call() }
            .retryIfNecessary(3)
            .flowOnIO()
    }

    @JvmSynthetic
    fun allowActivate() {
        isActivateAllowed.compareAndSet(expect = false, update = true)
    }

    @JvmSynthetic
    fun onActivateAllowed() =
        isActivateAllowed
            .filter { it }
            .take(1)
}