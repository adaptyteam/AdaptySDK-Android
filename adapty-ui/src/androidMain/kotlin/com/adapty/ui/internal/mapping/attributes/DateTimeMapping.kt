package com.adapty.ui.internal.mapping.attributes

import com.adapty.ui.internal.ui.attributes.DateTime
import java.util.Calendar
import java.util.TimeZone

internal fun Any?.toDateTime(): DateTime? {
    if (this is Number) {
        return DateTime.Absolute(this.toLong())
    }
    if (this !is Map<*, *>) return null
    val anchorStr = this["anchor"] as? String
        ?: return toDateComponentsDateTime()
    return toRelativeDateTime(anchorStr)
}

private fun Map<*, *>.toRelativeDateTime(anchorStr: String): DateTime? {
    val isNow = anchorStr == "start"
    val anchorTimeZone = (this["anchor_time_zone"] as? String)?.let(::parseTimeZone)
    val tz = anchorTimeZone ?: TimeZone.getDefault()

    val anchorTimeMs: Long = when (anchorStr) {
        "start" -> System.currentTimeMillis()
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
        else -> return null
    }

    val offset = this["offset"]
        ?: return if (isNow) DateTime.FromStart(0L) else DateTime.Absolute(anchorTimeMs)

    if (offset is Number) {
        val offsetMs = offset.toLong()
        return if (isNow) DateTime.FromStart(offsetMs) else DateTime.Absolute(anchorTimeMs + offsetMs)
    }

    if (offset is Map<*, *>) {
        val resultMs = offset.applyIntervalComponents(anchorTimeMs, tz)
        return if (isNow) DateTime.FromStart(resultMs - anchorTimeMs) else DateTime.Absolute(resultMs)
    }

    return null
}

private fun Map<*, *>.applyIntervalComponents(baseMs: Long, timeZone: TimeZone): Long {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = baseMs }
    (this["year"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.YEAR, it) }
    (this["month"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.MONTH, it) }
    (this["day"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.DAY_OF_MONTH, it) }
    (this["hour"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.HOUR_OF_DAY, it) }
    (this["minute"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.MINUTE, it) }
    (this["second"] as? Number)?.toInt()?.takeIf { it != 0 }?.let { cal.add(Calendar.SECOND, it) }
    return cal.timeInMillis
}

private fun Map<*, *>.toDateComponentsDateTime(): DateTime? {
    val year = (this["year"] as? Number)?.toInt() ?: return null
    val month = (this["month"] as? Number)?.toInt() ?: return null
    val day = (this["day"] as? Number)?.toInt() ?: return null
    val hour = (this["hour"] as? Number)?.toInt() ?: 0
    val minute = (this["minute"] as? Number)?.toInt() ?: 0
    val second = (this["second"] as? Number)?.toInt() ?: 0
    val timeZone = (this["time_zone"] as? String)?.let(::parseTimeZone) ?: TimeZone.getDefault()

    return DateTime.Absolute(
        Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    )
}

internal fun parseTimeZone(value: String): TimeZone {
    if (value == "UTC") return TimeZone.getTimeZone("UTC")
    if (value.isNotEmpty() && (value[0] == '+' || value[0] == '-')) {
        return TimeZone.getTimeZone("GMT$value")
    }
    return TimeZone.getTimeZone(value)
}
