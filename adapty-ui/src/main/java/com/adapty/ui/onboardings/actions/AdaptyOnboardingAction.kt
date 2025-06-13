package com.adapty.ui.onboardings.actions

import com.adapty.ui.onboardings.AdaptyOnboardingMetaParams

public sealed class AdaptyOnboardingAction(public val meta: AdaptyOnboardingMetaParams)

public class AdaptyOnboardingCloseAction(
    public val actionId: String,
    meta: AdaptyOnboardingMetaParams,
): AdaptyOnboardingAction(meta) {

    override fun toString(): String {
        return "AdaptyOnboardingCloseAction(actionId='$actionId', meta=$meta)"
    }
}

public class AdaptyOnboardingCustomAction(
    public val actionId: String,
    meta: AdaptyOnboardingMetaParams,
): AdaptyOnboardingAction(meta) {

    override fun toString(): String {
        return "AdaptyOnboardingCustomAction(actionId='$actionId', meta=$meta)"
    }
}

public class AdaptyOnboardingOpenPaywallAction(
    public val actionId: String,
    meta: AdaptyOnboardingMetaParams,
): AdaptyOnboardingAction(meta) {

    override fun toString(): String {
        return "AdaptyOnboardingOpenPaywallAction(actionId='$actionId', meta=$meta)"
    }
}

public class AdaptyOnboardingStateUpdatedAction(
    public val elementId: String,
    meta: AdaptyOnboardingMetaParams,
    public val params: AdaptyOnboardingStateUpdatedParams,
): AdaptyOnboardingAction(meta) {

    override fun toString(): String {
        return "AdaptyOnboardingStateUpdatedAction(elementId='$elementId', meta=$meta, params=$params)"
    }
}

public class AdaptyOnboardingLoadedAction(
    meta: AdaptyOnboardingMetaParams,
): AdaptyOnboardingAction(meta) {

    override fun toString(): String {
        return "AdaptyOnboardingLoadedAction(meta=$meta)"
    }
}
