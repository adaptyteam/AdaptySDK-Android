package com.adapty.internal.utils

import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cache.PROFILE_ID
import com.adapty.internal.data.cache.PreferenceManager
import com.adapty.internal.data.models.ProfileDto
import com.adapty.internal.utils.ProfileStateChange.IDENTIFIED_TO_ANOTHER
import com.adapty.internal.utils.ProfileStateChange.IDENTIFIED_TO_SELF
import com.adapty.internal.utils.ProfileStateChange.NEW
import com.adapty.internal.utils.ProfileStateChange.OUTDATED

internal class ProfileStateChangeChecker(
    private val cacheRepository: CacheRepository,
    private val preferenceManager: PreferenceManager,
) {

    fun getProfileStateChange(
        newProfile: ProfileDto,
        requestedProfileId: String?,
    ): ProfileStateChange =
        resolveProfileStateChange(
            newProfileId = newProfile.profileId,
            newProfileTimestamp = newProfile.timestamp.orDefault(),
            cachedProfileTimestamp = cacheRepository.getProfile()?.timestamp.orDefault(),
            persistedProfileId = preferenceManager.getString(PROFILE_ID),
            requestedProfileId = requestedProfileId,
        )
}

internal fun resolveProfileStateChange(
    newProfileId: String,
    newProfileTimestamp: Long,
    cachedProfileTimestamp: Long,
    persistedProfileId: String?,
    requestedProfileId: String?,
): ProfileStateChange {
    if (newProfileTimestamp < cachedProfileTimestamp)
        return OUTDATED
    if (persistedProfileId == null)
        return if (newProfileId == requestedProfileId) NEW else IDENTIFIED_TO_ANOTHER
    if (newProfileId != persistedProfileId)
        return IDENTIFIED_TO_ANOTHER
    return IDENTIFIED_TO_SELF
}

internal enum class ProfileStateChange {
    NEW, IDENTIFIED_TO_SELF, IDENTIFIED_TO_ANOTHER, OUTDATED
}
