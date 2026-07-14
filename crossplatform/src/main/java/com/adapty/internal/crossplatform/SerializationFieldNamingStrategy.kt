@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform

import com.adapty.internal.data.models.OnboardingBuilder
import com.adapty.internal.domain.models.BackendProduct
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyFlow
import com.adapty.models.AdaptyFlowPaywall
import com.adapty.models.AdaptyInstallationDetails
import com.adapty.models.AdaptyOnboarding
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPlacement
import com.adapty.models.AdaptyProductSubscriptionDetails
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyRemoteConfig
import com.google.gson.FieldNamingPolicy
import com.google.gson.FieldNamingStrategy
import java.lang.reflect.Field

internal class SerializationFieldNamingStrategy(
    private val uiHelper: SerializationFieldNamingStrategyUiHelper?,
) : FieldNamingStrategy {

    override fun translateName(f: Field?): String {
        return uiHelper?.translateNameOrSkip(f) ?: translateNameInternal(f)
    }

    private fun translateNameInternal(f: Field?): String {
        return when (f?.declaringClass) {
            AdaptyFlow::class.java -> when (f.name) {
                "snapshotAt" -> "response_created_at"
                "viewConfigurationId" -> "flow_version_id"
                "paywalls" -> "variations"
                "id" -> "flow_id"
                "name" -> "flow_name"
                else -> translateDefault(f)
            }
            AdaptyFlowPaywall::class.java -> when (f.name) {
                "webPurchaseUrl" -> "web_purchase_url"
                "id" -> "paywall_id"
                "name" -> "paywall_name"
                else -> translateDefault(f)
            }
            AdaptyPlacement::class.java -> when (f.name) {
                "id" -> "developer_id"
                "audienceVersionId" -> "placement_audience_version_id"
                else -> translateDefault(f)
            }
            AdaptyOnboarding::class.java -> when (f.name) {
                "snapshotAt" -> "response_created_at"
                "viewConfig" -> "onboarding_builder"
                "requestedLocale" -> "request_locale"
                "id" -> "onboarding_id"
                "name" -> "onboarding_name"
                else -> translateDefault(f)
            }
            OnboardingBuilder::class.java -> when (f.name) {
                "url" -> "config_url"
                else -> translateDefault(f)
            }
            AdaptyRemoteConfig::class.java -> when (f.name) {
                "jsonString" -> "data"
                "locale" -> "lang"
                else -> translateDefault(f)
            }
            AdaptyPaywallProduct::class.java -> when (f.name) {
                "paywallABTestName" -> "paywall_ab_test_name"
                "variationId" -> "paywall_variation_id"
                "subscriptionDetails" -> "subscription"
                "webPurchaseUrl" -> "web_purchase_url"
                else -> translateDefault(f)
            }
            AdaptyPaywallProduct.Price::class.java -> when (f.name) {
                "localizedPrice" -> "localized_string"
                else -> translateDefault(f)
            }
            AdaptyProductSubscriptionDetails::class.java -> when (f.name) {
                "subscriptionPeriod" -> "period"
                "localizedSubscriptionPeriod" -> "localized_period"
                else -> translateDefault(f)
            }
            AdaptyProfile::class.java -> when (f.name) {
                "accessLevels" -> "paid_access_levels"
                else -> translateDefault(f)
            }
            AdaptyInstallationDetails::class.java -> when (f.name) {
                "id" -> "install_id"
                "installedAt" -> "install_time"
                else -> translateDefault(f)
            }
            BackendProduct::class.java -> when (f.name) {
                "id" -> "adapty_product_id"
                "declaredProductType" -> "product_type"
                else -> translateDefault(f)
            }
            else -> translateDefault(f)
        }
    }

    private fun translateDefault(f: Field?) =
        FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(f)
}