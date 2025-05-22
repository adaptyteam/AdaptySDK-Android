package com.adapty.ui.onboardings.listeners

import android.content.Context
import com.adapty.ui.internal.utils.getActivityOrNull
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCloseAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCustomAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingOpenPaywallAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingStateUpdatedAction
import com.adapty.ui.onboardings.errors.AdaptyOnboardingError
import com.adapty.ui.onboardings.events.AdaptyOnboardingAnalyticsEvent

public open class AdaptyOnboardingDefaultEventListener: AdaptyOnboardingEventListener {
    override fun onFinishLoading(context: Context) { }

    override fun onCloseAction(action: AdaptyOnboardingCloseAction, context: Context) {
        context?.getActivityOrNull()?.onBackPressed()
    }

    override fun onOpenPaywallAction(action: AdaptyOnboardingOpenPaywallAction, context: Context) { }

    override fun onCustomAction(action: AdaptyOnboardingCustomAction, context: Context) { }

    override fun onStateUpdatedAction(action: AdaptyOnboardingStateUpdatedAction, context: Context) { }

    override fun onAnalyticsEvent(event: AdaptyOnboardingAnalyticsEvent, context: Context) { }

    override fun onError(error: AdaptyOnboardingError, context: Context) { }
}