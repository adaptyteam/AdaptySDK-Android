package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.PaywallDto
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallPicker {

    @JvmSynthetic
    fun pick(
        paywall1: PaywallDto?,
        paywall2: PaywallDto?,
        requestedLocale: String?
    ): PaywallDto? {
        val locales = setOf(requestedLocale, DEFAULT_PAYWALL_LOCALE, null)
        val paywall1 = paywall1?.takeIfContainsLocale(locales)
        val paywall2 = paywall2?.takeIfContainsLocale(locales)

        return when {
            paywall1 == null -> paywall2
            paywall2 == null -> paywall1
            paywall1.isNewerThan(paywall2) -> paywall1
            else -> paywall2
        }
    }

    private fun PaywallDto.isNewerThan(other: PaywallDto) =
        (this.updatedAt ?: 0L) >= (other.updatedAt ?: 0L)

    private fun PaywallDto.getLocaleOrNull() =
        this.remoteConfig?.lang?.split("-")?.getOrNull(0)
            ?.toLowerCase(Locale.ENGLISH)

    private fun PaywallDto.takeIfContainsLocale(locales: Collection<String?>) =
        this.takeIf { paywall -> paywall.getLocaleOrNull() in locales }
}