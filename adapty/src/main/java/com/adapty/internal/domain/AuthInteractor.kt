@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.models.CrossPlacementInfo
import com.adapty.internal.domain.models.IdentityParams
import com.adapty.internal.domain.models.ProfileRequestResult
import com.adapty.internal.domain.models.ProfileRequestResult.*
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyProfileParameters
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import kotlinx.coroutines.delay
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
    private val profileStateChangeChecker: ProfileStateChangeChecker,
) {

    @JvmSynthetic
    fun activateOrIdentify() =
        lifecycleManager.onActivateAllowed()
            .flatMapConcat { createProfileIfNeeded() }
            .retryIfNecessary(DEFAULT_RETRY_COUNT)

    private val authSemaphore = Semaphore(1)

    private suspend fun createProfileIfNeeded(): Flow<ProfileRequestResult> {
        cacheRepository.getUnsyncedAuthData().let { (newProfileId, newIdentityParams) ->
            if (newProfileId.isNullOrEmpty() && newIdentityParams == null) {
                return flowOf(ProfileIdSame)
            }
        }

        authSemaphore.acquire()
        val (newProfileId, newIdentityParams) = cacheRepository.getUnsyncedAuthData()
        return if (newProfileId.isNullOrEmpty() && newIdentityParams == null) {
            authSemaphore.release()
            flowOf(ProfileIdSame)
        } else {
            createInstallationMeta(true)
                .map { installationMeta ->
                    val params = cacheRepository.getExternalAnalyticsEnabled()?.let { enabled ->
                        AdaptyProfileParameters.Builder().withExternalAnalyticsDisabled(!enabled).build()
                    }
                    val (profile, _) = cloudRepository.createProfile(newIdentityParams, installationMeta, params)

                    val profileStateChange = profileStateChangeChecker.getProfileStateChange(profile)
                    when (profileStateChange) {
                        ProfileStateChange.NEW -> {
                            cacheRepository.updateDataOnCreateProfile(profile, installationMeta, profileStateChange)
                            cacheRepository.saveCrossPlacementInfo(CrossPlacementInfo.forNewProfile())
                            ProfileIdChanged
                        }
                        ProfileStateChange.IDENTIFIED_TO_ANOTHER -> {
                            val crossPlacementInfo = syncCrossPlacementInfoOnProfileChange(profile.profileId)
                            cacheRepository.updateDataOnCreateProfile(profile, installationMeta, profileStateChange)
                            cacheRepository.saveCrossPlacementInfo(crossPlacementInfo)
                            ProfileIdChanged
                        }
                        ProfileStateChange.IDENTIFIED_TO_SELF -> {
                            cacheRepository.updateDataOnCreateProfile(profile, installationMeta, profileStateChange)
                            ProfileIdSame
                        }
                        ProfileStateChange.OUTDATED -> ProfileIdSame
                    }
                }
                .onEach { authSemaphore.release() }
                .catch { error -> authSemaphore.release(); throw error }
        }
    }

    private suspend fun syncCrossPlacementInfoOnProfileChange(newProfileId: String): CrossPlacementInfo {
        try {
            return cloudRepository.getCrossPlacementInfo(newProfileId).data
        } catch (error: Throwable) {
            delay(500L)
            return syncCrossPlacementInfoOnProfileChange(newProfileId)
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
    fun prepareAuthDataToSync(newCustomerUserId: String?, newObfuscatedAccountId: String?) {
        cacheRepository.prepareProfileIdToSync()
        cacheRepository.prepareIdentityParamsToSync(IdentityParams.from(newCustomerUserId, newObfuscatedAccountId))
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
    }
}