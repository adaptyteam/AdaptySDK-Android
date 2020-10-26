package com.adapty.utils

import com.adapty.api.entity.purchaserInfo.AttributePurchaserInfoRes
import com.adapty.api.entity.purchaserInfo.model.NonSubscriptionsPurchaserInfoModel
import com.adapty.api.entity.purchaserInfo.model.PaidAccessLevelPurchaserInfoModel
import com.adapty.api.entity.purchaserInfo.model.PurchaserInfoModel
import com.adapty.api.entity.purchaserInfo.model.SubscriptionsPurchaserInfoModel
import java.lang.Exception
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun generatePurchaserInfoModel(res: AttributePurchaserInfoRes): PurchaserInfoModel {
    res.let {
        var nonSubs: HashMap<String, ArrayList<NonSubscriptionsPurchaserInfoModel>>? = null
        it.nonSubscriptions?.let { hashmap ->
            hashmap.forEach { map ->

                val array = ArrayList<NonSubscriptionsPurchaserInfoModel>()
                map.value.apply {
                    for (ns in this)
                        array.add(
                            NonSubscriptionsPurchaserInfoModel(
                                purchaseId = ns.purchaseId,
                                vendorProductId = ns.vendorProductId,
                                store = ns.store,
                                purchasedAt = ns.purchasedAt,
                                isOneTime = ns.isOneTime,
                                isSandbox = ns.isSandbox
                            )
                        )
                }

                if (nonSubs == null)
                    nonSubs = HashMap()

                nonSubs?.put(map.key, array)
            }
        }

        var subs: HashMap<String, SubscriptionsPurchaserInfoModel>? = null

        it.subscriptions?.let { hashMap ->
            hashMap.forEach { map ->
                var model: SubscriptionsPurchaserInfoModel? = null
                map.value.apply {
                    model = SubscriptionsPurchaserInfoModel(
                        isActive = this.isActive,
                        vendorProductId = this.vendorProductId,
                        store = this.store,
                        referenceName = this.referenceName,
                        purchasedAt = this.purchasedAt,
                        renewedAt = this.renewedAt,
                        expiresAt = this.expiresAt,
                        startsAt = this.startsAt,
                        isLifetime = this.isLifetime,
                        activeIntroductoryOfferType = this.activeIntroductoryOfferType,
                        activePromotionalOfferType = this.activePromotionalOfferType,
                        willRenew = this.willRenew,
                        isInGracePeriod = this.isInGracePeriod,
                        unsubscribedAt = this.unsubscribedAt,
                        billingIssueDetectedAt = this.billingIssueDetectedAt,
                        isSandbox = this.isSandbox
                    )
                }

                if (subs == null)
                    subs = HashMap()

                model?.let { nModel ->
                    subs?.put(map.key, nModel)
                }

            }
        }

        var pal: HashMap<String, PaidAccessLevelPurchaserInfoModel>? = null

        it.paidAccessLevels?.let { hashMap ->
            hashMap.forEach { map ->
                var model: PaidAccessLevelPurchaserInfoModel? = null
                map.value.apply {
                    model = PaidAccessLevelPurchaserInfoModel(
                        isActive = this.isActive,
                        vendorProductId = this.vendorProductId,
                        store = this.store,
                        purchasedAt = this.purchasedAt,
                        renewedAt = this.renewedAt,
                        expiresAt = this.expiresAt,
                        isLifetime = this.isLifetime,
                        activeIntroductoryOfferType = this.activeIntroductoryOfferType,
                        activePromotionalOfferType = this.activePromotionalOfferType,
                        willRenew = this.willRenew,
                        isInGracePeriod = this.isInGracePeriod,
                        unsubscribedAt = this.unsubscribedAt,
                        billingIssueDetectedAt = this.billingIssueDetectedAt
                    )
                }

                if (pal == null)
                    pal = HashMap()

                model?.let { nModel ->
                    pal?.put(map.key, nModel)
                }
            }
        }

        return PurchaserInfoModel(
            appId = it.appId,
            amplitudeUserId = it.amplitudeUserId,
            birthday = it.birthday,
            cognitoId = it.cognitoId,
            createdAt = it.createdAt,
            customerUserId = it.customerUserId,
            email = it.email,
            facebookUserId = it.facebookUserId,
            firstName = it.firstName,
            gender = it.gender,
            idfa = it.idfa,
            lastName = it.lastName,
            mixpanelUserId = it.mixpanelUserId,
            nonSubscriptions = nonSubs,
            paidAccessLevels = pal,
            subscriptions = subs,
            updatedAt = it.updatedAt,
            promotionalOfferEligibility = it.promotionalOfferEligibility,
            introductoryOfferEligibility = it.introductoryOfferEligibility,
            customAttributes = it.customAttributes
        )
    }
}

val priceFormatter by lazy {
    DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
}

fun formatPrice(priceAmountMicros: Long) : String {
    return priceFormatter.format(
        BigDecimal.valueOf(priceAmountMicros)
            .divide(BigDecimal.valueOf(1_000_000L))
    )
}

fun getPeriodUnit(period: String) : String? {
    if (period.isEmpty())
        return null

    return period.last().toString()
}

fun getPeriodNumberOfUnits(period: String) : Int? {
    val p = period.replace("[^0-9]".toRegex(), "")
    if (p.isEmpty())
        return null

    return p.toInt()
}

internal const val ADJUST_ATTRIBUTION_CLASS = "com.adjust.sdk.AdjustAttribution"

internal val adjustAttributionClass : Class<*> by lazy {
    Class.forName(ADJUST_ATTRIBUTION_CLASS)
}

internal fun convertAdjustAttributionToMap(adjustAttribution: Any) = hashMapOf(
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
        adjustAttributionClass.getField(propName).get(adjustAttribution)
    } catch (e: Exception) {
        ""
    }
}