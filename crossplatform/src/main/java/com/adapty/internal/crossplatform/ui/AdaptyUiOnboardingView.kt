package com.adapty.internal.crossplatform.ui

import com.adapty.models.AdaptyOnboarding
import java.util.UUID

class AdaptyUiOnboardingView(
    val id: String,
    val placementId: String,
    val variationId: String,
) {
    constructor(onboarding: AdaptyOnboarding, id: String = UUID.randomUUID().toString()): this(
        id = id,
        placementId = onboarding.placement.id,
        variationId = onboarding.variationId
    )
}