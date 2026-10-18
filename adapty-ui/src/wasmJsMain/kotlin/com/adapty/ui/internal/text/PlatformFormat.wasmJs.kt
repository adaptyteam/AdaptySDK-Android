package com.adapty.ui.internal.text

import com.adapty.ui.internal.platform.BigDecimalLike
import com.adapty.ui.internal.platform.PlatformDate
import com.adapty.ui.internal.platform.PlatformLocale
import com.adapty.ui.internal.platform.PlatformTimeZone
import com.adapty.ui.internal.platform.webStub
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

private object WebTimeZone : PlatformTimeZone()

internal actual object PlatformFormat {
    actual fun formatString(format: String, vararg args: Any?): String {
        webStub("PlatformFormat.formatString")
        return format
    }

    actual fun decimalSeparator(locale: PlatformLocale): Char {
        webStub("PlatformFormat.decimalSeparator")
        return '.'
    }

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

    // amount.divide(divisor, 4, CEILING)
    actual fun divide(amount: BigDecimalLike, divisor: Int): BigDecimalLike =
        BigDecimalLike(ceil(amount.toDouble() / divisor * 10_000.0) / 10_000.0)

    actual fun multiply(amount: BigDecimalLike, multiplier: Int): BigDecimalLike =
        BigDecimalLike(amount.toDouble() * multiplier)

    actual fun currencyFractionDigits(currencyCode: String): Int {
        webStub("PlatformFormat.currencyFractionDigits")
        return 2
    }

    actual fun formatPriceAmount(amount: BigDecimalLike, fractionDigits: Int, locale: PlatformLocale): String {
        webStub("PlatformFormat.formatPriceAmount")
        return amount.toDouble().toString()
    }

    actual fun formatPriceAmountInteger(amount: BigDecimalLike, locale: PlatformLocale): String {
        webStub("PlatformFormat.formatPriceAmountInteger")
        return floor(amount.toDouble()).toLong().toString()
    }

    actual fun formatPriceAmountFraction(amount: BigDecimalLike, fractionDigits: Int): String {
        webStub("PlatformFormat.formatPriceAmountFraction")
        val scaled = (amount.toDouble() * 10.0.pow(fractionDigits)).toLong()
        return (scaled % 10.0.pow(fractionDigits).toLong()).toString().padStart(fractionDigits, '0')
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
