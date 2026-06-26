@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.text

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import java.text.DateFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NUMBER_FORMAT_REGEX = Regex("^%[+\\-0 ]*(\\d+)?(\\.\\d+)?[df]$")
private val INTEGER_FORMAT_REGEX = Regex("^%[+\\-0 ]*\\d*d$")
private const val DEFAULT_NUMBER_FORMAT = "%d"
private const val DEFAULT_TIMER_COMPONENT_FORMAT = "%01d"
private const val DEFAULT_TIMER_TOTAL_FORMAT = "%d"

internal const val CONVERTER_PARAM_FORMAT = "format"
internal const val CONVERTER_PARAM_DATE_STYLE = "date_style"
internal const val CONVERTER_PARAM_TIME_STYLE = "time_style"

internal data class ConverterSpec(
    val name: String,
    val params: Map<String, Any?>? = null,
)

internal sealed class TagConverter {

    abstract fun format(value: Any, locale: Locale): String?

    class Percent(private val format: String) : TagConverter() {
        override fun format(value: Any, locale: Locale): String? {
            val raw = value.coerceToDouble() ?: return null
            val percent = raw.coerceIn(0.0, 1.0) * 100.0
            return runCatching {
                if (format.endsWith("d")) {
                    String.format(format, percent.toLong())
                } else {
                    String.format(format, percent).applyDecimalSeparator(locale)
                }
            }.getOrNull()
        }
    }

    class Number(private val format: String) : TagConverter() {
        override fun format(value: Any, locale: Locale): String? {
            val raw = value.coerceToDouble() ?: return null
            return runCatching {
                if (format.endsWith("d")) {
                    String.format(format, raw.toLong())
                } else {
                    String.format(format, raw).applyDecimalSeparator(locale)
                }
            }.getOrNull()
        }
    }

    class DateTime private constructor(
        private val pattern: String?,
        private val dateStyle: Int,
        private val timeStyle: Int,
    ) : TagConverter() {

        override fun format(value: Any, locale: Locale): String? {
            val date = when (value) {
                is Date -> value
                is Boolean -> return null
                is kotlin.Number -> Date(value.toLong())
                else -> return null
            }
            if (pattern == null && dateStyle == STYLE_NONE && timeStyle == STYLE_NONE) return ""
            val formatter = buildFormatter(locale) ?: return null
            return runCatching { formatter.format(date) }.getOrNull()
        }

        private fun buildFormatter(locale: Locale): DateFormat? {
            if (pattern != null) {
                return runCatching { SimpleDateFormat(pattern, locale) }.getOrNull()
            }
            return when {
                dateStyle == STYLE_NONE -> DateFormat.getTimeInstance(timeStyle, locale)
                timeStyle == STYLE_NONE -> DateFormat.getDateInstance(dateStyle, locale)
                else -> DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale)
            }
        }

        internal companion object Factory {
            internal const val STYLE_NONE = -1

            fun fromParams(params: Map<String, Any?>?): DateTime {
                val pattern = params?.get(CONVERTER_PARAM_FORMAT) as? String
                if (pattern != null) {
                    return DateTime(pattern, STYLE_NONE, STYLE_NONE)
                }
                return DateTime(
                    pattern = null,
                    dateStyle = parseStyle(params?.get(CONVERTER_PARAM_DATE_STYLE) as? String),
                    timeStyle = parseStyle(params?.get(CONVERTER_PARAM_TIME_STYLE) as? String),
                )
            }

            private fun parseStyle(s: String?): Int = when (s) {
                "short" -> DateFormat.SHORT
                "medium" -> DateFormat.MEDIUM
                "long" -> DateFormat.LONG
                "full" -> DateFormat.FULL
                null, "none" -> STYLE_NONE
                else -> {
                    log(WARN) { "$LOG_PREFIX Unknown date/time style: \"$s\", falling back to \"none\"" }
                    STYLE_NONE
                }
            }
        }
    }

    sealed class Timer(val updatesPerSecond: Int) : TagConverter() {
        final override fun format(value: Any, locale: Locale): String? {
            if (value is Boolean) return null
            val seconds = (value as? kotlin.Number)?.toDouble()?.coerceAtLeast(0.0) ?: return null
            return runCatching { formatSeconds(seconds) }.getOrNull()
        }

        protected abstract fun formatSeconds(seconds: Double): String

        class Days(private val format: String) : Timer(updatesPerSecond = 1) {
            override fun formatSeconds(seconds: Double): String =
                String.format(format, seconds.toLong() / SECONDS_PER_DAY)
        }

        class Hours(private val format: String) : Timer(updatesPerSecond = 1) {
            override fun formatSeconds(seconds: Double): String =
                String.format(format, seconds.toLong() % SECONDS_PER_DAY / SECONDS_PER_HOUR)
        }

        class Minutes(private val format: String) : Timer(updatesPerSecond = 1) {
            override fun formatSeconds(seconds: Double): String =
                String.format(format, seconds.toLong() % SECONDS_PER_HOUR / SECONDS_PER_MINUTE)
        }

        class Seconds(private val format: String) : Timer(updatesPerSecond = 1) {
            override fun formatSeconds(seconds: Double): String =
                String.format(format, seconds.toLong() % SECONDS_PER_MINUTE)
        }

        object Deciseconds : Timer(updatesPerSecond = 10) {
            override fun formatSeconds(seconds: Double): String =
                String.format("%d", (seconds * 10).toLong() % 10)
        }

        object Centiseconds : Timer(updatesPerSecond = 100) {
            override fun formatSeconds(seconds: Double): String =
                String.format("%02d", (seconds * 100).toLong() % 100)
        }

        object Milliseconds : Timer(updatesPerSecond = 120) {
            override fun formatSeconds(seconds: Double): String =
                String.format("%03d", (seconds * 1000).toLong() % 1000)
        }

        class TotalDays(private val format: String) : Timer(updatesPerSecond = 1) {
            override fun formatSeconds(seconds: Double): String =
                String.format(format, (seconds / SECONDS_PER_DAY).toLong())
        }

        class TotalHours(private val format: String) : Timer(updatesPerSecond = 1) {
            override fun formatSeconds(seconds: Double): String =
                String.format(format, (seconds / SECONDS_PER_HOUR).toLong())
        }

        class TotalMinutes(private val format: String) : Timer(updatesPerSecond = 1) {
            override fun formatSeconds(seconds: Double): String =
                String.format(format, (seconds / SECONDS_PER_MINUTE).toLong())
        }

        class TotalSeconds(private val format: String) : Timer(updatesPerSecond = 1) {
            override fun formatSeconds(seconds: Double): String =
                String.format(format, seconds.toLong())
        }

        class TotalMilliseconds(private val format: String) : Timer(updatesPerSecond = 120) {
            override fun formatSeconds(seconds: Double): String =
                String.format(format, (seconds * 1000).toLong())
        }

        internal companion object Factory {
            private const val SECONDS_PER_MINUTE = 60L
            private const val SECONDS_PER_HOUR = 3600L
            private const val SECONDS_PER_DAY = 86400L

            fun fromParams(name: String, params: Map<String, Any?>?): Timer? {
                val format = (params?.get(CONVERTER_PARAM_FORMAT) as? String)
                    ?.let(::validateIntegerFormat)
                return when (name) {
                    "days" -> Days(format ?: DEFAULT_TIMER_COMPONENT_FORMAT)
                    "hours" -> Hours(format ?: DEFAULT_TIMER_COMPONENT_FORMAT)
                    "minutes" -> Minutes(format ?: DEFAULT_TIMER_COMPONENT_FORMAT)
                    "seconds" -> Seconds(format ?: DEFAULT_TIMER_COMPONENT_FORMAT)
                    "deciseconds" -> Deciseconds
                    "centiseconds" -> Centiseconds
                    "milliseconds" -> Milliseconds
                    "total_days" -> TotalDays(format ?: DEFAULT_TIMER_TOTAL_FORMAT)
                    "total_hours" -> TotalHours(format ?: DEFAULT_TIMER_TOTAL_FORMAT)
                    "total_minutes" -> TotalMinutes(format ?: DEFAULT_TIMER_TOTAL_FORMAT)
                    "total_seconds" -> TotalSeconds(format ?: DEFAULT_TIMER_TOTAL_FORMAT)
                    "total_milliseconds" -> TotalMilliseconds(format ?: DEFAULT_TIMER_TOTAL_FORMAT)
                    else -> null
                }
            }

            private fun validateIntegerFormat(format: String): String? {
                if (INTEGER_FORMAT_REGEX.matches(format)) return format
                log(WARN) { "$LOG_PREFIX Invalid integer format \"$format\", falling back to default" }
                return null
            }
        }
    }

    internal companion object {
        fun fromJson(spec: ConverterSpec): TagConverter? {
            Timer.fromParams(spec.name, spec.params)?.let { return it }
            return when (spec.name) {
                "percent" -> Percent(validateNumberFormat(spec.formatParam()) ?: DEFAULT_NUMBER_FORMAT)
                "number" -> Number(validateNumberFormat(spec.formatParam()) ?: DEFAULT_NUMBER_FORMAT)
                "date_time" -> DateTime.fromParams(spec.params)
                else -> {
                    log(WARN) { "$LOG_PREFIX Unsupported tag converter: \"${spec.name}\"" }
                    null
                }
            }
        }

        private fun ConverterSpec.formatParam(): String? = params?.get(CONVERTER_PARAM_FORMAT) as? String

        private fun validateNumberFormat(format: String?): String? {
            if (format == null) return null
            if (NUMBER_FORMAT_REGEX.matches(format)) return format
            log(WARN) { "$LOG_PREFIX Invalid number format \"$format\", falling back to \"$DEFAULT_NUMBER_FORMAT\"" }
            return null
        }
    }
}

private fun Any.coerceToDouble(): Double? = when (this) {
    is Number -> toDouble()
    is String -> toDoubleOrNull()
    else -> null
}

private fun String.applyDecimalSeparator(locale: Locale): String {
    val sep = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    return if (sep == '.') this else replace('.', sep)
}
