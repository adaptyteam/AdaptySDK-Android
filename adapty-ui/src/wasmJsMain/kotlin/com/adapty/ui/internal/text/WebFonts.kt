@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.adapty.ui.internal.platform.ERROR
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformTypeface
import com.adapty.ui.internal.platform.WARN
import com.adapty.ui.internal.platform.log
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface as SkTypeface

/**
 * Fonts registered by the host page (Android has them in /system/fonts).
 * The system font stands in for every font asset, like Android's default sans-serif (Roboto) does for
 * `adapty_system` and unknown family names; weight/italic go through the variable font axes.
 */
internal object WebFonts {

    private class Registered(
        val generation: Int,
        val system: ByteArray,
        val systemTypeface: SkTypeface,
        val fallbacks: List<ByteArray>,
    )

    private var registered: Registered? = null

    // Bumps on every registration so compositions re-register fallbacks with their font resolver
    private var generationState by mutableIntStateOf(0)

    private val families = HashMap<Pair<Int, Boolean>, FontFamily>()
    private val typefaces = HashMap<Pair<Int, Boolean>, PlatformTypeface>()
    private var warnedMissing = false

    fun register(system: ByteArray, symbolFallbacks: List<ByteArray>, emoji: ByteArray?) {
        val systemTypeface = makeTypeface(system)
        if (systemTypeface == null) {
            log(ERROR) { "[AdaptyUI] web fonts: the system font could not be parsed; fonts are not registered" }
            return
        }
        val fallbacks = (symbolFallbacks + listOfNotNull(emoji)).filterIndexed { i, bytes ->
            (makeTypeface(bytes) != null).also { ok ->
                if (!ok) log(ERROR) { "[AdaptyUI] web fonts: fallback font #$i could not be parsed; skipped" }
            }
        }
        families.clear()
        typefaces.clear()
        registered = Registered(generationState + 1, system, systemTypeface, fallbacks)
        generationState += 1
    }

    internal fun reset() {
        registered = null
        families.clear()
        typefaces.clear()
        generationState += 1
    }

    fun family(weight: Int, italic: Boolean): FontFamily? {
        val reg = registered ?: return null.also { warnMissing() }
        return families.getOrPut(weight to italic) {
            FontFamily(systemFont(reg, weight, italic))
        }
    }

    fun typeface(weight: Int, italic: Boolean): PlatformTypeface? {
        val reg = registered ?: return null.also { warnMissing() }
        return typefaces.getOrPut(weight to italic) {
            val variations = buildList {
                add(org.jetbrains.skia.FontVariation("wght", weight.toFloat()))
                if (italic) add(org.jetbrains.skia.FontVariation("ital", 1f))
            }
            PlatformTypeface(runCatching { reg.systemTypeface.makeClone(variations.toTypedArray()) }.getOrDefault(reg.systemTypeface))
        }
    }

    /** Default (no font asset) typeface: Android's default is sans-serif at 400. */
    fun defaultTypeface(): PlatformTypeface? = if (registered == null) null else typeface(400, false)

    /**
     * Families whose loading registers the glyph fallbacks in the resolver's Skia font collection:
     * the 400 system font first (text without a font asset), then the symbol fonts, then emoji.
     */
    private fun fallbackFamilies(reg: Registered): List<FontFamily> =
        listOf(FontFamily(systemFont(reg, 400, false))) +
            reg.fallbacks.mapIndexed { i, bytes ->
                FontFamily(Font("adapty-web-fallback-$i-g${reg.generation}", bytes, FontWeight.Normal, FontStyle.Normal))
            }

    /**
     * Loads the fallback fonts through the current [LocalFontFamilyResolver] during composition.
     * The fonts are Blocking (in-memory), so `resolve` registers them synchronously and the first frame has no tofu.
     */
    @Composable
    fun RegisterFallbacks() {
        val resolver = LocalFontFamilyResolver.current
        val generation = generationState
        remember(resolver, generation) { registerFallbacksWith(resolver) }
    }

    /** Loads the fallback fonts into [resolver]'s Skia font collection (Blocking fonts: synchronous). */
    fun registerFallbacksWith(resolver: FontFamily.Resolver) {
        val reg = registered ?: return
        fallbackFamilies(reg).forEach { family ->
            runCatching { resolver.resolve(family) }
                .onFailure { e -> log(ERROR) { "[AdaptyUI] web fonts: fallback registration failed: ${e.message}" } }
        }
    }

    // Identity carries weight/style so no font cache can collapse the variation instances
    private fun systemFont(reg: Registered, weight: Int, italic: Boolean) =
        Font(
            identity = "adapty-web-system-g${reg.generation}-w$weight-i${if (italic) 1 else 0}",
            data = reg.system,
            weight = FontWeight(weight.coerceIn(1, 1000)),
            style = if (italic) FontStyle.Italic else FontStyle.Normal,
            variationSettings = if (italic) {
                FontVariation.Settings(FontVariation.weight(weight.coerceIn(1, 1000)), FontVariation.italic(1f))
            } else {
                FontVariation.Settings(FontVariation.weight(weight.coerceIn(1, 1000)))
            },
        )

    private fun makeTypeface(bytes: ByteArray): SkTypeface? =
        if (bytes.isEmpty()) null else runCatching { FontMgr.default.makeFromData(Data.makeFromBytes(bytes)) }.getOrNull()

    private fun warnMissing() {
        if (!warnedMissing) {
            warnedMissing = true
            log(WARN) { "[AdaptyUI] web fonts: AdaptyUIWeb.registerFonts was not called; text uses the platform default" }
        }
    }
}
