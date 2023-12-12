package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.PaywallDto
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallPicker {

    @JvmSynthetic
    fun pick(
        cachedPaywall: PaywallDto?,
        fallbackPaywall: PaywallDto?,
        locales: Set<String?>,
    ): PaywallDto? {
        val languageCodes = locales.map { locale ->
            locale?.let(::extractLanguageCode)?.lowercase(Locale.ENGLISH)
        }
        val cachedPaywall = cachedPaywall?.takeIf { paywall ->
            paywall.paywallId != null && (languageCodes.contains(null) || paywall.getLanguageCode() in languageCodes)
        }

        return when {
            cachedPaywall == null -> fallbackPaywall
            fallbackPaywall == null -> cachedPaywall
            cachedPaywall.isNewerThan(fallbackPaywall) -> cachedPaywall
            else -> fallbackPaywall
        }
    }

    private fun PaywallDto.isNewerThan(other: PaywallDto) =
        (this.updatedAt ?: 0L) >= (other.updatedAt ?: 0L)

    private fun PaywallDto.getLanguageCode() =
        this.remoteConfig?.lang?.split("-")?.getOrNull(0)
            ?.lowercase(Locale.ENGLISH)
}