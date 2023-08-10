package com.adapty.models

import com.adapty.utils.ImmutableList

/**
 * @property[basePlanId] A base plan of Google's subscription product.
 * @property[offerId] A discount offer for this [basePlanId].
 * @property[offerTags] Tags defined in Google Play console for current [basePlanId] and [offerId].
 * @property[renewalType] Indicates whether the subscription is auto-renewing or prepaid.
 * @property[subscriptionPeriod] An information about the period for the discount phase.
 * @property[localizedSubscriptionPeriod] A formatted subscription period of the discount phase for a user’s locale.
 * @property[introductoryOfferPhases] A list that can contain up to two discount phases: the free trial phase and the introductory price phase.
 */
public class AdaptyProductSubscriptionDetails(
    public val basePlanId: String,
    public val offerId: String?,
    public val offerTags: ImmutableList<String>,
    public val renewalType: RenewalType,
    public val subscriptionPeriod: AdaptyProductSubscriptionPeriod,
    public val localizedSubscriptionPeriod: String,
    public val introductoryOfferPhases: ImmutableList<AdaptyProductDiscountPhase>,
) {

    /**
     * User’s eligibility for your introductory offer. Check this property before displaying info about introductory offers.
     */
    public val introductoryOfferEligibility: AdaptyEligibility get() = when {
        renewalType == RenewalType.PREPAID -> AdaptyEligibility.NOT_APPLICABLE
        introductoryOfferPhases.isEmpty() -> AdaptyEligibility.INELIGIBLE
        else -> AdaptyEligibility.ELIGIBLE
    }

    public enum class RenewalType { AUTORENEWABLE, PREPAID }
}