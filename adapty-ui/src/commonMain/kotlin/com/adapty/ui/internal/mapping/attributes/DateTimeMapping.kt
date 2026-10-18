package com.adapty.ui.internal.mapping.attributes

import com.adapty.ui.internal.platform.PlatformTimeZone
import com.adapty.ui.internal.platform.currentTimeMillis
import com.adapty.ui.internal.text.PlatformFormat
import com.adapty.ui.internal.ui.attributes.DateTime

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
    val anchorTimeZone = (this["anchor_time_zone"] as? String)?.let(PlatformFormat::parseTimeZone)
    val tz = anchorTimeZone ?: PlatformFormat.defaultTimeZone()

    val anchorTimeMs: Long = when (anchorStr) {
        "start" -> currentTimeMillis()
        else -> PlatformFormat.calendarAnchorMillis(anchorStr, tz) ?: return null
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

private fun Map<*, *>.applyIntervalComponents(baseMs: Long, timeZone: PlatformTimeZone): Long =
    PlatformFormat.applyIntervalComponents(this, baseMs, timeZone)

private fun Map<*, *>.toDateComponentsDateTime(): DateTime? {
    val year = (this["year"] as? Number)?.toInt() ?: return null
    val month = (this["month"] as? Number)?.toInt() ?: return null
    val day = (this["day"] as? Number)?.toInt() ?: return null
    val hour = (this["hour"] as? Number)?.toInt() ?: 0
    val minute = (this["minute"] as? Number)?.toInt() ?: 0
    val second = (this["second"] as? Number)?.toInt() ?: 0
    val timeZone = (this["time_zone"] as? String)?.let(PlatformFormat::parseTimeZone) ?: PlatformFormat.defaultTimeZone()

    return DateTime.Absolute(
        PlatformFormat.dateComponentsToMillis(timeZone, year, month, day, hour, minute, second)
    )
}
