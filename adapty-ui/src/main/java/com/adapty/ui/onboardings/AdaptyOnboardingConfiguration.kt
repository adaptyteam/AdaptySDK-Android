package com.adapty.ui.onboardings

import com.adapty.models.AdaptyOnboarding

public class AdaptyOnboardingConfiguration internal constructor(
    @get:JvmSynthetic internal val onboarding: AdaptyOnboarding,
) {
    @get:JvmSynthetic
    internal val variationId: String get() = onboarding.variationId
    @get:JvmSynthetic
    @Suppress("INVISIBLE_MEMBER")
    internal val url: String get() = onboarding.viewConfig.url
    @get:JvmSynthetic
    @Suppress("INVISIBLE_MEMBER")
    internal val isTrackingPurchases: Boolean get() = onboarding.placement.isTrackingPurchases
}