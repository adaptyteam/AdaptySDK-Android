package com.adapty.utils

import com.adapty.api.entity.purchaserInfo.AttributePurchaserInfoRes
import com.adapty.api.entity.purchaserInfo.model.NonSubscriptionsPurchaserInfoModel
import com.adapty.api.entity.purchaserInfo.model.PaidAccessLevelPurchaserInfoModel
import com.adapty.api.entity.purchaserInfo.model.PurchaserInfoModel
import com.adapty.api.entity.purchaserInfo.model.SubscriptionsPurchaserInfoModel
import java.util.regex.Matcher
import java.util.regex.Pattern

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
            updatedAt = it.updatedAt
        )
    }
}

fun formatPrice(price: String?, c: String?): String {
    if (price == null)
        return ""
    var p: String = price
    c?.let { code ->
        p = price.replace(code, "")
    }

    p = p.replace(",", ".")

    val pattern: Pattern = Pattern.compile("(\\d+(?:\\.\\d+))")
    val m: Matcher = pattern.matcher(p)
    if (m.find())
        p = m.group(1)

    return p.trim()
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