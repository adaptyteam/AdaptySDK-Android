package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.models.SubscriptionUpdateParamModel
import com.adapty.models.SubscriptionUpdateParamModel.ProrationMode.*
import com.android.billingclient.api.BillingFlowParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProrationModeMapper {

    @JvmSynthetic
    fun map(prorationMode: SubscriptionUpdateParamModel.ProrationMode) = when (prorationMode) {
        IMMEDIATE_WITH_TIME_PRORATION -> BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION
        IMMEDIATE_WITHOUT_PRORATION -> BillingFlowParams.ProrationMode.IMMEDIATE_WITHOUT_PRORATION
        IMMEDIATE_AND_CHARGE_PRORATED_PRICE -> BillingFlowParams.ProrationMode.IMMEDIATE_AND_CHARGE_PRORATED_PRICE
        DEFERRED -> BillingFlowParams.ProrationMode.DEFERRED
        IMMEDIATE_AND_CHARGE_FULL_PRICE -> BillingFlowParams.ProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE
    }
}