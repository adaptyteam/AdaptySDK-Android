@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.domain.models.ProfileRequestResult
import com.adapty.internal.domain.models.ProfileRequestResult.*
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyProfileParameters
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AuthInteractor(
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val lifecycleManager: LifecycleManager,
    private val installationMetaCreator: InstallationMetaCreator,
    private val adIdRetriever: AdIdRetriever,
    private val appSetIdRetriever: AppSetIdRetriever,
    private val storeCountryRetriever: StoreCountryRetriever,
    private val hashingHelper: HashingHelper,
) {

    @JvmSynthetic
    fun activateOrIdentify() =
        lifecycleManager.onActivateAllowed()
            .flatMapConcat { createProfileIfNeeded() }
            .retryIfNecessary(DEFAULT_RETRY_COUNT)
            .flowOnIO()

    private val authSemaphore = Semaphore(1)

    private suspend fun createProfileIfNeeded(): Flow<ProfileRequestResult> {
        cacheRepository.getUnsyncedAuthData().let { (newProfileId, newCustomerUserId) ->
            if (newProfileId.isNullOrEmpty() && newCustomerUserId.isNullOrEmpty()) {
                return flowOf(ProfileIdSame)
            }
        }

        authSemaphore.acquire()
        val (newProfileId, newCustomerUserId) = cacheRepository.getUnsyncedAuthData()
        return if (newProfileId.isNullOrEmpty() && newCustomerUserId.isNullOrEmpty()) {
            authSemaphore.release()
            flowOf(ProfileIdSame)
        } else {
            createInstallationMeta(true)
                .flatMapConcat { installationMeta ->
                    val params = cacheRepository.getExternalAnalyticsEnabled()?.let { enabled ->
                        AdaptyProfileParameters.Builder().withExternalAnalyticsDisabled(!enabled).build()
                    }
                    cloudRepository.createProfile(newCustomerUserId, installationMeta, params)
                        .map { profile ->
                            val profileIdHasChanged =
                                cacheRepository.updateDataOnCreateProfile(profile, installationMeta)
                            if (profileIdHasChanged) ProfileIdChanged else ProfileIdSame
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
        val cachedHash = cacheRepository.getAppKey()
        if (keyHash != cachedHash) {
            if (!cachedHash.isNullOrEmpty() && keyHash != hashingHelper.sha256(cachedHash))
                cacheRepository.clearOnAppKeyChanged()
            Logger.log(VERBOSE) { "changing apiKeyHash = $keyHash" }
            cacheRepository.saveAppKey(keyHash)
        }
    }

    @JvmSynthetic
    fun getCustomerUserId() =
        cacheRepository.getCustomerUserId()

    @JvmSynthetic
    fun createInstallationMeta(isCreatingProfile: Boolean) =
        combine(
            adIdRetriever.getAdIdIfAvailable(),
            storeCountryRetriever.getStoreCountryIfAvailable(!isCreatingProfile),
            appSetIdRetriever.getAppSetIdIfAvailable(),
        ) { adId, storeCountry, appSetId ->
            installationMetaCreator.create(adId, appSetId, storeCountry)
        }

    @JvmSynthetic
    fun prepareAuthDataToSync(newCustomerUserId: String?) {
        cacheRepository.prepareProfileIdToSync()
        cacheRepository.prepareCustomerUserIdToSync(newCustomerUserId)
    }

    @JvmSynthetic
    fun <T> runWhenAuthDataSynced(
        maxAttemptCount: Long = DEFAULT_RETRY_COUNT,
        switchIfProfileCreationFailed: (() -> T?)? = null,
        call: suspend () -> T,
    ): Flow<T> {
        return lifecycleManager.onActivateAllowed()
            .flatMapConcat { createProfileIfNeeded() }
            .catch { error ->
                if (switchIfProfileCreationFailed == null) {
                    throw error
                } else {
                    emit(ProfileNotCreated(error))
                }
            }
            .mapLatest { result ->
                when (result) {
                    is ProfileNotCreated -> switchIfProfileCreationFailed?.invoke() ?: throw result.error
                    else -> call()
                }
            }
            .retryIfNecessary(maxAttemptCount)
            .flowOnIO()
    }
}