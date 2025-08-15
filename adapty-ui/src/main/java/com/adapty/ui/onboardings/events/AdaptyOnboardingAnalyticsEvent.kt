package com.adapty.ui.onboardings.events

import com.adapty.ui.onboardings.AdaptyOnboardingMetaParams

public sealed class AdaptyOnboardingAnalyticsEvent(public val meta: AdaptyOnboardingMetaParams) {
    public class Unknown(meta: AdaptyOnboardingMetaParams, public val name: String) :
        AdaptyOnboardingAnalyticsEvent(meta) {

        override fun toString(): String {
            return "Unknown(meta=$meta, name='$name')"
        }
    }

    public class OnboardingStarted(meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent(meta) {

        override fun toString(): String {
            return "OnboardingStarted(meta=$meta)"
        }
    }

    public class ScreenPresented(meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent(meta) {

        override fun toString(): String {
            return "ScreenPresented(meta=$meta)"
        }
    }

    public class ScreenCompleted(
        meta: AdaptyOnboardingMetaParams,
        public val elementId: String?,
        public val reply: String?
    ) : AdaptyOnboardingAnalyticsEvent(meta) {

        override fun toString(): String {
            return "ScreenCompleted(meta=$meta, elementId=$elementId, reply=$reply)"
        }
    }

    public class SecondScreenPresented(meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent(meta) {

        override fun toString(): String {
            return "SecondScreenPresented(meta=$meta)"
        }
    }

    public class RegistrationScreenPresented(meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent(meta) {

        override fun toString(): String {
            return "RegistrationScreenPresented(meta=$meta)"
        }
    }

    public class ProductsScreenPresented(meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent(meta) {

        override fun toString(): String {
            return "ProductsScreenPresented(meta=$meta)"
        }
    }

    public class UserEmailCollected(meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent(meta) {

        override fun toString(): String {
            return "UserEmailCollected(meta=$meta)"
        }
    }

    public class OnboardingCompleted(meta: AdaptyOnboardingMetaParams) :
        AdaptyOnboardingAnalyticsEvent(meta) {

        override fun toString(): String {
            return "OnboardingCompleted(meta=$meta)"
        }
    }
}
