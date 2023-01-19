package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyProfileParameters
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AuthInteractor(
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val installationMetaCreator: InstallationMetaCreator,
    private val adIdRetriever: AdIdRetriever,
    private val hashingHelper: HashingHelper,
) {

    @JvmSynthetic
    fun activateOrIdentify() =
        cloudRepository.onActivateAllowed()
            .flatMapConcat { createProfileIfNeeded() }
            .retryIfNecessary(DEFAULT_RETRY_COUNT)
            .flowOnIO()

    private val authSemaphore = Semaphore(1)

    private suspend fun createProfileIfNeeded(): Flow<Boolean> {
        cacheRepository.getUnsyncedAuthData().let { (newProfileId, newCustomerUserId) ->
            if (newProfileId.isNullOrEmpty() && newCustomerUserId.isNullOrEmpty()) {
                return flowOf(false)
            }
        }

        authSemaphore.acquire()
        val (newProfileId, newCustomerUserId) = cacheRepository.getUnsyncedAuthData()
        return if (newProfileId.isNullOrEmpty() && newCustomerUserId.isNullOrEmpty()) {
            authSemaphore.release()
            flowOf(false)
        } else {
            createInstallationMeta()
                .flatMapConcat { installationMeta ->
                    val params = cacheRepository.getExternalAnalyticsEnabled()?.let { enabled ->
                        AdaptyProfileParameters.Builder().withExternalAnalyticsDisabled(!enabled).build()
                    }
                    cloudRepository.createProfile(newCustomerUserId, installationMeta, params)
                        .map { profile ->
                            cacheRepository.updateDataOnCreateProfile(profile, installationMeta)
                        }
                        .onEach { authSemaphore.release() }
                        .catch { error -> authSemaphore.release(); throw error }
                }
        }
    }

    @JvmSynthetic
    fun clearDataOnLogout() {
        cacheRepository.clearOnLogout()
    }

    @JvmSynthetic
    fun handleAppKey(appKey: String) {
        val keyHash = hashingHelper.sha256(appKey)
        if (keyHash != cacheRepository.getAppKey()) {
            Logger.log(VERBOSE) { "changing apiKeyHash = $keyHash" }
            cacheRepository.clearOnAppKeyChanged()
            cacheRepository.saveAppKey(keyHash)
        }
    }

    @JvmSynthetic
    fun getCustomerUserId() =
        cacheRepository.getCustomerUserId()

    @JvmSynthetic
    fun createInstallationMeta() =
        adIdRetriever.getAdIdIfAvailable().map { adId -> installationMetaCreator.create(adId) }

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