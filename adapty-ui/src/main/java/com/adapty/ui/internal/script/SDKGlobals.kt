@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.script

import android.os.Build
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.MetaInfoRetriever
import com.adapty.models.AdaptyFlow
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProductDiscountPhase
import com.adapty.models.AdaptyProductDiscountPhase.PaymentMode
import com.adapty.models.AdaptyProductSubscriptionDetails
import com.adapty.ui.internal.store.resolveProductKey
import com.adapty.ui.internal.text.BILLING_PLAN_UP_FRONT
import com.adapty.ui.internal.utils.CONFIGURATION_FORMAT_VERSION
import com.adapty.ui.internal.utils.FlowMode
import java.util.Locale

internal object SDKGlobals {

    fun buildSDKEnvJson(
        metaInfoRetriever: MetaInfoRetriever,
        context: android.content.Context,
        mode: FlowMode,
        locale: Locale,
        localizationId: String? = null,
        isRtl: Boolean = false,
    ): String {
        val (appBuild, appVersion) = metaInfoRetriever.appBuildAndVersion

        val flow = (mode as? FlowMode.Live)?.flow
        val placement = flow?.placement

        val appCurrentLocale = metaInfoRetriever.currentLocale?.let { loc ->
            if (loc.country.isNullOrEmpty()) loc.language else "${loc.language}-${loc.country}"
        }
        val userLocales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = android.os.LocaleList.getDefault()
            (0 until localeList.size()).map { i ->
                val loc = localeList[i]
                if (loc.country.isNullOrEmpty()) loc.language else "${loc.language}-${loc.country}"
            }
        } else {
            listOf(appCurrentLocale ?: Locale.getDefault().language)
        }
        val uses24Hour = android.text.format.DateFormat.is24HourFormat(context)

        val pairs = mutableListOf<Pair<String, Any?>>()
        pairs.add("platform" to "android")
        pairs.add("schemaVersion" to CONFIGURATION_FORMAT_VERSION)
        pairs.add("localizationId" to localizationId)
        pairs.add("localizationDirection" to if (isRtl) "rtl" else "ltr")
        pairs.add("sdkVersion" to metaInfoRetriever.adaptySdkVersion)
        pairs.add("osName" to "Android")
        pairs.add("osVersion" to metaInfoRetriever.os)
        pairs.add("deviceVendor" to Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString()
        })
        pairs.add("deviceModel" to metaInfoRetriever.deviceName)
        pairs.add("appBundleId" to metaInfoRetriever.applicationId)
        pairs.add("appVersion" to appVersion)
        pairs.add("appBuild" to appBuild)
        pairs.add("appCurrentLocale" to appCurrentLocale)
        pairs.add("userLocales" to userLocales)
        pairs.add("userUses24HourClock" to uses24Hour)
        pairs.add("placementId" to placement?.id)
        pairs.add("placementVariationId" to flow?.variationId)
        pairs.add("placementVariationName" to flow?.variationName)
        pairs.add("placementName" to flow?.name)
        pairs.add("placementABTestName" to placement?.abTestName)

        return buildJsonObject(pairs)
    }

    fun buildPreviewSDKEnvJson(
        metaInfoRetriever: MetaInfoRetriever,
        locale: Locale,
        localizationId: String? = null,
        isRtl: Boolean = false,
    ): String {
        val (appBuild, appVersion) = metaInfoRetriever.appBuildAndVersion

        val appCurrentLocale = metaInfoRetriever.currentLocale?.let { loc ->
            if (loc.country.isNullOrEmpty()) loc.language else "${loc.language}-${loc.country}"
        }
        val userLocales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = android.os.LocaleList.getDefault()
            (0 until localeList.size()).map { i ->
                val loc = localeList[i]
                if (loc.country.isNullOrEmpty()) loc.language else "${loc.language}-${loc.country}"
            }
        } else {
            listOf(appCurrentLocale ?: Locale.getDefault().language)
        }

        val pairs = mutableListOf<Pair<String, Any?>>()
        pairs.add("platform" to "android")
        pairs.add("schemaVersion" to CONFIGURATION_FORMAT_VERSION)
        pairs.add("localizationId" to localizationId)
        pairs.add("localizationDirection" to if (isRtl) "rtl" else "ltr")
        pairs.add("sdkVersion" to metaInfoRetriever.adaptySdkVersion)
        pairs.add("osName" to "Android")
        pairs.add("osVersion" to metaInfoRetriever.os)
        pairs.add("deviceVendor" to Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString()
        })
        pairs.add("deviceModel" to metaInfoRetriever.deviceName)
        pairs.add("appBundleId" to metaInfoRetriever.applicationId)
        pairs.add("appVersion" to appVersion)
        pairs.add("appBuild" to appBuild)
        pairs.add("appCurrentLocale" to appCurrentLocale)
        pairs.add("userLocales" to userLocales)
        pairs.add("userUses24HourClock" to false)
        pairs.add("placementId" to "preview_placement")
        pairs.add("placementVariationId" to "preview_variation")
        pairs.add("placementVariationName" to "preview_variation_name")
        pairs.add("placementName" to "preview")
        pairs.add("placementABTestName" to "preview_abtest")

        return buildJsonObject(pairs)
    }

    fun buildSDKProductsJson(
        products: List<AdaptyPaywallProduct>,
    ): String = buildSDKProductsJson(
        products.associateBy { resolveProductKey(it) }
    )

    fun buildSDKProductsJson(
        products: Map<String, AdaptyPaywallProduct>,
    ): String {
        if (products.isEmpty()) return "{}"

        val sb = StringBuilder("{")
        var first = true
        for ((key, product) in products) {
            if (!first) sb.append(',')
            first = false
            sb.append(escapeJsonString(key))
            sb.append(':')
            sb.append(buildSingleProductJson(product))
        }
        sb.append('}')
        return sb.toString()
    }

    fun buildStaticSDKProductsJson(flow: AdaptyFlow): String {
        val sb = StringBuilder("{")
        var first = true
        for (paywall in flow.paywalls) {
            for (product in paywall.products) {
                val key = resolveProductKey(product)
                if (!first) sb.append(',')
                first = false
                sb.append(escapeJsonString(key))
                sb.append(':')
                val pairs = mutableListOf<Pair<String, Any?>>()
                pairs.add("flowProductId" to product.flowProductId)
                pairs.add("adaptyProductId" to product.id)
                pairs.add("adaptyAccessLevelId" to product.accessLevelId)
                pairs.add("adaptyProductType" to product.declaredProductType)
                pairs.add("paywallVariationId" to paywall.variationId)
                pairs.add("paywallName" to paywall.name)
                sb.append(buildJsonObject(pairs))
            }
        }
        sb.append('}')
        return sb.toString()
    }

    private fun buildSingleProductJson(
        product: AdaptyPaywallProduct,
    ): String {
        val pairs = mutableListOf<Pair<String, Any?>>()
        pairs.add("flowProductId" to product.payloadData.flowProductId)
        pairs.add("adaptyProductId" to product.payloadData.adaptyProductId)
        pairs.add("adaptyAccessLevelId" to product.accessLevelId)
        pairs.add("adaptyProductType" to product.productType)
        pairs.add("paywallVariationId" to product.variationId)
        pairs.add("paywallName" to product.paywallName)
        pairs.add("localizedTitle" to product.localizedTitle)
        pairs.add("localizedDescription" to product.localizedDescription)

        val pricePairs = mutableListOf<Pair<String, Any?>>()
        pricePairs.add("amount" to product.price.amount.toDouble())
        pricePairs.add("currencyCode" to product.price.currencyCode)
        pricePairs.add("currencySymbol" to product.price.currencySymbol)
        pricePairs.add("localizedString" to product.price.localizedString)
        pairs.add("price" to RawJson(buildJsonObject(pricePairs)))

        val subscriptionJson = product.subscriptionDetails?.let { sub ->
            val periodPairs = mutableListOf<Pair<String, Any?>>()
            periodPairs.add("unit" to sub.subscriptionPeriod.unit.name.lowercase())
            periodPairs.add("numberOfUnits" to sub.subscriptionPeriod.numberOfUnits)

            val subPairs = mutableListOf<Pair<String, Any?>>()
            subPairs.add("groupIdentifier" to sub.basePlanId)
            subPairs.add("period" to RawJson(buildJsonObject(periodPairs)))
            subPairs.add("localizedPeriod" to sub.localizedSubscriptionPeriod)

            val commitmentPairs = mutableListOf<Pair<String, Any?>>()
            commitmentPairs.add("price" to RawJson(buildJsonObject(pricePairs)))
            commitmentPairs.add("period" to RawJson(buildJsonObject(periodPairs)))

            val pricingTermsPairs = mutableListOf<Pair<String, Any?>>()
            pricingTermsPairs.add("billingPlanId" to BILLING_PLAN_UP_FRONT)
            pricingTermsPairs.add("billingPrice" to RawJson(buildJsonObject(pricePairs)))
            pricingTermsPairs.add("billingPeriod" to RawJson(buildJsonObject(periodPairs)))
            pricingTermsPairs.add("commitmentInfo" to RawJson(buildJsonObject(commitmentPairs)))
            subPairs.add("pricingTerms" to RawJson(buildJsonObject(pricingTermsPairs)))

            subPairs.add("offer" to buildOfferJson(sub)?.let(::RawJson))

            RawJson(buildJsonObject(subPairs))
        }
        pairs.add("subscription" to subscriptionJson)

        return buildJsonObject(pairs)
    }

    /**
     * `SDKProducts[*].subscription.offer`, or null when the product has no offer.
     *
     * `phases` lists every discount phase of the Google Play offer in Play order (for example a
     * free trial, then an intro price); the base plan is not a phase. An offer without discount
     * phases has no `phases` key.
     */
    internal fun buildOfferJson(sub: AdaptyProductSubscriptionDetails): String? {
        val offerId = sub.offerId ?: return null
        val offerPairs = mutableListOf<Pair<String, Any?>>()
        offerPairs.add("id" to offerId)
        offerPairs.add("type" to "introductory")
        val phases = sub.introductoryOfferPhases.map { RawJson(buildOfferPhaseJson(it)) }
        if (phases.isNotEmpty()) {
            offerPairs.add("phases" to phases)
        }
        return buildJsonObject(offerPairs)
    }

    private fun buildOfferPhaseJson(phase: AdaptyProductDiscountPhase): String {
        val pricePairs = mutableListOf<Pair<String, Any?>>()
        pricePairs.add("amount" to phase.price.amount.toDouble())
        pricePairs.add("currencyCode" to phase.price.currencyCode)
        pricePairs.add("currencySymbol" to phase.price.currencySymbol)
        pricePairs.add("localizedString" to phase.price.localizedString)

        val periodPairs = mutableListOf<Pair<String, Any?>>()
        periodPairs.add("unit" to phase.subscriptionPeriod.unit.name.lowercase())
        periodPairs.add("numberOfUnits" to phase.subscriptionPeriod.numberOfUnits)

        val phasePairs = mutableListOf<Pair<String, Any?>>()
        phasePairs.add("price" to RawJson(buildJsonObject(pricePairs)))
        phasePairs.add("paymentMode" to phase.paymentMode.toJsonValue())
        phasePairs.add("period" to RawJson(buildJsonObject(periodPairs)))
        phasePairs.add("numberOfPeriods" to phase.numberOfPeriods)
        phasePairs.add("localizedPeriod" to phase.localizedSubscriptionPeriod)
        phasePairs.add("localizedNumberOfPeriods" to phase.localizedNumberOfPeriods)
        return buildJsonObject(phasePairs)
    }

    private class RawJson(val json: String)

    private fun buildJsonObject(pairs: List<Pair<String, Any?>>): String {
        val sb = StringBuilder("{")
        var first = true
        for ((k, v) in pairs) {
            if (!first) sb.append(',')
            first = false
            sb.append(escapeJsonString(k))
            sb.append(':')
            sb.append(toJsonValue(v))
        }
        sb.append('}')
        return sb.toString()
    }

    private fun toJsonValue(value: Any?): String = when (value) {
        null -> "null"
        is Boolean -> value.toString()
        is Number -> value.toString()
        is String -> escapeJsonString(value)
        is RawJson -> value.json
        is List<*> -> {
            val sb = StringBuilder("[")
            var first = true
            for (item in value) {
                if (!first) sb.append(',')
                first = false
                sb.append(toJsonValue(item))
            }
            sb.append(']')
            sb.toString()
        }
        else -> escapeJsonString(value.toString())
    }

    private fun PaymentMode.toJsonValue(): String = when (this) {
        PaymentMode.FREE_TRIAL -> "free_trial"
        PaymentMode.PAY_AS_YOU_GO -> "pay_as_you_go"
        PaymentMode.PAY_UPFRONT -> "pay_up_front"
        PaymentMode.UNKNOWN -> "unknown"
    }

    private fun escapeJsonString(value: String): String {
        val sb = StringBuilder("\"")
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '' -> sb.append("\\f")
                else -> {
                    if (c.code < 0x20) {
                        sb.append("\\u")
                        sb.append(String.format("%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }
}
