package com.adapty.ui.internal.text

import com.adapty.ui.internal.platform.BigDecimalLike
import com.adapty.ui.internal.platform.PlatformDate
import com.adapty.ui.internal.platform.PlatformLocale
import com.adapty.ui.internal.platform.PlatformTimeZone
import com.adapty.ui.internal.platform.RoundingMode
import com.adapty.ui.internal.platform.bigDecimalTen
import com.adapty.ui.internal.platform.defaultLocale
import com.adapty.ui.internal.platform.platformDate
import java.net.URI
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.TimeZone

// Locale/number/date formatting used by shared code; Android bodies moved verbatim from the callers.
internal actual object PlatformFormat {

    // TagConverter / SDKGlobals: String.format

    actual fun formatString(format: String, vararg args: Any?): String = String.format(format, *args)

    actual fun decimalSeparator(locale: PlatformLocale): Char =
        DecimalFormatSymbols.getInstance(locale).decimalSeparator

    actual val STYLE_SHORT: Int get() = DateFormat.SHORT
    actual val STYLE_MEDIUM: Int get() = DateFormat.MEDIUM
    actual val STYLE_LONG: Int get() = DateFormat.LONG
    actual val STYLE_FULL: Int get() = DateFormat.FULL

    // TagConverter.DateTime: buildFormatter + format; styles are STYLE_* or TagConverter.DateTime.STYLE_NONE
    actual fun formatDateTime(
        date: PlatformDate,
        pattern: String?,
        dateStyle: Int,
        timeStyle: Int,
        locale: PlatformLocale,
    ): String? {
        val formatter = buildDateFormatter(pattern, dateStyle, timeStyle, locale) ?: return null
        return runCatching { formatter.format(date) }.getOrNull()
    }

    private fun buildDateFormatter(pattern: String?, dateStyle: Int, timeStyle: Int, locale: PlatformLocale): DateFormat? {
        if (pattern != null) {
            return runCatching { SimpleDateFormat(pattern, locale) }.getOrNull()
        }
        return when {
            dateStyle == TagConverter.DateTime.STYLE_NONE -> DateFormat.getTimeInstance(timeStyle, locale)
            timeStyle == TagConverter.DateTime.STYLE_NONE -> DateFormat.getDateInstance(dateStyle, locale)
            else -> DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale)
        }
    }

    // SDKGlobals: Char.titlecase(Locale)

    actual fun titlecase(char: Char, locale: PlatformLocale): String = char.titlecase(locale)

    // PriceConverter / TagResolver: BigDecimal arithmetic

    actual fun divide(amount: BigDecimalLike, divisor: Int): BigDecimalLike =
        amount.divide(divisor.toBigDecimal(), 4, RoundingMode.CEILING)

    actual fun multiply(amount: BigDecimalLike, multiplier: Int): BigDecimalLike =
        amount * multiplier.toBigDecimal()

    // TagResolver

    actual fun currencyFractionDigits(currencyCode: String): Int {
        return try {
            Currency.getInstance(currencyCode).defaultFractionDigits
        } catch (_: Exception) {
            2
        }
    }

    actual fun formatPriceAmount(amount: BigDecimalLike, fractionDigits: Int, locale: PlatformLocale): String {
        val formatter = NumberFormat.getInstance(locale)
        formatter.minimumFractionDigits = fractionDigits
        formatter.maximumFractionDigits = fractionDigits
        formatter.isGroupingUsed = true
        return formatter.format(amount)
    }

    actual fun formatPriceAmountInteger(amount: BigDecimalLike, locale: PlatformLocale): String {
        val formatter = NumberFormat.getInstance(locale)
        formatter.maximumFractionDigits = 0
        formatter.roundingMode = RoundingMode.DOWN
        formatter.isGroupingUsed = true
        return formatter.format(amount)
    }

    actual fun formatPriceAmountFraction(amount: BigDecimalLike, fractionDigits: Int): String {
        val multiplier = bigDecimalTen().pow(fractionDigits)
        val scaled = amount.multiply(multiplier)
            .setScale(0, RoundingMode.HALF_UP)
            .toBigInteger()
            .abs()
        val modulus = multiplier.toBigInteger()
        val fraction = scaled.mod(modulus)
        return fraction.toString().padStart(fractionDigits, '0')
    }

    // ConfigNormalizer

    actual fun uriHasScheme(url: String): Boolean =
        runCatching { URI(url).scheme != null }.getOrDefault(false)

    // DateTimeMapping / ConfigNormalizer: time zones and calendar math

    actual fun parseTimeZone(value: String): PlatformTimeZone {
        if (value == "UTC") return TimeZone.getTimeZone("UTC")
        if (value.isNotEmpty() && (value[0] == '+' || value[0] == '-')) {
            return TimeZone.getTimeZone("GMT$value")
        }
        return TimeZone.getTimeZone(value)
    }

    actual fun defaultTimeZone(): PlatformTimeZone = TimeZone.getDefault()

    actual fun utcTimeZone(): PlatformTimeZone = TimeZone.getTimeZone("UTC")

    // start_of_day / start_of_week / start_of_month / start_of_year; null for other anchors
    actual fun calendarAnchorMillis(anchor: String, tz: PlatformTimeZone): Long? = when (anchor) {
        "start_of_day" -> {
            Calendar.getInstance(tz).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        "start_of_week" -> {
            Calendar.getInstance(tz).apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        "start_of_month" -> {
            Calendar.getInstance(tz).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        "start_of_year" -> {
            Calendar.getInstance(tz).apply {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        else -> null
    }

    // Adds the non-zero year/month/day/hour/minute/second entries of [components] to [baseMs]
    actual fun applyIntervalComponents(components: Map<*, *>, baseMs: Long, timeZone: PlatformTimeZone): Long {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = baseMs }
        (components["year"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.YEAR, it) }
        (components["month"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.MONTH, it) }
        (components["day"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.DAY_OF_MONTH, it) }
        (components["hour"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.HOUR_OF_DAY, it) }
        (components["minute"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.MINUTE, it) }
        (components["second"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.SECOND, it) }
        return cal.timeInMillis
    }

    // month is 1-based
    actual fun dateComponentsToMillis(
        timeZone: PlatformTimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
    ): Long =
        Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // DateTimePickerElement calendar model (default time zone unless noted; month is 0-based)

    actual fun localDateFields(ms: Long): LocalDateFields {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        return LocalDateFields(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH),
            day = cal.get(Calendar.DAY_OF_MONTH),
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
        )
    }

    actual fun shortMonthNames(locale: PlatformLocale): List<String> {
        val symbols = DateFormatSymbols.getInstance(locale)
        return symbols.shortMonths.take(12)
    }

    actual fun startOfDayUtc(ms: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    actual fun yearOf(ms: Long): Int =
        Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.YEAR)

    // dateMs is a UTC-midnight date from the Material date picker
    actual fun combineDateAndTime(dateMs: Long, currentMs: Long): Long {
        val datePart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateMs }
        val cal = Calendar.getInstance().apply { timeInMillis = currentMs }
        cal.set(Calendar.YEAR, datePart.get(Calendar.YEAR))
        cal.set(Calendar.MONTH, datePart.get(Calendar.MONTH))
        cal.set(Calendar.DAY_OF_MONTH, datePart.get(Calendar.DAY_OF_MONTH))
        return cal.timeInMillis
    }

    actual fun replaceTime(currentMs: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMs }
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    actual fun buildDateMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val safeDay = day.coerceIn(1, daysInMonth(year, month))
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, safeDay)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    actual fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    actual fun formatPickerDate(ms: Long): String =
        SimpleDateFormat("MMM d, yyyy", defaultLocale()).format(platformDate(ms))

    actual fun formatPickerTime(ms: Long): String =
        SimpleDateFormat("h:mm a", defaultLocale()).format(platformDate(ms))
}
