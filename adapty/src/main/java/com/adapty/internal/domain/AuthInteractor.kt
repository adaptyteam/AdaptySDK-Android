package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.utils.PurchaserInfoMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AuthInteractor(
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
) {

    @JvmSynthetic
    fun activate(customerUserId: String?) =
        onActivateAllowed()
            .map { getProfileIdOnStart() }
            .flatMapConcat { profileId ->
                if (profileId.isNullOrEmpty()) {
                    cloudRepository.createProfile(customerUserId)
                        .map(cacheRepository::updateDataOnCreateProfile)
                } else {
                    flowOf(Unit)
                }
            }

    @JvmSynthetic
    fun identify(customerUserId: String): Flow<Boolean> =
        cloudRepository.createProfile(customerUserId)
            .map { newPurchaserInfoResponse ->
                val isProfileIdChanged = newPurchaserInfoResponse?.profileId?.let { profileId ->
                    cacheRepository.getProfileId().orEmpty() != profileId
                } ?: false

                cacheRepository.updateDataOnCreateProfile(newPurchaserInfoResponse)
                newPurchaserInfoResponse?.let(PurchaserInfoMapper::map)
                    ?.let { cacheRepository.savePurchaserInfo(it) }
                if (isProfileIdChanged) {
                    cacheRepository.clearContainersAndProducts()
                }
                isProfileIdChanged
            }

    @JvmSynthetic
    fun clearDataOnLogout() {
        cacheRepository.clearOnLogout()
    }

    @JvmSynthetic
    fun saveAppKey(appKey: String) =
        cacheRepository.saveAppKey(appKey)

    @JvmSynthetic
    fun getProfileIdOnStart() =
        cacheRepository.getProfileId()

    @JvmSynthetic
    fun getCustomerUserId() =
        cacheRepository.getCustomerUserId()

    @JvmSynthetic
    fun onActivateAllowed() =
        cloudRepository.onActivateAllowed()

    @JvmSynthetic
    fun blockRequests() =
        cloudRepository.blockRequests()

    @JvmSynthetic
    fun unblockRequests() =
        cloudRepository.unblockRequests()
}