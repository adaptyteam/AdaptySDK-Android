package com.adapty.ui.onboardings

public class AdaptyOnboardingMetaParams(
    public val onboardingId: String,
    public val screenClientId: String,
    public val screenIndex: Int,
    public val totalScreens: Int,
) {

    public val isLastScreen: Boolean get() = totalScreens - screenIndex == 1

    override fun toString(): String {
        return "AdaptyOnboardingMetaParams(onboardingId='$onboardingId', screenClientId='$screenClientId', screenIndex=$screenIndex, totalScreens=$totalScreens)"
    }
}