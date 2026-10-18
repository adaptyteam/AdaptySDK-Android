package com.adapty.ui.internal.text

import com.adapty.ui.internal.platform.BigDecimalLike
import com.adapty.ui.internal.platform.PlatformDate
import com.adapty.ui.internal.platform.PlatformLocale
import com.adapty.ui.internal.platform.PlatformTimeZone
import com.adapty.ui.internal.platform.country
import com.adapty.ui.internal.platform.language
import com.adapty.ui.internal.platform.webStub

private object WebTimeZone : PlatformTimeZone()

internal actual object PlatformFormat {
    /** java.util.Formatter subset: `%[flags][width][.precision](d|x|X|f|s)` and `%%`; flags `0 - +`. */
    actual fun formatString(format: String, vararg args: Any?): String = formatPrintf(format, args)

    // formatString always writes '.' (Locale.US-like); callers swap it via applyDecimalSeparator(locale)
    actual fun decimalSeparator(locale: PlatformLocale): Char = decimalSeparatorOf(localeTag(locale)).firstOrNull() ?: '.'

    // java.text.DateFormat values
    actual val STYLE_SHORT: Int get() = 3
    actual val STYLE_MEDIUM: Int get() = 2
    actual val STYLE_LONG: Int get() = 1
    actual val STYLE_FULL: Int get() = 0

    actual fun formatDateTime(date: PlatformDate, pattern: String?, dateStyle: Int, timeStyle: Int, locale: PlatformLocale): String? {
        webStub("PlatformFormat.formatDateTime")
        return null
    }

    actual fun titlecase(char: Char, locale: PlatformLocale): String = char.titlecase()

    // amount.divide(divisor, 4, CEILING), exact on the amount's decimal (Double fallback only on Long overflow)
    actual fun divide(amount: BigDecimalLike, divisor: Int): BigDecimalLike =
        BigDecimalLike(
            divideCeilingExact(amount.toDouble(), divisor, 4)
                ?: roundDecimal(amount.toDouble() / divisor, 4, WebRounding.CEILING),
        )

    // amount * multiplier, exact on the amount's decimal (keeps its scale)
    actual fun multiply(amount: BigDecimalLike, multiplier: Int): BigDecimalLike =
        BigDecimalLike(
            multiplyExact(amount.toDouble(), multiplier)
                ?: roundDecimal(amount.toDouble() * multiplier, decimalScale(amount.toDouble()), WebRounding.HALF_UP),
        )

    // java.util.Currency.defaultFractionDigits via ICU (Intl); unknown codes -> 2 like the Android catch
    actual fun currencyFractionDigits(currencyCode: String): Int =
        intlCurrencyFractionDigits(currencyCode).takeIf { it >= 0 } ?: 2

    // NumberFormat(locale): min = max = fractionDigits, HALF_EVEN, grouping
    actual fun formatPriceAmount(amount: BigDecimalLike, fractionDigits: Int, locale: PlatformLocale): String =
        intlFormatNumber(roundDecimal(amount.toDouble(), fractionDigits, WebRounding.HALF_EVEN), localeTag(locale), fractionDigits, fractionDigits)

    // NumberFormat(locale): no fraction, RoundingMode.DOWN, grouping
    actual fun formatPriceAmountInteger(amount: BigDecimalLike, locale: PlatformLocale): String =
        intlFormatNumber(roundDecimal(amount.toDouble(), 0, WebRounding.DOWN), localeTag(locale), 0, 0)

    // |amount| * 10^fractionDigits, HALF_UP, mod 10^fractionDigits, zero-padded
    actual fun formatPriceAmountFraction(amount: BigDecimalLike, fractionDigits: Int): String {
        val rounded = roundDecimalString(plainDecimal(amount.toDouble()).removePrefix("-"), fractionDigits, WebRounding.HALF_UP)
        return rounded.substringAfter('.', "").padEnd(fractionDigits, '0').take(fractionDigits)
    }

    // java.net.URI scheme: ALPHA *( ALPHA / DIGIT / "+" / "-" / "." ) ":"
    actual fun uriHasScheme(url: String): Boolean = Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(url)

    actual fun parseTimeZone(value: String): PlatformTimeZone {
        webStub("PlatformFormat.parseTimeZone")
        return WebTimeZone
    }

    actual fun defaultTimeZone(): PlatformTimeZone {
        webStub("PlatformFormat.defaultTimeZone")
        return WebTimeZone
    }

    actual fun utcTimeZone(): PlatformTimeZone {
        webStub("PlatformFormat.utcTimeZone")
        return WebTimeZone
    }

    actual fun calendarAnchorMillis(anchor: String, tz: PlatformTimeZone): Long? {
        webStub("PlatformFormat.calendarAnchorMillis")
        return null
    }

    actual fun applyIntervalComponents(components: Map<*, *>, baseMs: Long, timeZone: PlatformTimeZone): Long {
        webStub("PlatformFormat.applyIntervalComponents")
        return baseMs
    }

    actual fun dateComponentsToMillis(timeZone: PlatformTimeZone, year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        webStub("PlatformFormat.dateComponentsToMillis")
        return 0L
    }

    actual fun localDateFields(ms: Long): LocalDateFields {
        webStub("PlatformFormat.localDateFields")
        return LocalDateFields(1970, 0, 1, 0, 0)
    }

    actual fun shortMonthNames(locale: PlatformLocale): List<String> {
        webStub("PlatformFormat.shortMonthNames")
        return listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    actual fun startOfDayUtc(ms: Long): Long {
        webStub("PlatformFormat.startOfDayUtc")
        return ms - ms.mod(86_400_000L)
    }

    actual fun yearOf(ms: Long): Int {
        webStub("PlatformFormat.yearOf")
        return 1970
    }

    actual fun combineDateAndTime(dateMs: Long, currentMs: Long): Long {
        webStub("PlatformFormat.combineDateAndTime")
        return dateMs
    }

    actual fun replaceTime(currentMs: Long, hour: Int, minute: Int): Long {
        webStub("PlatformFormat.replaceTime")
        return currentMs
    }

    actual fun buildDateMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        webStub("PlatformFormat.buildDateMs")
        return 0L
    }

    actual fun daysInMonth(year: Int, month: Int): Int {
        webStub("PlatformFormat.daysInMonth")
        return 31
    }

    actual fun formatPickerDate(ms: Long): String {
        webStub("PlatformFormat.formatPickerDate")
        return ""
    }

    actual fun formatPickerTime(ms: Long): String {
        webStub("PlatformFormat.formatPickerTime")
        return ""
    }
}

private fun localeTag(locale: PlatformLocale): String =
    if (locale.country.isEmpty()) locale.language else "${locale.language}-${locale.country}"

@JsFun("(tag) => { try { const p = new Intl.NumberFormat(tag).formatToParts(1.5).find(x => x.type === 'decimal'); return p ? p.value : '.'; } catch (e) { return '.'; } }")
private external fun decimalSeparatorOf(tag: String): String

private val printfSpec = Regex("%([-0+ ]*)(\\d+)?(?:\\.(\\d+))?([dxXfs%])")

private fun formatPrintf(format: String, args: Array<out Any?>): String {
    var argIndex = 0
    return printfSpec.replace(format) { m ->
        val conv = m.groupValues[4]
        if (conv == "%") return@replace "%"
        val flags = m.groupValues[1]
        val width = m.groupValues[2].toIntOrNull() ?: 0
        val precision = m.groupValues[3].toIntOrNull()
        val arg = args.getOrNull(argIndex++) ?: throw IllegalArgumentException("Missing format argument for ${m.value}")
        val body = when (conv) {
            "d" -> (arg as? Number ?: throw IllegalArgumentException("%d needs a number")).toLong().toString()
            "x", "X" -> {
                val n = (arg as? Number ?: throw IllegalArgumentException("%x needs a number"))
                val hex = when (n) {
                    is Int -> n.toUInt().toString(16)
                    is Short -> n.toUShort().toString(16)
                    is Byte -> n.toUByte().toString(16)
                    else -> n.toLong().toULong().toString(16)
                }
                if (conv == "X") hex.uppercase() else hex
            }
            "f" -> formatFixed((arg as? Number ?: throw IllegalArgumentException("%f needs a number")).toDouble(), precision ?: 6)
            else -> arg.toString().let { if (precision != null) it.take(precision) else it }
        }
        val signed = if (conv in "df" && !body.startsWith("-")) when {
            '+' in flags -> "+$body"
            ' ' in flags -> " $body"
            else -> body
        } else body
        when {
            signed.length >= width -> signed
            '-' in flags -> signed.padEnd(width)
            '0' in flags && conv != "s" -> {
                val sign = signed.takeWhile { it == '-' || it == '+' || it == ' ' }
                sign + signed.drop(sign.length).padStart(width - sign.length, '0')
            }
            else -> signed.padStart(width)
        }
    }
}

/** HALF_UP fixed-point like java.util.Formatter %f. */
private fun formatFixed(value: Double, precision: Int): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
    return toFixedHalfUp(value, precision)
}

@JsFun("""(v, p) => {
    const neg = v < 0 || Object.is(v, -0);
    let [i, f = ''] = Math.abs(v).toLocaleString('en-US', { useGrouping: false, maximumFractionDigits: 20 }).split('.');
    if (f.length > p) {
        const round = f.charCodeAt(p) >= 53;
        f = f.slice(0, p);
        if (round) {
            const digits = (i + f).split('').map(Number);
            let k = digits.length - 1;
            while (k >= 0) { if (digits[k] === 9) { digits[k] = 0; k--; } else { digits[k]++; break; } }
            if (k < 0) digits.unshift(1);
            const all = digits.join('');
            i = all.slice(0, all.length - p) || '0';
            f = all.slice(all.length - p);
        }
    } else {
        f = f.padEnd(p, '0');
    }
    const r = p > 0 ? i + '.' + f : i;
    return neg && /[1-9]/.test(r) ? '-' + r : r;
}""")
private external fun toFixedHalfUp(value: Double, precision: Int): String
