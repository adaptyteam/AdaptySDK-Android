package com.adapty.internal.crossplatform.ui

import com.adapty.ui.onboardings.AdaptyOnboardingConfiguration

internal class OnboardingUiData(
    val config: AdaptyOnboardingConfiguration,
    val view: AdaptyUiOnboardingView,
) {

    operator fun component1() = config

    operator fun component2() = view
}
