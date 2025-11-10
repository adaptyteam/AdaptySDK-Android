package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.data.models.ProductPALMappings
import com.adapty.internal.data.models.ProfileDto
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class OfflineProfileManager(
    private val cacheRepository: CacheRepository,
    private val storeManager: StoreManager,
) {

    fun getLocalPAL(): Flow<ProductPALMappings.ItemExtended?> {
        val unsyncedValidateData = cacheRepository.getUnsyncedValidateData()?.takeIf { it.isNotEmpty() }
            ?: run {
                Logger.log(WARN) { "Cannot retrieve local access level: no unsynced validate data available" }
                return flowOf(null)
            }

        val productPalMappings = cacheRepository.getProductPALMappings()?.takeIf { it.items.isNotEmpty() }
            ?: run {
                Logger.log(WARN) { "Cannot retrieve local access level: no product-to-access-level mappings available" }
                return flowOf(null)
            }

        return storeManager.queryActivePurchases(DEFAULT_RETRY_COUNT)
            .catch { emit(listOf()) }
            .map { purchases ->
                if (purchases.isEmpty()) return@map null
                var productPalMappingItem: ProductPALMappings.Item? = null
                var productId: String? = null
                var basePlanId: String? = null
                var offerId: String? = null
                var purchase: Purchase? = null
                var endTimestamp = 0L
                for (currentPurchase in purchases) {
                    val currentProductId = currentPurchase.products.firstOrNull() ?: continue
                    val currentValidateData = unsyncedValidateData[currentProductId] ?: continue
                    val currentBasePlanId = currentValidateData.basePlanId
                    val currentKey = combinedProductId(currentProductId, currentBasePlanId)
                    val currentProductPalMappingItem = productPalMappings.items[currentKey]
                        ?.takeIf { it.productType !in listOf("consumable", "nonsubscriptions", "uncategorised") }
                        ?: continue
                    val duration = Duration.fromProductType(currentProductPalMappingItem.productType)
                        .takeIf { it.isPositive() }
                        ?: continue
                    val currentEndTimestamp = if (duration.isInfinite()) Long.MAX_VALUE else currentPurchase.purchaseTime + duration.inWholeMilliseconds
                    if (currentEndTimestamp > endTimestamp) {
                        endTimestamp = currentEndTimestamp
                        productPalMappingItem = currentProductPalMappingItem
                        productId = currentProductId
                        basePlanId = currentBasePlanId
                        offerId = currentValidateData.offerId
                        purchase = currentPurchase
                    }
                }
                if (productPalMappingItem == null || productId == null || purchase == null)
                    return@map null
                ProductPALMappings.ItemExtended(
                    productId,
                    basePlanId,
                    offerId,
                    productPalMappingItem.accessLevelId,
                    productPalMappingItem.productType,
                    purchase,
                    endTimestamp,
                )
            }
    }

    fun constructProfile(): ProfileDto {
        val (profileId, identityParams) = cacheRepository.getUnsyncedAuthData()
        return ProfileDto(
            profileId ?: cacheRepository.getProfileId(),
            identityParams?.customerUserId,
            "",
            false,
            System.currentTimeMillis(),
            null,
            null,
            null,
            null,
        )
    }
}