package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.PaywallDto

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallPicker {

    @JvmSynthetic
    fun pick(
        paywall1: PaywallDto?,
        paywall2: PaywallDto?,
    ): PaywallDto? {
        return when {
            paywall1 == null && paywall2 == null -> null
            paywall1 != null && (paywall2 == null || paywall1.isNewerThan(paywall2)) -> paywall1
            else -> paywall2
        }
    }

    private fun PaywallDto.isNewerThan(other: PaywallDto) =
        (this.updatedAt ?: 0L) >= (other.updatedAt ?: 0L)
}