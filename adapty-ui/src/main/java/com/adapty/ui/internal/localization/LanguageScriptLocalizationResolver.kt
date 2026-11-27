package com.adapty.ui.internal.localization

import androidx.core.os.LocaleListCompat
import com.adapty.ui.AdaptyUiLocalizationSource
import java.util.Locale

internal object LanguageScriptLocalizationResolver {

    private val SYNONYMS = mapOf("tl" to "fil", "no" to "nb", "mo" to "ro")

    fun resolve(preferredLocales: List<String>, source: AdaptyUiLocalizationSource): String? {
        val preferences = preferredLocales.map(String::trim).filter(String::isNotEmpty)
        if (preferences.isEmpty()) return source.defaultLocale
        val flowLocales = source.locales.map(::normalize)
        for (raw in preferences) {
            val preferred = normalize(raw) ?: continue
            val exact = flowLocales.indexOf(preferred)
            if (exact >= 0) return source.locales[exact]
            val first = flowLocales.indexOfFirst { candidate ->
                candidate != null && LocaleListCompat.matchesLanguageAndScript(candidate, preferred)
            }
            if (first >= 0) return source.locales[first]
        }
        return source.defaultLocale
    }

    fun normalize(raw: String): Locale? = runCatching {
        val locale = Locale.forLanguageTag(raw.replace('_', '-'))
        val language = locale.language.takeIf(String::isNotEmpty) ?: return null
        Locale.Builder()
            .setLanguage(SYNONYMS[language] ?: language)
            .setScript(locale.script)
            .setRegion(locale.country)
            .build()
    }.getOrNull()
}
