@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUIWeb
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformContextHandle
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/** Web text behavior that must match Android (Pixel Tablet reference, density 2). */
class WebTextParityTest {

    @AfterTest
    fun reset() {
        AdaptyUIWeb.resetFontsForTest()
    }

    @Test
    fun emojiRunsFollowEmojiPresentation() {
        // Emoji_Presentation=Yes: ✅ ❓ ⭐ ❌ 🌙 🎧
        assertEquals(listOf(2..2), emojiRuns("a ✅ b"))
        assertEquals(listOf(0..0, 2..2, 4..4, 6..7), emojiRuns("❓ ⭐ ❌ 🌙"))
        // Text presentation symbols stay with the symbol fonts: ✓ ✕ ✦ ∞ ♪ → ▶ ★
        assertEquals(emptyList(), emojiRuns("✓ ✕ ✦ ∞ ♪ → ▶ ★ 1 # *"))
        // VS16 turns text-default symbols into emoji, VS15 keeps text presentation
        assertEquals(listOf(0..1), emojiRuns("⚠️ ok"))
        assertEquals(listOf(0..1), emojiRuns("▶️"))
        assertEquals(emptyList(), emojiRuns("✅︎"))
    }

    @Test
    fun emojiRunsKeepSequencesWhole() {
        assertEquals(listOf(0..2), emojiRuns("1️⃣"), "keycap")
        assertEquals(listOf(0..1), emojiRuns("#⃣"), "keycap without VS16")
        assertEquals(listOf(0..3), emojiRuns("🇺🇸"), "flag")
        assertEquals(listOf(0..3), emojiRuns("👍🏽"), "skin tone")
        assertEquals(listOf(0..4), emojiRuns("👩‍💻"), "ZWJ")
        assertEquals(listOf(0..5), emojiRuns("🏳️‍🌈"), "VS16 + ZWJ")
        assertEquals(listOf(0..13), emojiRuns("🏴󠁧󠁢󠁥󠁮󠁧󠁿"), "tag sequence")
        assertEquals(listOf(0..3), emojiRuns("🌙🎧"), "adjacent runs merge")
        assertEquals(listOf(0..2), emojiRuns("☝🏽"), "text-default base + skin tone")
        assertEquals(listOf(2..4), emojiRuns("a ✌🏽 b"), "text-default base + skin tone")
    }

    @Test
    fun emojiRunsGetTheEmojiFont() = runTest {
        // Any parsable font stands in for NotoColorEmoji: only the span placement is checked
        AdaptyUIWeb.registerFonts(fetchResource("Roboto-Regular.ttf"), emoji = fetchResource("NotoSansSymbols-Regular-Subsetted.ttf"))
        val emoji = assertNotNull(WebFonts.emojiFamily())
        val text = AnnotatedString("Done ✅ ✓")
        val presented = text.withEmojiPresentation(emoji)
        assertEquals(listOf(5 to 6), presented.spanStyles.filter { it.item.fontFamily == emoji }.map { it.start to it.end })
        val plain = AnnotatedString("✓ ✦ ♪")
        assertSame(plain, plain.withEmojiPresentation(emoji))
        assertSame(text, text.withEmojiPresentation(null))
    }

    @Test
    fun tooLongWordStartsOnANewLine() = runTest {
        val family = registerSystemFont(700)
        val text = AnnotatedString("Lorem ipsum dolor sit amet, consectetur adipiscing elit")
        val layout = measurer().measure(text, TextStyle(fontFamily = family, fontSize = 16.sp), constraints = Constraints(maxWidth = 130))
        // Skia: "amet, co" / "nsectetu" / "r adipisci" / "ng elit"
        assertTrue(layout.lines().contains("amet, co"), "precondition (Skia breaks inside the word): ${layout.lines()}")

        val reflow = assertNotNull(textReflow(text, layout))
        assertNull(reflow.cut)
        val fixed = measurer().measure(text.withReflow(reflow.breaks, null), TextStyle(fontFamily = family, fontSize = 16.sp), constraints = Constraints(maxWidth = 130))
        val lines = fixed.lines()
        // Minikin: the word starts a line, then breaks by graphemes; a following word that fits goes on its own line
        assertEquals("amet,", lines[3].trim(), "lines: $lines")
        assertTrue(lines[4].startsWith("consect"), "lines: $lines")
        assertEquals("adipisci", lines[6], "lines: $lines")
        assertTrue(lines.none { it.startsWith("r adipisci") }, "lines: $lines")
        assertEquals(text.length, text.withReflow(reflow.breaks, null).length, "breaks replace spaces in place")
    }

    @Test
    fun reflowIsRecomputedWhenTheLayoutChanges() = runTest {
        val family = registerSystemFont(700)
        val text = AnnotatedString("Lorem ipsum dolor sit amet, consectetur adipiscing elit")
        val style = TextStyle(fontFamily = family, fontSize = 16.sp)
        val layout = measurer().measure(text, style, constraints = Constraints(maxWidth = 130))
        val reflow = assertNotNull(textReflow(text, layout))
        assertTrue(reflow.holdsFor(text, layout.layoutInput))

        // Same text and width, smaller font: the long word fits, the old break no longer holds
        val small = measurer().measure(text, style.copy(fontSize = 8.sp), constraints = Constraints(maxWidth = 130))
        assertFalse(reflow.holdsFor(text, small.layoutInput))
        assertNull(textReflow(text, small))
        assertFalse(reflow.holdsFor(text, measurer().measure(text, style.copy(fontWeight = FontWeight.Normal), constraints = Constraints(maxWidth = 130)).layoutInput))
        val otherDensity = TextMeasurer(createFontFamilyResolver(), Density(3f), LayoutDirection.Ltr)
            .measure(text, style, constraints = Constraints(maxWidth = 130))
        assertFalse(reflow.holdsFor(text, otherDensity.layoutInput))
        assertFalse(reflow.holdsFor(AnnotatedString(text.text + "!"), layout.layoutInput))
    }

    @Test
    fun scriptsWithoutSpacesAreNotReflowed() = runTest {
        val family = registerSystemFont(400)
        // Thai has no spaces between words; Skia breaks it by dictionary like Minikin
        val text = AnnotatedString("Hello สวัสดีครับยินดีต้อนรับสู่แอปพลิเคชันของเราขอบคุณมากครับ")
        val layout = measurer().measure(text, TextStyle(fontFamily = family, fontSize = 16.sp), constraints = Constraints(maxWidth = 200))
        // Precondition: line 1 starts inside the phrase that began after "Hello " on line 0 (the too-long-word shape)
        assertTrue(layout.lineCount > 1 && layout.getLineStart(1) > 6, "precondition: ${layout.lines()}")
        assertNull(textReflow(text, layout))
    }

    @Test
    fun wordsThatFitAreNotEdited() = runTest {
        val family = registerSystemFont(400)
        val text = AnnotatedString("Unlimited free access for 7 days")
        val layout = measurer().measure(text, TextStyle(fontFamily = family, fontSize = 16.sp), constraints = Constraints(maxWidth = 200))
        assertTrue(layout.lineCount > 1)
        assertNull(textReflow(text, layout))
    }

    @Test
    fun lastVisibleLineEndingAtHardBreakIsEllipsized() = runTest {
        val family = registerSystemFont(400)
        val style = TextStyle(fontFamily = family, fontSize = 16.sp)
        val text = AnnotatedString("00:59:55\nRemaining")
        val layout = measurer().measure(text, style, overflow = TextOverflow.Ellipsis, maxLines = 1)
        val reflow = assertNotNull(textReflow(text, layout))
        assertEquals(8, reflow.cut)
        val edited = text.withReflow(reflow.breaks, reflow.cut)
        assertEquals("00:59:55…\nRemaining", edited.text)
        val fixed = measurer().measure(edited, style, overflow = TextOverflow.Ellipsis, maxLines = 1)
        assertEquals(listOf("00:59:55…"), fixed.lines())
        // Like Android, the paragraph is as wide as the widest line of the whole text
        assertEquals(layout.size.width, fixed.size.width)

        // The next timer tick reuses the edits until they are checked (no frame without the ellipsis)
        assertEquals("00:59:56…\nRemaining", reflow.applyTo(AnnotatedString("00:59:56\nRemaining"))?.text)
        assertNull(reflow.applyTo(AnnotatedString("1:00\nRemaining")), "structure changed")

        // Not when the text fits, nor for Clip (Android ellipsizes only with Ellipsis)
        assertNull(textReflow(text, measurer().measure(text, style, overflow = TextOverflow.Ellipsis, maxLines = 2)))
        assertNull(textReflow(text, measurer().measure(text, style, overflow = TextOverflow.Clip, maxLines = 1)))
    }

    @Test
    fun mixedSizeLineMatchesAndroidLineBox() = runTest {
        val regular = registerSystemFont(400)
        val bold = resolveFontFamily(font(700), PlatformContextHandle())
        // data-binding/9_binding_bmi_calculator: 16sp text with a 28sp span on the first line, natural 16sp line height,
        // the gallery theme's LineHeightStyle(Center, Trim.None)
        val text = buildAnnotatedString {
            append("Your BMI is ")
            withStyle(SpanStyle(fontSize = 28.sp, fontFamily = bold)) { append("24.2") }
            append("\n")
            withStyle(SpanStyle(fontSize = 15.sp, fontFamily = bold)) { append("Normal") }
        }
        val style = TextStyle(
            fontFamily = regular,
            fontSize = 16.sp,
            lineHeight = naturalLineHeightSp(16f, null).sp,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        )
        val layout = measurer().measure(text, style)
        val box = assertNotNull(androidLineBox(layout, ::robotoMetrics))
        // Android: 14 px top padding + 2 x 38 px lines + 14 px bottom padding, first baseline at 52 px
        assertEquals(104, box.height)
        assertEquals(52f, layout.getLineBaseline(0) + box.top, 0.5f)

        // A single font size already matches Android
        assertNull(androidLineBox(measurer().measure("Your BMI is\nNormal", style), ::robotoMetrics))
    }

    private var roboto: ByteArray? = null

    private suspend fun registerSystemFont(weight: Int): FontFamily {
        val bytes = roboto ?: fetchResource("Roboto-Regular.ttf").also { roboto = it }
        AdaptyUIWeb.registerFonts(bytes)
        return resolveFontFamily(font(weight), PlatformContextHandle())
    }

    private fun robotoMetrics(sizePx: Float): Pair<Float, Float> {
        val typeface = FontMgr.default.makeFromData(Data.makeFromBytes(roboto ?: fail("Roboto not loaded"))) ?: fail("Roboto must parse")
        val m = Font(typeface, sizePx).metrics
        return m.ascent to m.descent
    }
}

private fun font(weight: Int) = Asset.Font("adapty_system", emptyList(), weight, false, 15f, null, null, null)

private fun measurer() = TextMeasurer(createFontFamilyResolver(), Density(2f), LayoutDirection.Ltr)

private fun TextLayoutResult.lines(): List<String> =
    (0 until lineCount).map { layoutInput.text.text.substring(getLineStart(it), getLineEnd(it)) }
