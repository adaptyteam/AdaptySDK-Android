@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.onboardings

import com.adapty.models.AdaptyOnboarding
import com.adapty.models.AdaptyWebPresentation

public class AdaptyOnboardingConfiguration internal constructor(
    @get:JvmSynthetic internal val onboarding: AdaptyOnboarding,
    @get:JvmSynthetic internal val externalUrlsPresentation: AdaptyWebPresentation,
) {
    @get:JvmSynthetic
    internal val variationId: String get() = onboarding.variationId
    @get:JvmSynthetic
    internal val url: String get() = onboarding.viewConfig.url
    @get:JvmSynthetic
    internal val requestedLocale: String? get() = onboarding.requestedLocale
    @get:JvmSynthetic
    internal val isTrackingPurchases: Boolean get() = onboarding.placement.isTrackingPurchases
}