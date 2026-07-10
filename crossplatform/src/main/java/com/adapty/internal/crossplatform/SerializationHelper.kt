package com.adapty.internal.crossplatform

import com.adapty.errors.AdaptyErrorCode
import com.adapty.models.AdaptyAttributionSource
import com.adapty.models.AdaptyConfig
import com.adapty.models.AdaptyEligibility
import com.adapty.models.AdaptyInstallationDetails
import com.adapty.models.AdaptyPeriodUnit
import com.adapty.models.AdaptyPlacementFetchPolicy
import com.adapty.models.AdaptyProductDiscountPhase
import com.adapty.models.AdaptyProductSubscriptionDetails
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyUI
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.ImmutableList
import com.adapty.utils.TimeInterval
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder

internal class SerializationHelper(
    private val transformFileLocation: FileLocationTransformer,
) {

    private val gson: Gson by lazy {
        val hasAdaptyUi = getClassForNameOrNull("com.adapty.ui.AdaptyUI") != null
        GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setFieldNamingStrategy(SerializationFieldNamingStrategy(if (hasAdaptyUi) SerializationFieldNamingStrategyUiHelper() else null))
            .addSerializationExclusionStrategy(SerializationExclusionStrategy())
            .registerTypeAdapterFactory(AdaptyPaywallProductTypeAdapterFactory())
            .registerTypeAdapterFactory(AdaptyProfileTypeAdapterFactory())
            .registerTypeAdapterFactory(AdaptyProductSubscriptionDetailsTypeAdapterFactory())
            .registerTypeAdapterFactory(AdaptyImmutableMapTypeAdapterFactory())
            .registerTypeAdapterFactory(AdaptyFlowPaywallTypeAdapterFactory())
            .registerTypeAdapterFactory(AdaptyPurchaseParametersTypeAdapterFactory())
            .registerTypeAdapterFactory(AdaptyResultTypeAdapterFactory())
            .registerTypeAdapterFactory(AdaptyPurchaseResultTypeAdapterFactory())
            .registerTypeAdapterFactory(AdaptyProductTypeTypeAdapterFactory())
            .registerTypeAdapterFactory(CrossplatformConfigTypeAdapterFactory(hasAdaptyUi))
            .registerTypeAdapterFactory(IdentifyArgsTypeAdapterFactory())
            .registerTypeAdapterFactory(SetIntegrationIdArgsTypeAdapterFactory())
            .registerTypeAdapterFactory(UpdateAttributionArgsTypeAdapterFactory())
            .registerTypeAdapterFactory(WebPurchaseArgsTypeAdapterFactory())
            .registerTypeAdapterFactory(AdaptyWebPresentationTypeAdapterFactory())
            .registerTypeAdapterFactory(AdaptyInstallationStatusTypeAdapterFactory())
            .registerTypeAdapterFactory(AdaptyCustomAssetTypeAdapterFactory(transformFileLocation))
            .registerTypeAdapter(
                FileLocationArgs::class.java,
                FileLocationArgsDeserializer()
            )
            .registerTypeAdapter(
                AdaptyAttributionSource::class.java,
                AdaptyAttributionSourceSerializer()
            )
            .registerTypeAdapter(
                AdaptyPeriodUnit::class.java,
                AdaptyPeriodUnitSerializer()
            )
            .registerTypeAdapter(
                AdaptyProductDiscountPhase.PaymentMode::class.java,
                AdaptyPaymentModeSerializer()
            )
            .registerTypeAdapter(
                AdaptyEligibility::class.java,
                AdaptyEligibilityAdapter()
            )
            .registerTypeAdapter(
                AdaptyProductSubscriptionDetails.RenewalType::class.java,
                AdaptyRenewalTypeAdapter()
            )
            .registerTypeAdapter(
                AdaptyPlacementFetchPolicy::class.java,
                AdaptyPlacementFetchPolicyDeserializer()
            )
            .registerTypeAdapter(
                AdaptySubscriptionUpdateParameters::class.java,
                AdaptySubscriptionUpdateParametersDeserializer()
            )
            .registerTypeAdapter(
                AdaptySubscriptionUpdateParameters.ReplacementMode::class.java,
                AdaptyReplacementModeDeserializer()
            )
            .registerTypeAdapter(
                TimeInterval::class.java,
                TimeIntervalDeserializer()
            )
            .registerTypeAdapter(
                AdaptyCustomAssets::class.java,
                AdaptyCustomAssetsDeserializer()
            )
            .registerTypeAdapter(
                AdaptyInstallationDetails.Payload::class.java,
                AdaptyInstallationDetailsPayloadSerializer()
            )
            .registerTypeAdapter(
                AdaptyConfig.ServerCluster::class.java,
                AdaptyConfigServerClusterDeserializer()
            )
            .registerTypeAdapter(
                ImmutableList::class.java,
                AdaptyImmutableListSerializer()
            )
            .registerTypeAdapter(
                AdaptyLogLevel::class.java,
                AdaptyLogLevelDeserializer()
            )
            .registerTypeAdapterFactory(AdaptyErrorTypeAdapterFactory())
            .registerTypeAdapter(
                AdaptyErrorCode::class.java,
                AdaptyErrorCodeSerializer()
            )
            .run {
                return@run try {
                    if (hasAdaptyUi) {
                        registerTypeAdapterFactory(AdaptyUIActionTypeAdapterFactory())
                            .registerTypeAdapterFactory(CreateFlowViewArgsTypeAdapterFactory())
                            .registerTypeAdapter(android.net.Uri::class.java, AndroidUriAdapter())
                            .registerTypeAdapter(
                                AdaptyUI.MediaCacheConfiguration::class.java,
                                AdaptyUIMediaCacheConfigurationDeserializer()
                            )
                    } else {
                        this
                    }
                } catch (t: Throwable) {
                    this
                }
            }
            .create()
    }

    fun <T> fromJson(json: String, type: Class<T>) =
        gson.fromJson(json, type)

    inline fun <reified T: Any> parseJsonArgument(argument: Any?): T? {
        return try {
            (argument as? String)?.takeIf(String::isNotEmpty)?.let { json ->
                fromJson(json, T::class.java)
            }
        } catch (e: Throwable) { null }
    }

    fun toJson(src: Any) = gson.toJson(src)
}