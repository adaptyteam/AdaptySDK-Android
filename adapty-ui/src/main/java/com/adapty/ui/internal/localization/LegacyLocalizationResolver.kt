package com.adapty.ui.internal.localization

import com.adapty.ui.AdaptyUiLocalizationSource
import java.util.Locale

internal class LegacyLocalizationResolver(
    private val ignoresCase: Boolean = false,
    private val replaceUnderscores: Boolean = false,
) {

    fun resolve(preferredLocales: List<String>, source: AdaptyUiLocalizationSource): String? {
        val locale = preferredLocales.firstOrNull() ?: return source.defaultLocale
        return resolve(locale, source)
    }

    fun resolve(locale: String, source: AdaptyUiLocalizationSource): String? {
        val parts = locale.replace("_", "-")
            .lowercase(Locale.ROOT)
            .split("-")
            .toMutableList()

        val locales = LinkedHashMap<String, String>()
        source.locales.forEach { original ->
            var key = if (replaceUnderscores) original.replace("_", "-") else original
            if (ignoresCase) key = key.lowercase(Locale.ROOT)
            if (key !in locales) locales[key] = original
        }

        while (parts.isNotEmpty()) {
            val candidate = parts.joinToString("-")
            val found = locales[candidate]
            if (found != null) {
                return if (candidate.isEmpty()) source.defaultLocale else found
            }
            parts.removeAt(parts.lastIndex)
        }
        return source.defaultLocale
    }
}
