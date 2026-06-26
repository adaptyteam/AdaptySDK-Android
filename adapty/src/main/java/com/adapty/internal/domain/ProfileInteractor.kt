package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.Response
import com.adapty.internal.data.models.AttributionData
import com.adapty.internal.data.models.ProfileDto
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyIntegrationIdentifier
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyProfileParameters
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProfileInteractor(
    private val authInteractor: AuthInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val profileMapper: ProfileMapper,
    private val attributionHelper: AttributionHelper,
    private val customAttributeValidator: CustomAttributeValidator,
    private val iPv4Retriever: IPv4Retriever,
    private val offlineProfileManager: OfflineProfileManager,
    private val allowLocalPAL: Boolean,
) {

    fun getProfile(maxAttemptCount: Long = DEFAULT_RETRY_COUNT): Flow<AdaptyProfile> {
        val baseProfileFlow = authInteractor.runWhenAuthDataSynced(maxAttemptCount) {
            cloudRepository.getProfile()
        }
            .map { (profile, request) ->
                cacheRepository.updateOnProfileReceived(
                    profile,
                    request.currentDataWhenSent?.profileId,
                )
            }
            .catch { error ->
                if (error !is Response.Error || error.backendError == null || error.backendError.responseCode !in 400..406) {
                    val cachedProfile = cacheRepository.getProfile()
                    if (cachedProfile != null) {
                        emitAll(flowOf(cachedProfile))
                    } else {
                        throw error
                    }
                } else {
                    throw error
                }
            }

        return if (allowLocalPAL) {
            baseProfileFlow
                .zip(offlineProfileManager.getLocalPAL()) { profile, localPALData ->
                    profileMapper.map(profile, localPALData)
                }
        } else {
            baseProfileFlow
                .map { profileMapper.map(it) }
        }
    }

    fun updateProfile(params: AdaptyProfileParameters?, maxAttemptCount: Long = DEFAULT_RETRY_COUNT) =
        validateCustomAttributes(params?.customAttributes?.map)
            .flatMapConcat { authInteractor.createInstallationMeta(false) }
            .flatMapConcat { installationMeta ->
                val metaHasChanged = installationMeta.hasChanged(cacheRepository.getInstallationMeta())
                val metaToBeSent = installationMeta.takeIf { metaHasChanged }

                var ip: String? = null
                authInteractor.runWhenAuthDataSynced(maxAttemptCount) {
                    ip = (if (!iPv4Retriever.disabled) {
                        iPv4Retriever.value
                            .also { value ->
                                if (value == null)
                                    sendIpWhenReceived()
                            }
                    } else null)
                        ?.takeIf { it != cacheRepository.getLastSentIp() }
                    if (ip == null && params == null && metaToBeSent == null)
                        throw NothingToUpdateException()
                    cloudRepository.updateProfile(params, metaToBeSent, ip)
                }
                    .map { (profile, request) ->
                        cacheRepository.updateOnProfileReceived(
                            profile,
                            request.currentDataWhenSent?.profileId,
                        )
                        metaToBeSent?.let(cacheRepository::saveLastSentInstallationMeta)
                        ip?.let { ip ->
                            cacheRepository.saveLastSentIp(ip, request.currentDataWhenSent?.profileId)
                        }
                        Unit
                    }
                    .catch { e ->
                        if (e is NothingToUpdateException)
                            emit(Unit)
                        else
                            throw e
                    }
            }
            .also {
                params?.analyticsDisabled?.not()?.let(cacheRepository::saveExternalAnalyticsEnabled)
            }

    private fun validateCustomAttributes(attrs: Map<String, Any?>?) =
        flow {
            attrs?.let(customAttributeValidator::validate)
            emit(Unit)
        }

    fun getProfileOnStart() =
        getProfile(INFINITE_RETRY)

    fun syncMetaOnStart() =
        updateProfile(params = null, INFINITE_RETRY)

    fun updateAttribution(attribution: Map<String, Any>, source: String) =
        updateAttribution {
            attributionHelper.createAttributionData(attribution, source, cacheRepository.getProfileId())
        }

    fun updateAttribution(attributionJson: String, source: String) =
        updateAttribution {
            attributionHelper.createAttributionData(attributionJson, source, cacheRepository.getProfileId())
        }

    private fun updateAttribution(attributionData: () -> AttributionData) =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.updateAttribution(attributionData())
        }
            .map { (profile, request) ->
                cacheRepository.updateOnProfileReceived(
                    profile,
                    request.currentDataWhenSent?.profileId,
                )
                Unit
            }

    fun setIntegrationIdentifiers(identifiers: List<AdaptyIntegrationIdentifier>) =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.setIntegrationIdentifiers(identifiers.toKeyValueMap())
        }

    fun syncCrossPlacementInfo(replacementProfileId: String? = null) =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.getCrossPlacementInfo(replacementProfileId).data
        }
            .map { crossPlacementInfo ->
                cacheRepository.saveCrossPlacementInfo(crossPlacementInfo)
            }

    fun subscribeOnProfileChanges() =
        if (allowLocalPAL) {
            cacheRepository.subscribeOnProfileChanges()
                .zip(offlineProfileManager.getLocalPAL()) { profile, localPALData ->
                    profileMapper.map(profile, localPALData)
                }
        } else {
            cacheRepository.subscribeOnProfileChanges()
                .map { profileMapper.map(it) }
        }
            .distinctUntilChanged()

    fun subscribeOnEventsForStartRequests() =
        cacheRepository.subscribeOnProfileChanges()
            .distinctUntilChanged()
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

    private fun sendIpWhenReceived() {
        iPv4Retriever.onValueReceived = { value ->
            execute {
                if (value == cacheRepository.getLastSentIp())
                    return@execute
                flow {
                    emit(cloudRepository.updateProfile(ipv4Address = value))
                }
                    .retryIfNecessary(INFINITE_RETRY)
                    .map { (_, request) ->
                        cacheRepository.saveLastSentIp(value, request.currentDataWhenSent?.profileId)
                    }
                    .catch { }.collect()
            }
        }
    }

    private class NothingToUpdateException : Exception()
}