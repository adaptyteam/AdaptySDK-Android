package com.adapty.ui.internal.text

import com.adapty.ui.internal.platform.BigDecimalLike
import com.adapty.ui.internal.platform.PlatformDate
import com.adapty.ui.internal.platform.PlatformLocale
import com.adapty.ui.internal.platform.PlatformTimeZone

// Locale/number/date formatting used by shared code.
internal expect object PlatformFormat {
    fun formatString(format: String, vararg args: Any?): String
    fun decimalSeparator(locale: PlatformLocale): Char

    val STYLE_SHORT: Int
    val STYLE_MEDIUM: Int
    val STYLE_LONG: Int
    val STYLE_FULL: Int

    /** Styles are STYLE_* or TagConverter.DateTime.STYLE_NONE (-1, "no date"/"no time"). */
    fun formatDateTime(date: PlatformDate, pattern: String?, dateStyle: Int, timeStyle: Int, locale: PlatformLocale): String?

    fun titlecase(char: Char, locale: PlatformLocale): String

    fun divide(amount: BigDecimalLike, divisor: Int): BigDecimalLike
    fun multiply(amount: BigDecimalLike, multiplier: Int): BigDecimalLike
    fun currencyFractionDigits(currencyCode: String): Int
    fun formatPriceAmount(amount: BigDecimalLike, fractionDigits: Int, locale: PlatformLocale): String
    fun formatPriceAmountInteger(amount: BigDecimalLike, locale: PlatformLocale): String
    fun formatPriceAmountFraction(amount: BigDecimalLike, fractionDigits: Int): String

    fun uriHasScheme(url: String): Boolean

    fun parseTimeZone(value: String): PlatformTimeZone
    fun defaultTimeZone(): PlatformTimeZone
    fun utcTimeZone(): PlatformTimeZone
    fun calendarAnchorMillis(anchor: String, tz: PlatformTimeZone): Long?
    fun applyIntervalComponents(components: Map<*, *>, baseMs: Long, timeZone: PlatformTimeZone): Long

    /** [month] is 1-based. */
    fun dateComponentsToMillis(timeZone: PlatformTimeZone, year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long

    // DateTimePickerElement calendar model (default time zone, month 0-based)
    fun localDateFields(ms: Long): LocalDateFields
    fun shortMonthNames(locale: PlatformLocale): List<String>
    fun startOfDayUtc(ms: Long): Long
    fun yearOf(ms: Long): Int
    fun combineDateAndTime(dateMs: Long, currentMs: Long): Long
    fun replaceTime(currentMs: Long, hour: Int, minute: Int): Long
    fun buildDateMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long
    fun daysInMonth(year: Int, month: Int): Int
    fun formatPickerDate(ms: Long): String
    fun formatPickerTime(ms: Long): String
}
