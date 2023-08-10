package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.models.AdaptySubscriptionUpdateParameters.ReplacementMode.*
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams.ReplacementMode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ReplacementModeMapper {

    @JvmSynthetic
    fun map(replacementMode: AdaptySubscriptionUpdateParameters.ReplacementMode) = when (replacementMode) {
        WITH_TIME_PRORATION -> ReplacementMode.WITH_TIME_PRORATION
        WITHOUT_PRORATION -> ReplacementMode.WITHOUT_PRORATION
        CHARGE_PRORATED_PRICE -> ReplacementMode.CHARGE_PRORATED_PRICE
        DEFERRED -> ReplacementMode.DEFERRED
        CHARGE_FULL_PRICE -> ReplacementMode.CHARGE_FULL_PRICE
    }
}