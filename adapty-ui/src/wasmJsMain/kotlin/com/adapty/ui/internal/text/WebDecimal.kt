package com.adapty.ui.internal.text

import com.adapty.ui.internal.platform.BigDecimalLike

// Decimal rounding for the Double-backed BigDecimalLike: rounds the shortest round-trip decimal string,
// so values like 79.99 behave like java.math.BigDecimal("79.99") instead of their binary expansion.

internal enum class WebRounding { CEILING, DOWN, HALF_UP, HALF_EVEN }

/** Shortest round-trip decimal of [value] without exponent, e.g. "79.99", "-0.5", "80". */
internal fun plainDecimal(value: Double): String = plainDecimalJs(value)

/** Number of fraction digits of [value]'s shortest decimal (BigDecimal scale of a Gson-parsed price). */
internal fun decimalScale(value: Double): Int = plainDecimal(value).substringAfter('.', "").length

/** BigDecimal.setScale(scale, mode) on the shortest decimal of [value]. */
internal fun roundDecimal(value: Double, scale: Int, mode: WebRounding): Double =
    roundDecimalString(plainDecimal(value), scale, mode).toDouble()

internal fun roundDecimalString(decimal: String, scale: Int, mode: WebRounding): String {
    val negative = decimal.startsWith("-")
    val abs = decimal.removePrefix("-")
    val intPart = abs.substringBefore('.')
    val fracPart = abs.substringAfter('.', "")
    if (fracPart.length <= scale) return decimal
    val kept = fracPart.substring(0, scale)
    val dropped = fracPart.substring(scale)
    val nonZeroDropped = dropped.any { it != '0' }
    val lastKeptDigit = (intPart + kept).last().digitToInt()
    val roundUp = when (mode) {
        WebRounding.DOWN -> false
        WebRounding.CEILING -> !negative && nonZeroDropped
        WebRounding.HALF_UP -> dropped[0] >= '5'
        WebRounding.HALF_EVEN -> when {
            dropped[0] > '5' -> true
            dropped[0] < '5' -> false
            dropped.drop(1).any { it != '0' } -> true
            else -> lastKeptDigit % 2 == 1
        }
    }
    var digits = (intPart + kept).map { it.digitToInt() }.toMutableList()
    if (roundUp) {
        var k = digits.lastIndex
        while (k >= 0) {
            if (digits[k] == 9) {
                digits[k] = 0
                k--
            } else {
                digits[k]++
                break
            }
        }
        if (k < 0) digits = (listOf(1) + digits).toMutableList()
    }
    val all = digits.joinToString("")
    val newInt = all.substring(0, all.length - scale).ifEmpty { "0" }
    val newFrac = all.substring(all.length - scale)
    val body = if (scale > 0) "$newInt.$newFrac" else newInt
    return if (negative && body.any { it in '1'..'9' }) "-$body" else body
}

internal fun BigDecimalLike.isIntegral(): Boolean = plainDecimal(toDouble()).substringAfter('.', "").all { it == '0' }

@JsFun("(v) => { const s = String(v); return /e/i.test(s) ? v.toLocaleString('en-US', { useGrouping: false, maximumFractionDigits: 20 }) : s; }")
private external fun plainDecimalJs(value: Double): String

/** Intl.NumberFormat(tag) with grouping and fixed fraction bounds; [value] is already rounded by the caller. */
@JsFun("(v, tag, minF, maxF) => { try { return new Intl.NumberFormat(tag, { useGrouping: true, minimumFractionDigits: minF, maximumFractionDigits: maxF }).format(v); } catch (e) { return new Intl.NumberFormat('en-US', { useGrouping: true, minimumFractionDigits: minF, maximumFractionDigits: maxF }).format(v); } }")
internal external fun intlFormatNumber(value: Double, tag: String, minFraction: Int, maxFraction: Int): String

@JsFun("(code) => { try { return new Intl.NumberFormat('en-US', { style: 'currency', currency: code }).resolvedOptions().maximumFractionDigits; } catch (e) { return -1; } }")
internal external fun intlCurrencyFractionDigits(code: String): Int
