@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.localization

import android.content.Context
import android.os.Build
import com.adapty.internal.di.Dependencies
import com.adapty.internal.utils.InternalAdaptyApi
import androidx.annotation.RequiresApi
import com.adapty.ui.AdaptyUiLocalizationSource
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.WARN

internal object SystemLocalizationResolver {

    fun resolve(
        preferredLocales: List<String>,
        source: AdaptyUiLocalizationSource,
        context: Context? = appContext(),
    ): String? {
        val preferences = preferredLocales.map(String::trim).filter(String::isNotEmpty)
        if (preferences.isEmpty()) return source.defaultLocale
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                return resolveWithResources(context, preferences, source)
            } catch (e: Exception) {
                log(WARN) { "$LOG_PREFIX flow localization: resources can not be asked, falling back (${e.javaClass.name}: ${e.message})" }
            }
        }
        return LanguageScriptLocalizationResolver.resolve(preferences, source)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun resolveWithResources(
        context: Context,
        preferences: List<String>,
        source: AdaptyUiLocalizationSource,
    ): String? {
        val flowLocales = source.locales.map(LanguageScriptLocalizationResolver::normalize)
        var probe: LocaleResourceProbe? = null
        try {
            for (raw in preferences) {
                val preferred = LanguageScriptLocalizationResolver.normalize(raw) ?: continue
                val exact = flowLocales.indexOf(preferred)
                if (exact >= 0) return source.locales[exact]
                val resources = probe ?: LocaleResourceProbe.create(context, flowLocales).also { probe = it }
                val index = resources.ask(preferred)
                if (index >= 0) return source.locales[index]
            }
            return source.defaultLocale
        } finally {
            probe?.close()
        }
    }

    private fun appContext(): Context? = runCatching { Dependencies.injectInternal<Context>() }.getOrNull()
}
