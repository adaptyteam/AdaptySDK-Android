package com.adapty.ui.internal.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis

/**
 * Pins emoji-presentation runs to [emoji]. Android (Minikin) takes the color emoji font for them although the
 * symbol fonts earlier in the fallback chain cover many of the same code points; Skia's fallback only follows the
 * registration order, so the runs get an explicit font family.
 */
internal fun AnnotatedString.withEmojiPresentation(emoji: FontFamily?): AnnotatedString {
    if (emoji == null) return this
    val runs = emojiRuns(text)
    if (runs.isEmpty()) return this
    val style = SpanStyle(fontFamily = emoji, fontSynthesis = FontSynthesis.None)
    return AnnotatedString.Builder(this).apply {
        runs.forEach { addStyle(style, it.first, it.last + 1) }
    }.toAnnotatedString()
}

/**
 * Char ranges rendered with emoji presentation: a code point with Emoji_Presentation=Yes, followed by VS16
 * (U+FE0F) or by a skin-tone modifier (☝🏽), or a keycap base followed by U+20E3, together with its sequence
 * (VS16, keycap, skin tones, tags, ZWJ-joined code points). VS15 (U+FE0E) keeps text presentation.
 * Adjacent runs are merged.
 */
internal fun emojiRuns(text: String): List<IntRange> {
    val runs = ArrayList<IntRange>()
    var i = 0
    while (i < text.length) {
        val cp = codePointAt(text, i)
        val next = i + charCount(cp)
        val following = if (next < text.length) codePointAt(text, next) else -1
        val starts = following != VS15 &&
            (isEmojiPresentation(cp) || following == VS16 || following in SKIN_TONES || (following == KEYCAP && isKeycapBase(cp)))
        if (!starts) {
            i = next
            continue
        }
        var end = next
        while (end < text.length) {
            val c = codePointAt(text, end)
            end = when {
                c == VS16 || c == KEYCAP || c in SKIN_TONES || c in TAGS -> end + charCount(c)
                c == ZWJ && end + 1 < text.length -> (end + 1).let { it + charCount(codePointAt(text, it)) }
                // A flag is a pair of regional indicators
                isRegionalIndicator(cp) && end == next && isRegionalIndicator(c) -> end + charCount(c)
                else -> break
            }
        }
        val last = runs.lastOrNull()
        if (last != null && last.last + 1 == i) runs[runs.lastIndex] = last.first until end else runs += i until end
        i = end
    }
    return runs
}

internal fun isEmojiPresentation(cp: Int): Boolean {
    var lo = 0
    var hi = EMOJI_PRESENTATION.size / 2 - 1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        when {
            cp < EMOJI_PRESENTATION[mid * 2] -> hi = mid - 1
            cp > EMOJI_PRESENTATION[mid * 2 + 1] -> lo = mid + 1
            else -> return true
        }
    }
    return false
}

private fun isKeycapBase(cp: Int) = cp in '0'.code..'9'.code || cp == '#'.code || cp == '*'.code

private fun isRegionalIndicator(cp: Int) = cp in 0x1F1E6..0x1F1FF

private fun codePointAt(s: String, i: Int): Int {
    val c = s[i]
    if (c.isHighSurrogate() && i + 1 < s.length && s[i + 1].isLowSurrogate()) {
        return 0x10000 + ((c.code - 0xD800) shl 10) + (s[i + 1].code - 0xDC00)
    }
    return c.code
}

private fun charCount(cp: Int) = if (cp >= 0x10000) 2 else 1

private const val VS15 = 0xFE0E
private const val VS16 = 0xFE0F
private const val ZWJ = 0x200D
private const val KEYCAP = 0x20E3
private val SKIN_TONES = 0x1F3FB..0x1F3FF
private val TAGS = 0xE0020..0xE007F

// Emoji_Presentation=Yes (Unicode 16.0 emoji-data.txt), inclusive [start, end] pairs
private val EMOJI_PRESENTATION = intArrayOf(
    0x231A, 0x231B, 0x23E9, 0x23EC, 0x23F0, 0x23F0, 0x23F3, 0x23F3, 0x25FD, 0x25FE, 0x2614, 0x2615,
    0x2648, 0x2653, 0x267F, 0x267F, 0x2693, 0x2693, 0x26A1, 0x26A1, 0x26AA, 0x26AB, 0x26BD, 0x26BE,
    0x26C4, 0x26C5, 0x26CE, 0x26CE, 0x26D4, 0x26D4, 0x26EA, 0x26EA, 0x26F2, 0x26F3, 0x26F5, 0x26F5,
    0x26FA, 0x26FA, 0x26FD, 0x26FD, 0x2705, 0x2705, 0x270A, 0x270B, 0x2728, 0x2728, 0x274C, 0x274C,
    0x274E, 0x274E, 0x2753, 0x2755, 0x2757, 0x2757, 0x2795, 0x2797, 0x27B0, 0x27B0, 0x27BF, 0x27BF,
    0x2B1B, 0x2B1C, 0x2B50, 0x2B50, 0x2B55, 0x2B55, 0x1F004, 0x1F004, 0x1F0CF, 0x1F0CF, 0x1F18E, 0x1F18E,
    0x1F191, 0x1F19A, 0x1F1E6, 0x1F1FF, 0x1F201, 0x1F201, 0x1F21A, 0x1F21A, 0x1F22F, 0x1F22F, 0x1F232, 0x1F236,
    0x1F238, 0x1F23A, 0x1F250, 0x1F251, 0x1F300, 0x1F320, 0x1F32D, 0x1F335, 0x1F337, 0x1F37C, 0x1F37E, 0x1F393,
    0x1F3A0, 0x1F3CA, 0x1F3CF, 0x1F3D3, 0x1F3E0, 0x1F3F0, 0x1F3F4, 0x1F3F4, 0x1F3F8, 0x1F43E, 0x1F440, 0x1F440,
    0x1F442, 0x1F4FC, 0x1F4FF, 0x1F53D, 0x1F54B, 0x1F54E, 0x1F550, 0x1F567, 0x1F57A, 0x1F57A, 0x1F595, 0x1F596,
    0x1F5A4, 0x1F5A4, 0x1F5FB, 0x1F64F, 0x1F680, 0x1F6C5, 0x1F6CC, 0x1F6CC, 0x1F6D0, 0x1F6D2, 0x1F6D5, 0x1F6D7,
    0x1F6DC, 0x1F6DF, 0x1F6EB, 0x1F6EC, 0x1F6F4, 0x1F6FC, 0x1F7E0, 0x1F7EB, 0x1F7F0, 0x1F7F0, 0x1F90C, 0x1F93A,
    0x1F93C, 0x1F945, 0x1F947, 0x1F9FF, 0x1FA70, 0x1FA7C, 0x1FA80, 0x1FA89, 0x1FA8F, 0x1FAC6, 0x1FACE, 0x1FADC,
    0x1FADF, 0x1FAE9, 0x1FAF0, 0x1FAF8,
)
