package com.adapty.ui

import com.adapty.utils.Callback

/**
 * The localizations a flow offers: what an [AdaptyUiCustomLocalizationResolver] chooses from.
 *
 * @property[locales] The identifiers of the flow localizations, spelled and ordered as in the flow.
 * @property[defaultLocale] The identifier of the default localization of the flow, or `null`
 * if the flow has none.
 */
public class AdaptyUiLocalizationSource internal constructor(
    public val locales: List<String>,
    public val defaultLocale: String?,
) {
    override fun toString(): String =
        "AdaptyUiLocalizationSource(locales=$locales, defaultLocale=$defaultLocale)"
}

/**
 * A built-in algorithm that chooses a flow localization for the preferred locales.
 *
 * - [Auto] — [Legacy] for flows in a format older than 5.0.0, [System] otherwise.
 * - [Legacy] — the algorithm of earlier SDK versions: takes only the first preferred locale and
 *   matches it against the flow localizations exactly, dropping subtags from the end
 *   (`pt-BR-x` → `pt-BR` → `pt`). Identifiers are compared as is, without case or `_`/`-`
 *   normalization of the flow localizations.
 * - [System] — chooses the localization Android resources of the device would choose for the
 *   preferred locales: language and script must match, then the closest region wins
 *   (`es-MX` → `es-419` → `es`). Synonyms `tl`/`fil`, `no`/`nb`, `mo`/`ro` are matched. Below
 *   API 24, whose resource selection knows neither scripts nor parent regions, it takes the first
 *   localization with the same language and script.
 *
 * A built-in resolver always answers. If it finds nothing, the default localization of the flow
 * is used.
 */
public class AdaptyUiLocalizationResolver private constructor(
    private val name: String,
) {
    override fun toString(): String = name

    public companion object {
        @JvmField
        public val Auto: AdaptyUiLocalizationResolver = AdaptyUiLocalizationResolver("Auto")

        @JvmField
        public val Legacy: AdaptyUiLocalizationResolver = AdaptyUiLocalizationResolver("Legacy")

        @JvmField
        public val System: AdaptyUiLocalizationResolver = AdaptyUiLocalizationResolver("System")
    }
}

/**
 * Implement this interface to choose the flow localization yourself, see
 * [AdaptyUiLocalizationStrategy.Custom].
 */
public fun interface AdaptyUiCustomLocalizationResolver {
    /**
     * Called on the main thread once the flow configuration is loaded.
     *
     * Call [callback] exactly once, from any thread: with an identifier from
     * [AdaptyUiLocalizationSource.locales] to use that localization, or with `null` to let the
     * fallback resolver choose. An identifier the flow does not have is ignored, and the
     * fallback resolver chooses. There is no timeout: the configuration is not delivered until
     * [callback] is called.
     *
     * Not called when the flow has only one localization or none: that localization is used.
     *
     * @param[preferredLocales] The preferred locales passed to `AdaptyUI.getFlowConfiguration`,
     * trimmed, without empty ones.
     * @param[source] The localizations of the flow.
     */
    public fun resolve(
        preferredLocales: List<String>,
        source: AdaptyUiLocalizationSource,
        callback: Callback<String?>,
    )
}

/**
 * How `AdaptyUI.getFlowConfiguration` chooses the flow localization for the preferred locales.
 *
 * - [Auto], [Legacy], [System] — the built-in resolvers, see [AdaptyUiLocalizationResolver].
 * - [Custom] — your own resolver; when it answers `null` or an identifier the flow does not
 *   have, the built-in fallback resolver chooses.
 *
 * With an empty list of preferred locales, every built-in resolver gives the default localization
 * of the flow.
 */
public class AdaptyUiLocalizationStrategy private constructor(
    @get:JvmSynthetic internal val resolver: AdaptyUiLocalizationResolver,
    @get:JvmSynthetic internal val customResolver: AdaptyUiCustomLocalizationResolver?,
) {
    override fun toString(): String =
        if (customResolver == null) resolver.toString() else "Custom(fallbackResolver=$resolver)"

    public companion object {
        @JvmField
        public val Auto: AdaptyUiLocalizationStrategy =
            AdaptyUiLocalizationStrategy(AdaptyUiLocalizationResolver.Auto, null)

        @JvmField
        public val Legacy: AdaptyUiLocalizationStrategy =
            AdaptyUiLocalizationStrategy(AdaptyUiLocalizationResolver.Legacy, null)

        @JvmField
        public val System: AdaptyUiLocalizationStrategy =
            AdaptyUiLocalizationStrategy(AdaptyUiLocalizationResolver.System, null)

        /**
         * @param[resolver] Chooses the localization.
         * @param[fallbackResolver] Chooses the localization when [resolver] answers `null` or an
         * identifier the flow does not have.
         */
        @JvmStatic
        @JvmOverloads
        public fun Custom(
            resolver: AdaptyUiCustomLocalizationResolver,
            fallbackResolver: AdaptyUiLocalizationResolver = AdaptyUiLocalizationResolver.Auto,
        ): AdaptyUiLocalizationStrategy = AdaptyUiLocalizationStrategy(fallbackResolver, resolver)
    }
}
