package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.Onboarding
import com.adapty.models.AdaptyOnboarding

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class OnboardingMapper(
    private val placementMapper: PlacementMapper,
    private val remoteConfigMapper: RemoteConfigMapper,
) {

    fun map(onboarding: Onboarding, requestedLocale: String) = AdaptyOnboarding(
        id = onboarding.id,
        name = onboarding.name,
        variationId = onboarding.variationId,
        remoteConfig = onboarding.remoteConfig?.let(remoteConfigMapper::map),
        snapshotAt = onboarding.snapshotAt,
        requestedLocale = requestedLocale,
        viewConfig = onboarding.viewConfig,
        placement = placementMapper.map(onboarding.placement),
    )
}