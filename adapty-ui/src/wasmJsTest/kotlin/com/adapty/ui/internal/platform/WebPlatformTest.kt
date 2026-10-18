@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.platform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import com.adapty.ui.internal.ui.drawPlatformInnerShadow
import com.adapty.ui.internal.ui.drawPlatformShadow
import kotlin.test.assertTrue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUIWeb
import com.adapty.ui.WebEnvironment
import com.adapty.ui.internal.text.PlatformFormat
import com.adapty.ui.internal.text.naturalLineHeightSp
import com.adapty.ui.internal.text.resolveFontFamily
import com.adapty.ui.internal.text.resolvePlatformTypeface
import com.adapty.ui.internal.ui.composeMatrixFromRowMajor
import com.adapty.ui.internal.utils.decodeBase64
import com.adapty.ui.internal.utils.decodeImage
import com.adapty.ui.internal.utils.imageNaturalSize
import com.adapty.ui.internal.utils.loadImageBitmap
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

class WebPlatformTest {

    @AfterTest
    fun resetEnvironment() {
        AdaptyUIWeb.environment = WebEnvironment()
    }

    @Test
    fun decodeImageDecodesWebp() = runTest {
        val image = decodeImage(fetchResource("star@3x.webp"))
        assertNotNull(image, "WebP fixture must decode")
        assertEquals(63, image.width)
        assertEquals(60, image.height)
    }

    @Test
    fun decodeImageReturnsNullOnGarbage() {
        assertNull(decodeImage(byteArrayOf(1, 2, 3)))
        assertNull(decodeImage(ByteArray(0)))
    }

    @Test
    fun base64LenientLikeAndroid() {
        assertEquals("Adapty", decodeBase64("QWRh\ncHR5")?.decodeToString())
        assertEquals("Adapt", decodeBase64("QWRhcHQ")?.decodeToString(), "missing padding is accepted")
        assertEquals("Adapt", decodeBase64(" QWRh\r\n cHQ= \t")?.decodeToString(), "whitespace is ignored")
        assertNull(decodeBase64("QW*h"), "illegal characters fail like Base64.DEFAULT")
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun loadImageBitmapHandlesBase64Assets() = runTest {
        val bytes = fetchResource("star@3x.webp")
        val star = Asset.Composite(Asset.Image(Asset.Image.Source.Base64Str(Base64.encode(bytes))))
        val ctx = PlatformContextHandle()

        assertEquals(63 to 60, imageNaturalSize(ctx, star))

        val full = assertNotNull(loadImageBitmap(ctx, star))
        assertEquals(63 to 60, full.width to full.height)

        // FIT_MAX, wider bounds -> width 20 requested -> inSampleSize 2; WebP rounds the scaled size like Skia's codec
        val sampled = assertNotNull(loadImageBitmap(ctx, star, 20, 10, Asset.Image.ScaleType.FIT_MAX))
        assertEquals(32 to 30, sampled.width to sampled.height)

        // Android uses the fallback only when the main source throws (stream errors), not for undecodable bytes
        val undecodable = Asset.Composite(
            Asset.Image(Asset.Image.Source.Base64Str("AAAA")),
            fallback = Asset.Image(Asset.Image.Source.Base64Str(Base64.encode(bytes))),
        )
        assertNull(loadImageBitmap(ctx, undecodable))
        assertEquals(63 to 60, imageNaturalSize(ctx, undecodable), "natural size falls back on null like Android")
        val remote = Asset.Composite(
            Asset.Image(Asset.Image.Source.Uri(WebUri("https://example.com/a.webp"))),
            fallback = Asset.Image(Asset.Image.Source.Base64Str(Base64.encode(bytes))),
        )
        assertEquals(63, loadImageBitmap(ctx, remote)?.width, "unsupported source falls back")
        assertNull(loadImageBitmap(ctx, Asset.Composite(Asset.Image(Asset.Image.Source.Base64Str(null)))))
    }

    @Test
    fun imageFillMatrixMatchesAndroidRowMajor() {
        val m = composeMatrixFromRowMajor(floatArrayOf(2f, 0f, 10f, 0f, 3f, -4f, 0f, 0f, 1f))
        assertEquals(Offset(12f, -1f), m.map(Offset(1f, 1f)))
    }

    @Test
    fun shadowsDrawBlurredOutlines() {
        val outline = Outline.Rounded(RoundRect(30f, 30f, 70f, 70f, CornerRadius(8f)))
        val outer = render { drawPlatformShadow(outline, Color.Black, 10f, Offset(0f, 5f), 0f) }
        assertEquals(255, outer[50, 55].alpha255(), "shadow body is opaque")
        val bleed = outer[50, 80].alpha255()
        assertTrue(bleed in 1..254, "blur bleeds past the outline, alpha=$bleed")
        assertEquals(0, outer[50, 5].alpha255(), "no shadow far away")

        val inner = render { drawPlatformInnerShadow(outline, Color.Black, 6f, Offset(0f, 4f)) }
        assertTrue(inner[50, 32].alpha255() > 0, "inner shadow along the top edge")
        assertEquals(0, inner[50, 50].alpha255(), "center stays clear")
        // Outside the outline the literal Android algorithm (DST_IN path draw) leaves the layer fill; not asserted
    }

    @Test
    fun environmentFeedsScreenSeams() {
        AdaptyUIWeb.environment = WebEnvironment(
            safeInsets = AdaptyFlowInsets.of(1, 2, 3, 4),
            smallestScreenWidthDp = 600,
            isLandscape = false,
        )
        assertEquals(600, screenSmallestWidthDp())
        val probe = PlatformSafeInsets()
        assertNotNull(probe.rootInsetsOrNull(), "non-empty insets go through the fallback path")
        val fallback = probe.fallbackInsets(LayoutDirection.Ltr)
        assertEquals(listOf(1, 2, 3, 4), listOf(fallback.start, fallback.top, fallback.end, fallback.bottom))

        AdaptyUIWeb.environment = WebEnvironment()
        assertEquals(800, screenSmallestWidthDp())
        assertNull(PlatformSafeInsets().rootInsetsOrNull(), "no insets: fallback skipped")
    }

    // The positive path needs a real TTF (Roboto ships with the web app, Task 9); here only the guards are covered
    @Test
    fun fontsFallBackToDefaultWithoutValidRegistration() {
        val font = Asset.Font("adapty_system", emptyList(), 500, false, 15f, null, null, null)
        val ctx = PlatformContextHandle()
        AdaptyUIWeb.resetFontsForTest()
        assertEquals(FontFamily.Default, resolveFontFamily(font, ctx))

        AdaptyUIWeb.registerFonts(system = byteArrayOf(0, 1, 2, 3))
        assertEquals(FontFamily.Default, resolveFontFamily(font, ctx), "unparsable system font is not registered")
        assertNull(resolvePlatformTypeface(font, ctx).skia)
        assertEquals(18f, naturalLineHeightSp(15f, null), "approximation without registered fonts")
        AdaptyUIWeb.resetFontsForTest()
    }

    @Test
    fun formatStringSupportsPrintfSubset() {
        assertEquals("001f", PlatformFormat.formatString("%04x", 31))
        assertEquals("07", PlatformFormat.formatString("%02d", 7L))
        assertEquals("-5", PlatformFormat.formatString("%d", -5L))
        assertEquals("1.50", PlatformFormat.formatString("%.2f", 1.5))
        assertEquals("3", PlatformFormat.formatString("%.0f", 2.5 + 0.4))
        assertEquals("50%", PlatformFormat.formatString("%d%%", 50L))
        assertEquals("a-b", PlatformFormat.formatString("%s-%s", "a", "b"))
    }
}

private suspend fun fetchResource(name: String): ByteArray {
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

private fun Color.alpha255(): Int = (alpha * 255f + 0.5f).toInt()

private fun render(block: DrawScope.() -> Unit): PixelMap {
    val bitmap = ImageBitmap(100, 100)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(100f, 100f), block)
    return bitmap.toPixelMap()
}
