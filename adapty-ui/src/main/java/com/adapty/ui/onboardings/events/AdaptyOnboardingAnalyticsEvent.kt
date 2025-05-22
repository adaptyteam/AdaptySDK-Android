package com.adapty.ui.onboardings.events

import com.adapty.ui.onboardings.AdaptyOnboardingMetaParams

public sealed class AdaptyOnboardingAnalyticsEvent {
    public class Unknown(public val meta: AdaptyOnboardingMetaParams, public val name: String) :
        AdaptyOnboardingAnalyticsEvent() {

        override fun toString(): String {
            return "Unknown(meta=$meta, name='$name')"
        }
    }

    public class OnboardingStarted(public val meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent() {

        override fun toString(): String {
            return "OnboardingStarted(meta=$meta)"
        }
    }

    public class ScreenPresented(public val meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent() {

        override fun toString(): String {
            return "ScreenPresented(meta=$meta)"
        }
    }

    public class ScreenCompleted(
        public val meta: AdaptyOnboardingMetaParams,
        public val elementId: String?,
        public val reply: String?
    ) : AdaptyOnboardingAnalyticsEvent() {

        override fun toString(): String {
            return "ScreenCompleted(meta=$meta, elementId=$elementId, reply=$reply)"
        }
    }

    public class SecondScreenPresented(public val meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent() {

        override fun toString(): String {
            return "SecondScreenPresented(meta=$meta)"
        }
    }

    public class RegistrationScreenPresented(public val meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent() {

        override fun toString(): String {
            return "RegistrationScreenPresented(meta=$meta)"
        }
    }

    public class ProductsScreenPresented(public val meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent() {

        override fun toString(): String {
            return "ProductsScreenPresented(meta=$meta)"
        }
    }

    public class UserEmailCollected(public val meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent() {

        override fun toString(): String {
            return "UserEmailCollected(meta=$meta)"
        }
    }

    public class OnboardingCompleted(public val meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent() {

        override fun toString(): String {
            return "OnboardingCompleted(meta=$meta)"
        }
    }
}
