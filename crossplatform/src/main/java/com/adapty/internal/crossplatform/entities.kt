package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyConfig
import com.adapty.models.AdaptyFlow
import com.adapty.models.AdaptyFlowPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPlacementFetchPolicy
import com.adapty.models.AdaptyProfileParameters
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.models.AdaptyWebPresentation
import com.adapty.ui.AdaptyUI.MediaCacheConfiguration
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.TimeInterval

class MethodArg(
    val method: String?
)

open class CrossplatformConfig(
    val baseConfig: AdaptyConfig,
    val logLevel: AdaptyLogLevel?,
    val crossPlatformSdkName: String?,
    val crossPlatformSdkVersion: String?,
)

class CrossplatformConfigWithUi(
    baseConfig: AdaptyConfig,
    logLevel: AdaptyLogLevel?,
    crossPlatformSdkName: String?,
    crossPlatformSdkVersion: String?,
    val mediaCache: MediaCacheConfiguration?,
): CrossplatformConfig(baseConfig, logLevel, crossPlatformSdkName, crossPlatformSdkVersion)

class ActivateArgs(val configuration: CrossplatformConfig)

class GetPlacementArgs(
    val placementId: String,
    val locale: String?,
    val fetchPolicy: AdaptyPlacementFetchPolicy?,
    val loadTimeout: TimeInterval?,
)

class GetPlacementForDefaultAudienceArgs(
    val placementId: String,
    val locale: String?,
    val fetchPolicy: AdaptyPlacementFetchPolicy?,
)

class GetPaywallProductsArgs(
    val flow: AdaptyFlow,
)

class IdentifyArgs(
    val customerUserId: String,
    val gpObfuscatedAccountId: String?,
)

class SetLogLevelArgs(
    val value: AdaptyLogLevel,
)

class LogShowOnboardingArgs(
    val params: Map<*, *>,
)

class LogShowFlowArgs(
    val flow: AdaptyFlow,
)

class MakePurchaseArgs(
    val product: AdaptyPaywallProduct,
    val parameters: AdaptyPurchaseParameters?,
)

class FileLocationArgs(
    val value: String,
)

class SetIntegrationIdArgs(
    val key: String,
    val value: String,
)

class SetVariationIdArgs(
    val variationId: String?,
    val transactionId: String,
)

sealed class WebPaywallArgs(val presentation: AdaptyWebPresentation?) {
    class Paywall(val value: AdaptyFlowPaywall, presentation: AdaptyWebPresentation?): WebPaywallArgs(presentation)
    class Product(val value: AdaptyPaywallProduct, presentation: AdaptyWebPresentation?): WebPaywallArgs(presentation)
}

class UpdateAttributionArgs(
    val attribution: Map<String, Any>,
    val source: String,
)

class UpdateProfileArgs(
    val params: AdaptyProfileParameters,
)

class FlowViewDidAnswerPermissionArgs(
    val eventId: String,
    val status: String,
    val detail: String?,
)

class ObserverModeRequestArgs(
    val eventId: String,
)

class OpenUrlArgs(
    val url: String,
    val openIn: AdaptyWebPresentation?,
)
