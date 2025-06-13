package com.adapty.ui.onboardings.listeners

import android.content.Context
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCloseAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCustomAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingLoadedAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingOpenPaywallAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingStateUpdatedAction
import com.adapty.ui.onboardings.errors.AdaptyOnboardingError
import com.adapty.ui.onboardings.events.AdaptyOnboardingAnalyticsEvent

public interface AdaptyOnboardingEventListener {

    public fun onFinishLoading(action: AdaptyOnboardingLoadedAction, context: Context)

    public fun onCloseAction(action: AdaptyOnboardingCloseAction, context: Context)

    public fun onOpenPaywallAction(action: AdaptyOnboardingOpenPaywallAction, context: Context)

    public fun onCustomAction(action: AdaptyOnboardingCustomAction, context: Context)

    public fun onStateUpdatedAction(action: AdaptyOnboardingStateUpdatedAction, context: Context)

    public fun onAnalyticsEvent(event: AdaptyOnboardingAnalyticsEvent, context: Context)

    public fun onError(error: AdaptyOnboardingError, context: Context)
}

