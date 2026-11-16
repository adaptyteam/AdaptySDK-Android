package com.adapty.ui.internal.script

import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPeriodUnit
import com.adapty.models.AdaptyProductDiscountPhase
import com.adapty.models.AdaptyProductDiscountPhase.PaymentMode
import com.adapty.models.AdaptyProductSubscriptionDetails
import com.adapty.models.AdaptyProductSubscriptionPeriod
import com.adapty.utils.ImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * `SDKProducts[*].subscription.offer` for the flow script: every Google Play discount phase is
 * passed, in Play order.
 */
public class SDKGlobalsOfferTest {

    private val freeTrialWeek = AdaptyProductDiscountPhase(
        price = price("0", "Free"),
        numberOfPeriods = 1,
        paymentMode = PaymentMode.FREE_TRIAL,
        localizedNumberOfPeriods = "1 week",
        subscriptionPeriod = AdaptyProductSubscriptionPeriod(AdaptyPeriodUnit.WEEK, 1),
        localizedSubscriptionPeriod = "1 week",
    )

    private val introThreeMonths = AdaptyProductDiscountPhase(
        price = price("2.99", "$2.99"),
        numberOfPeriods = 3,
        paymentMode = PaymentMode.PAY_AS_YOU_GO,
        localizedNumberOfPeriods = "3 months",
        subscriptionPeriod = AdaptyProductSubscriptionPeriod(AdaptyPeriodUnit.MONTH, 1),
        localizedSubscriptionPeriod = "1 month",
    )

    private val freeTrialWeekJson =
        """{"price":{"amount":0.0,"currencyCode":"USD","currencySymbol":"$","localizedString":"Free"},""" +
            """"paymentMode":"free_trial","period":{"unit":"week","numberOfUnits":1},""" +
            """"numberOfPeriods":1,"localizedPeriod":"1 week","localizedNumberOfPeriods":"1 week"}"""

    private val introThreeMonthsJson =
        """{"price":{"amount":2.99,"currencyCode":"USD","currencySymbol":"$","localizedString":"$2.99"},""" +
            """"paymentMode":"pay_as_you_go","period":{"unit":"month","numberOfUnits":1},""" +
            """"numberOfPeriods":3,"localizedPeriod":"1 month","localizedNumberOfPeriods":"3 months"}"""

    @Test
    public fun freeTrialAndIntroPrice_passesBothPhasesInPlayOrder() {
        val json = SDKGlobals.buildOfferJson(subscription("trial-intro", freeTrialWeek, introThreeMonths))

        assertEquals(
            """{"id":"trial-intro","type":"introductory","phases":[$freeTrialWeekJson,$introThreeMonthsJson]}""",
            json,
        )
    }

    @Test
    public fun singlePhaseOffer_isOneElementArray() {
        val json = SDKGlobals.buildOfferJson(subscription("intro", introThreeMonths))

        assertEquals("""{"id":"intro","type":"introductory","phases":[$introThreeMonthsJson]}""", json)
    }

    @Test
    public fun offerWithoutDiscountPhases_hasNoPhasesKey() {
        val json = SDKGlobals.buildOfferJson(subscription("empty"))

        assertEquals("""{"id":"empty","type":"introductory"}""", json)
    }

    @Test
    public fun noOffer_isNull() {
        assertNull(SDKGlobals.buildOfferJson(subscription(offerId = null)))
    }

    private fun price(amount: String, localizedString: String) =
        AdaptyPaywallProduct.Price(BigDecimal(amount), localizedString, "USD", "$")

    private fun subscription(offerId: String?, vararg phases: AdaptyProductDiscountPhase) =
        AdaptyProductSubscriptionDetails(
            basePlanId = "monthly",
            offerId = offerId,
            offerTags = ImmutableList(emptyList()),
            renewalType = AdaptyProductSubscriptionDetails.RenewalType.AUTORENEWABLE,
            subscriptionPeriod = AdaptyProductSubscriptionPeriod(AdaptyPeriodUnit.MONTH, 1),
            localizedSubscriptionPeriod = "1 month",
            introductoryOfferPhases = ImmutableList(phases.toList()),
        )
}
