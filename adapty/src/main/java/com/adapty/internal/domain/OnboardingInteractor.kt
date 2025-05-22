package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.utils.OnboardingMapper
import com.adapty.models.AdaptyOnboarding
import com.adapty.models.AdaptyPlacementFetchPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class OnboardingInteractor(
    private val onboardingFetcher: BasePlacementFetcher,
    private val onboardingMapper: OnboardingMapper,
    private val analyticsTracker: AnalyticsTracker,
    private val cacheRepository: CacheRepository,
) {

    fun getOnboarding(placementId: String, locale: String, fetchPolicy: AdaptyPlacementFetchPolicy, loadTimeout: Int): Flow<AdaptyOnboarding> {
        return onboardingFetcher.fetchOnboarding(placementId, locale, fetchPolicy, loadTimeout)
            .map { onboarding -> onboardingMapper.map(onboarding) }
    }

    fun getOnboardingUntargeted(placementId: String, locale: String, fetchPolicy: AdaptyPlacementFetchPolicy): Flow<AdaptyOnboarding> {
        return onboardingFetcher.fetchOnboardingUntargeted(placementId, locale, fetchPolicy)
            .map { onboarding -> onboardingMapper.map(onboarding) }
    }

    fun logShowOnboardingInternal(
        onboarding: AdaptyOnboarding,
        screenName: String?,
        screenOrder: Int,
        isLastScreen: Boolean,
    ) {
        analyticsTracker.trackEvent(
            "new_onboarding_screen_showed",
            hashMapOf<String, Any>(
                "variation_id" to onboarding.variationId,
                "onboarding_screen_order" to screenOrder,
                "onboarding_latest_screen" to isLastScreen,
            )
                .apply {
                    screenName?.let { put("onboarding_screen_name", screenName) }
                },
        )
        if (screenOrder == 0 && onboarding.placement.isTrackingPurchases)
            cacheRepository.saveOnboardingVariationId(onboarding.variationId)
    }
}
