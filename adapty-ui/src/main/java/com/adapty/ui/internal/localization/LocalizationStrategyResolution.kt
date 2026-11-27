package com.adapty.ui.internal.localization

import com.adapty.ui.AdaptyUiCustomLocalizationResolver
import com.adapty.ui.AdaptyUiLocalizationResolver
import com.adapty.ui.AdaptyUiLocalizationSource
import com.adapty.ui.AdaptyUiLocalizationStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

internal suspend fun AdaptyUiLocalizationStrategy.resolve(
    preferredLocales: List<String>,
    source: AdaptyUiLocalizationSource,
    isLegacyFormat: Boolean,
): String? {
    if (!source.hasChoice) return source.defaultLocale ?: source.locales.firstOrNull()
    customResolver?.await(preferredLocales, source)
        ?.takeIf(source::contains)
        ?.let { return it }
    return resolver.resolve(preferredLocales, source, isLegacyFormat)
}

private val AdaptyUiLocalizationSource.hasChoice: Boolean
    get() = (locales + listOfNotNull(defaultLocale)).toSet().size > 1

private fun AdaptyUiLocalizationSource.contains(locale: String): Boolean =
    locale in locales || locale == defaultLocale

internal fun AdaptyUiLocalizationResolver.resolve(
    preferredLocales: List<String>,
    source: AdaptyUiLocalizationSource,
    isLegacyFormat: Boolean,
): String? {
    val useLegacy = this === AdaptyUiLocalizationResolver.Legacy ||
        (this === AdaptyUiLocalizationResolver.Auto && isLegacyFormat)
    return if (useLegacy) {
        LegacyLocalizationResolver().resolve(preferredLocales, source)
    } else {
        SystemLocalizationResolver.resolve(preferredLocales, source)
    }
}

private suspend fun AdaptyUiCustomLocalizationResolver.await(
    preferredLocales: List<String>,
    source: AdaptyUiLocalizationSource,
): String? = withContext(Dispatchers.Main.immediate) {
    suspendCancellableCoroutine { continuation ->
        val answered = AtomicBoolean(false)
        resolve(preferredLocales, source) { locale ->
            if (answered.compareAndSet(false, true) && continuation.isActive) {
                continuation.resume(locale)
            }
        }
    }
}
