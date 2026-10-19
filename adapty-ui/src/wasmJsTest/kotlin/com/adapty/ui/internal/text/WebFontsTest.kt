@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.text

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUIWeb
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.ui.platformBlurSigma
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import kotlin.js.Promise
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Compose text stack under Karma with the Android emulator fonts (wasmJsTest/resources):
 * Roboto-Regular.ttf (variable system font, Apache-2.0) and NotoSansSymbols-Regular-Subsetted.ttf
 * (symbol fallback, SIL OFL 1.1).
 */
class WebFontsTest {

    @AfterTest
    fun reset() {
        AdaptyUIWeb.resetFontsForTest()
    }

    @Test
    fun platformBlurSigmaMatchesAndroidRenderEffect() {
        // hwui RenderEffect.createBlurEffect -> SkImageFilters::Blur(Blur::convertRadiusToSigma(r))
        assertEquals(0.57735f * 10f + 0.5f, platformBlurSigma(10f))
        assertEquals(0.57735f * 24f + 0.5f, platformBlurSigma(24f))
        assertEquals(0f, platformBlurSigma(0f))
    }

    @Test
    fun weights500And600LayOutDifferently() = runTest {
        AdaptyUIWeb.registerFonts(system = fetchResource("Roboto-Regular.ttf"))
        val ctx = PlatformContextHandle()
        val f500 = resolveFontFamily(font(500), ctx)
        val f600 = resolveFontFamily(font(600), ctx)
        assertNotEquals(FontFamily.Default, f500)
        assertNotEquals(f500, f600)

        val measurer = measurer()
        val text = "Better sleep starts tonight"
        val w500 = measurer.measure(text, TextStyle(fontFamily = f500, fontSize = 100.sp)).size.width
        val w600 = measurer.measure(text, TextStyle(fontFamily = f600, fontSize = 100.sp)).size.width
        assertTrue(w600 > w500, "600 must be wider than 500 (variable wght applied): 500=$w500 600=$w600")

        // Same width for the same weight: the per-weight family is stable
        val again = measurer.measure(text, TextStyle(fontFamily = resolveFontFamily(font(500), ctx), fontSize = 100.sp)).size.width
        assertEquals(w500, again)
    }

    @Test
    fun regularWeightUsesLinearAdvances() = runTest {
        val roboto = fetchResource("Roboto-Regular.ttf")
        AdaptyUIWeb.registerFonts(system = roboto)
        val family = resolveFontFamily(font(400), PlatformContextHandle())
        val text = "Unlimited free access for 7 days"

        // Android lays out with unhinted advances: the sum of the Roboto wght-400 advances at 16sp x density 2
        val typeface = FontMgr.default.makeFromData(Data.makeFromBytes(roboto)) ?: fail("Roboto must parse")
        val linear = org.jetbrains.skia.Font(typeface, 32f).apply {
            hinting = org.jetbrains.skia.FontHinting.NONE
            isSubpixel = true
            isLinearMetrics = true
        }.let { f -> f.getWidths(f.getStringGlyphs(text)).sum() }

        val measured = TextMeasurer(createFontFamilyResolver(), Density(2f), LayoutDirection.Ltr).measure(
            text,
            TextStyle(fontFamily = family, fontSize = 16.sp, platformStyle = platformTextStyle(includeFontPadding = false)),
        ).size.width
        assertTrue(abs(measured - linear) <= linear * 0.01f, "400-weight width must match linear advances: got $measured, linear $linear")
    }

    @Test
    fun checkmarkUsesSymbolFallback() = runTest {
        val roboto = fetchResource("Roboto-Regular.ttf")
        val symbols = fetchResource("NotoSansSymbols-Regular-Subsetted.ttf")
        val check = "✓"

        // Expected advance of U+2713 from the symbol font at 100px
        val symbolsTypeface = FontMgr.default.makeFromData(Data.makeFromBytes(symbols)) ?: fail("symbols font must parse")
        val expected = org.jetbrains.skia.Font(symbolsTypeface, 100f).let { f -> f.getWidths(f.getStringGlyphs(check)).single() }

        AdaptyUIWeb.registerFonts(system = roboto)
        val family = resolveFontFamily(font(500), PlatformContextHandle())
        val noFallback = createFontFamilyResolver()
        WebFonts.registerFallbacksWith(noFallback)
        val tofu = measurer(noFallback).measure(check, TextStyle(fontFamily = family, fontSize = 100.sp)).size.width

        AdaptyUIWeb.registerFonts(system = roboto, symbolFallbacks = listOf(symbols))
        val familyWithFallbacks = resolveFontFamily(font(500), PlatformContextHandle())
        val withFallback = createFontFamilyResolver()
        WebFonts.registerFallbacksWith(withFallback)
        val glyph = measurer(withFallback).measure(check, TextStyle(fontFamily = familyWithFallbacks, fontSize = 100.sp)).size.width

        assertTrue(abs(glyph - expected) <= 1f, "✓ advance comes from NotoSansSymbols: got $glyph, expected $expected (tofu=$tofu)")
        assertNotEquals(tofu, glyph, "without the fallback ✓ renders as tofu")
    }
}

private fun font(weight: Int) = Asset.Font("adapty_system", emptyList(), weight, false, 15f, null, null, null)

private fun measurer(resolver: FontFamily.Resolver = createFontFamilyResolver()) =
    TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr)

internal suspend fun fetchResource(name: String): ByteArray {
    val base = resourcesBaseUrl()
    if (base.isEmpty()) {
        fail("fixture base URL is not configured: karma.config.d/fixtures.js or the generated paths.js was not loaded")
    }
    val url = "$base/$name"
    val response = fetchBytes(url).await<JsAny>()
    val status = statusOf(response)
    if (status != 200) fail("fixture fetch returned HTTP $status for $url (check karma.config.d)")
    val size = lengthOf(response)
    return ByteArray(size) { byteAt(response, it).toByte() }
}

@JsFun("() => (window.__karma__ && window.__karma__.config && window.__karma__.config.adaptyUiTestResourcesUrl) || ''")
private external fun resourcesBaseUrl(): String

@JsFun("(url) => fetch(url).then(r => r.arrayBuffer().then(b => ({ status: r.status, body: new Uint8Array(b) })))")
private external fun fetchBytes(url: String): Promise<JsAny>

@JsFun("(r) => r.status")
private external fun statusOf(response: JsAny): Int

@JsFun("(r) => r.body.length")
private external fun lengthOf(response: JsAny): Int

@JsFun("(r, i) => r.body[i]")
private external fun byteAt(response: JsAny, index: Int): Int
