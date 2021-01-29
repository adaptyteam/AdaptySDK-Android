package com.adapty.utils

import com.adapty.api.AttributionType
import com.adapty.api.entity.attribution.AttributeUpdateAttributionReq
import com.adapty.api.entity.paywalls.DataContainer
import com.adapty.api.entity.paywalls.PaywallMapper
import com.adapty.api.entity.purchaserInfo.AttributePurchaserInfoRes
import com.adapty.api.entity.purchaserInfo.model.AccessLevelInfoModel
import com.adapty.api.entity.purchaserInfo.model.NonSubscriptionInfoModel
import com.adapty.api.entity.purchaserInfo.model.PurchaserInfoModel
import com.adapty.api.entity.purchaserInfo.model.SubscriptionInfoModel
import org.json.JSONObject
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun generatePurchaserInfoModel(res: AttributePurchaserInfoRes) = PurchaserInfoModel(
    customerUserId = res.customerUserId,
    nonSubscriptions = res.nonSubscriptions?.mapValues { entry ->
        entry.value.map { nonSub ->
            NonSubscriptionInfoModel(
                purchaseId = nonSub.purchaseId,
                vendorProductId = nonSub.vendorProductId,
                store = nonSub.store,
                purchasedAt = nonSub.purchasedAt,
                isOneTime = nonSub.isOneTime,
                isSandbox = nonSub.isSandbox,
                isRefund = nonSub.isRefund
            )
        }
    },
    accessLevels = res.accessLevels?.mapValues { (key, value) ->
        AccessLevelInfoModel(
            id = key,
            isActive = value.isActive,
            vendorProductId = value.vendorProductId,
            vendorTransactionId = value.vendorTransactionId,
            vendorOriginalTransactionId = value.vendorOriginalTransactionId,
            store = value.store,
            activatedAt = value.activatedAt,
            startsAt = value.startsAt,
            renewedAt = value.renewedAt,
            expiresAt = value.expiresAt,
            isLifetime = value.isLifetime,
            cancellationReason = value.cancellationReason,
            isRefund = value.isRefund,
            activeIntroductoryOfferType = value.activeIntroductoryOfferType,
            activePromotionalOfferType = value.activePromotionalOfferType,
            willRenew = value.willRenew,
            isInGracePeriod = value.isInGracePeriod,
            unsubscribedAt = value.unsubscribedAt,
            billingIssueDetectedAt = value.billingIssueDetectedAt
        )
    },
    subscriptions = res.subscriptions?.mapValues { (_, sub) ->
        SubscriptionInfoModel(
            isActive = sub.isActive,
            vendorProductId = sub.vendorProductId,
            store = sub.store,
            activatedAt = sub.activatedAt,
            renewedAt = sub.renewedAt,
            expiresAt = sub.expiresAt,
            startsAt = sub.startsAt,
            isLifetime = sub.isLifetime,
            activeIntroductoryOfferType = sub.activeIntroductoryOfferType,
            activePromotionalOfferType = sub.activePromotionalOfferType,
            willRenew = sub.willRenew,
            isInGracePeriod = sub.isInGracePeriod,
            unsubscribedAt = sub.unsubscribedAt,
            billingIssueDetectedAt = sub.billingIssueDetectedAt,
            isSandbox = sub.isSandbox,
            isRefund = sub.isRefund,
            cancellationReason = sub.cancellationReason
        )
    },
    customAttributes = res.customAttributes
)

val priceFormatter by lazy {
    DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
}

private val currencyLocaleMap by lazy {
    hashMapOf<Currency, Locale>().apply {
        for (locale in Locale.getAvailableLocales()) {
            try {
                val currency = Currency.getInstance(locale)
                this[currency] = locale
            } catch (e: Exception) {
            }
        }
    }
}

fun getCurrencySymbol(currencyCode: String) =
    Currency.getInstance(currencyCode).getOnlySymbol() ?: currencyCode

private fun Currency.getOnlySymbol(): String? {
    if (!currencyLocaleMap.containsKey(this)) return null

    val rawSign = getSymbol(currencyLocaleMap[this])
    return rawSign.firstOrNull { char -> char !in CharRange('A', 'Z') }?.toString() ?: rawSign
}

fun formatPrice(priceAmountMicros: Long): String {
    return priceFormatter.format(
        BigDecimal.valueOf(priceAmountMicros)
            .divide(BigDecimal.valueOf(1_000_000L))
    )
}

fun getPeriodUnit(period: String) = period.takeIf(String::isNotEmpty)?.last()?.toString()

fun getPeriodNumberOfUnits(period: String) =
    period.replace("[^0-9]".toRegex(), "").takeIf(String::isNotEmpty)?.toInt()

internal fun ArrayList<DataContainer>.toPaywalls() = mapNotNull { it.attributes }.map(PaywallMapper::map)

private const val ADJUST_ATTRIBUTION_CLASS = "com.adjust.sdk.AdjustAttribution"

private val adjustAttributionClass: Class<*>? by lazy {
    try {
        Class.forName(ADJUST_ATTRIBUTION_CLASS)
    } catch (e: ClassNotFoundException) {
        null
    }
}

private fun convertAdjustAttributionToMap(adjustAttribution: Any) = hashMapOf(
    "adgroup" to getAdjustProperty(adjustAttribution, "adgroup"),
    "adid" to getAdjustProperty(adjustAttribution, "adid"),
    "campaign" to getAdjustProperty(adjustAttribution, "campaign"),
    "click_label" to getAdjustProperty(adjustAttribution, "clickLabel"),
    "creative" to getAdjustProperty(adjustAttribution, "creative"),
    "network" to getAdjustProperty(adjustAttribution, "network"),
    "tracker_name" to getAdjustProperty(adjustAttribution, "trackerName"),
    "tracker_token" to getAdjustProperty(adjustAttribution, "trackerToken")
)

private fun getAdjustProperty(adjustAttribution: Any, propName: String): Any {
    return try {
        adjustAttributionClass?.getField(propName)?.get(adjustAttribution) ?: ""
    } catch (e: Exception) {
        ""
    }
}

internal fun createAttributionData(
    attribution: Any,
    source: AttributionType,
    networkUserId: String?
) = AttributeUpdateAttributionReq().apply {
    this.source = source.toString()
    this.attribution = when {
        attribution is JSONObject -> {
            val jo = HashMap<String, Any>()
            for (a in attribution.keys()) {
                jo[a] = attribution.get(a)
            }
            jo
        }
        adjustAttributionClass?.isAssignableFrom(attribution::class.java) == true -> {
            convertAdjustAttributionToMap(attribution)
        }
        else -> {
            attribution
        }
    }

    this.networkUserId = networkUserId
}