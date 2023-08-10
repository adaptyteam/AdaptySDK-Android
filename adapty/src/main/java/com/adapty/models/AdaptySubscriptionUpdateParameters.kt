package com.adapty.models

public class AdaptySubscriptionUpdateParameters(
    oldSubVendorProductId: String,
    public val replacementMode: ReplacementMode,
) {

    public val oldSubVendorProductId: String = oldSubVendorProductId.split(":")[0]

    public enum class ReplacementMode {
        WITH_TIME_PRORATION,
        CHARGE_PRORATED_PRICE,
        WITHOUT_PRORATION,
        DEFERRED,
        CHARGE_FULL_PRICE,
    }

    override fun toString(): String {
        return "SubscriptionUpdateParameters(oldSubVendorProductId='$oldSubVendorProductId', replacementMode=$replacementMode)"
    }
}