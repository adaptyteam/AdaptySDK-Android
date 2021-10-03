package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.utils.DEFAULT_RETRY_COUNT
import com.adapty.internal.utils.flowOnIO
import com.adapty.internal.utils.retryIfNecessary
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AuthInteractor(
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
) {

    @JvmSynthetic
    fun activateOrIdentify() =
        cloudRepository.onActivateAllowed()
            .flatMapConcat { createProfileIfNeeded() }
            .retryIfNecessary(DEFAULT_RETRY_COUNT)
            .flowOnIO()

    private val authSemaphore = Semaphore(1)

    private suspend fun createProfileIfNeeded(): Flow<Unit> {
        cacheRepository.getUnsyncedAuthData().let { (newProfileId, newCustomerUserId) ->
            if (newProfileId.isNullOrEmpty() && newCustomerUserId.isNullOrEmpty()) {
                return flowOf(Unit)
            }
        }

        authSemaphore.acquire()
        val (newProfileId, newCustomerUserId) = cacheRepository.getUnsyncedAuthData()
        return if (newProfileId.isNullOrEmpty() && newCustomerUserId.isNullOrEmpty()) {
            authSemaphore.release()
            flowOf(Unit)
        } else {
            cloudRepository.createProfile(newCustomerUserId)
                .map(cacheRepository::updateDataOnCreateProfile)
                .onEach { authSemaphore.release() }
                .catch { error -> authSemaphore.release(); throw error }
        }
    }

    @JvmSynthetic
    fun clearDataOnLogout() {
        cacheRepository.clearOnLogout()
    }

    @JvmSynthetic
    fun saveAppKey(appKey: String) =
        cacheRepository.saveAppKey(appKey)

    @JvmSynthetic
    fun getCustomerUserId() =
        cacheRepository.getCustomerUserId()

    @JvmSynthetic
    fun prepareAuthDataToSync(newCustomerUserId: String?) {
        cacheRepository.prepareProfileIdToSync()
        cacheRepository.prepareCustomerUserIdToSync(newCustomerUserId)
    }

    @JvmSynthetic
    fun <T> runWhenAuthDataSynced(
        maxAttemptCount: Long = DEFAULT_RETRY_COUNT,
        call: suspend () -> T
    ): Flow<T> {
        return cloudRepository.onActivateAllowed()
            .flatMapConcat { createProfileIfNeeded() }
            .mapLatest { call() }
            .retryIfNecessary(maxAttemptCount)
            .flowOnIO()
    }
}