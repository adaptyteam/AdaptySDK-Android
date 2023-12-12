package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.models.ProfileDto
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyAttributionSource
import com.adapty.models.AdaptyProfileParameters
import kotlinx.coroutines.flow.*
import java.io.IOException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProfileInteractor(
    private val authInteractor: AuthInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val profileMapper: ProfileMapper,
    private val attributionHelper: AttributionHelper,
    private val customAttributeValidator: CustomAttributeValidator,
) {

    @JvmSynthetic
    fun getProfile(maxAttemptCount: Long = DEFAULT_RETRY_COUNT) =
        authInteractor.runWhenAuthDataSynced(maxAttemptCount) {
            cloudRepository.getProfile()
        }
            .map { (profile, currentDataWhenRequestSent) ->
                cacheRepository.updateOnProfileReceived(
                    profile,
                    currentDataWhenRequestSent?.profileId,
                ).let(profileMapper::map)
            }
            .catch { error ->
                if (error is AdaptyError && (error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR || error.originalError is IOException)) {
                    val cachedProfile = cacheRepository.getProfile()?.let(profileMapper::map)
                    if (cachedProfile != null) {
                        emitAll(flowOf(cachedProfile))
                    } else {
                        throw error
                    }
                } else {
                    throw error
                }
            }
            .flowOnIO()

    @JvmSynthetic
    fun updateProfile(params: AdaptyProfileParameters?, maxAttemptCount: Long = DEFAULT_RETRY_COUNT) =
        validateCustomAttributes(params?.customAttributes?.map)
            .flatMapConcat { authInteractor.createInstallationMeta(false) }
            .flatMapConcat { installationMeta ->
                val metaHasChanged = installationMeta.hasChanged(cacheRepository.getInstallationMeta())
                val metaToBeSent = installationMeta.takeIf { metaHasChanged }

                if (params != null || metaToBeSent != null) {
                    authInteractor.runWhenAuthDataSynced(maxAttemptCount) {
                        cloudRepository.updateProfile(params, metaToBeSent)
                    }
                        .map { (profile, currentDataWhenRequestSent) ->
                            cacheRepository.updateOnProfileReceived(
                                profile,
                                currentDataWhenRequestSent?.profileId,
                            )
                            metaToBeSent?.let(cacheRepository::saveLastSentInstallationMeta)
                            Unit
                        }
                } else {
                    flowOf(Unit)
                }
            }
            .flowOnIO()
            .also {
                params?.analyticsDisabled?.not()?.let(cacheRepository::saveExternalAnalyticsEnabled)
            }

    private fun validateCustomAttributes(attrs: Map<String, Any?>?) =
        flow {
            attrs?.let(customAttributeValidator::validate)
            emit(Unit)
        }

    @JvmSynthetic
    fun getProfileOnStart() =
        getProfile(INFINITE_RETRY)

    @JvmSynthetic
    fun getAnalyticsCredsOnStart() =
        getAnalyticsCreds(INFINITE_RETRY)

    @JvmSynthetic
    fun getAnalyticsCreds(maxAttemptCount: Long) =
        cloudRepository.onActivateAllowed()
            .mapLatest { cloudRepository.getAnalyticsCreds() }
            .retryIfNecessary(maxAttemptCount)
            .map(cacheRepository::updateAnalyticsCreds)
            .flowOnIO()

    @JvmSynthetic
    fun syncMetaOnStart() =
        updateProfile(params = null, INFINITE_RETRY)

    @JvmSynthetic
    fun updateAttribution(attribution: Any, source: AdaptyAttributionSource, networkUserId: String?) =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.updateAttribution(
                attributionHelper.createAttributionData(attribution, source, networkUserId)
            )
        }.flowOnIO()

    @JvmSynthetic
    fun subscribeOnProfileChanges() =
        cacheRepository.subscribeOnProfileChanges()
            .map(profileMapper::map)

    @JvmSynthetic
    fun subscribeOnEventsForStartRequests() =
        cacheRepository.subscribeOnProfileChanges()
            .onStart {
                cacheRepository.getProfile()?.let { cachedProfile -> emit(cachedProfile) }
            }
            .scan(Pair<ProfileDto?, ProfileDto?>(null, null)) { (_, prevProfile), currentProfile ->
                prevProfile to currentProfile
            }
            .map { (prevProfile, currentProfile) ->
                val profileIdHasChanged = prevProfile?.profileId.orEmpty() != currentProfile?.profileId.orEmpty()
                val customerUserIdHasChanged = prevProfile?.customerUserId.orEmpty() != currentProfile?.customerUserId.orEmpty()
                profileIdHasChanged to customerUserIdHasChanged
            }
            .filter { (profileIdHasChanged, customerUserIdHasChanged) ->
                profileIdHasChanged || customerUserIdHasChanged
            }
            .flowOnIO()
}