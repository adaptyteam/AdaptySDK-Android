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
    ): ProfileStateChange {
        if (newProfile.timestamp.orDefault() < cacheRepository.getProfile()?.timestamp.orDefault())
            return OUTDATED
        val persistedProfileId = preferenceManager.getString(PROFILE_ID)
            ?: return NEW
        if (newProfile.profileId != persistedProfileId)
            return IDENTIFIED_TO_ANOTHER
        return IDENTIFIED_TO_SELF
    }
}

internal enum class ProfileStateChange {
    NEW, IDENTIFIED_TO_SELF, IDENTIFIED_TO_ANOTHER, OUTDATED
}