package com.adapty.ui.internal.ui.attributes

internal sealed class DateTime {
    data class Absolute(val timestampMs: Long) : DateTime()
    data class FromStart(val offsetMs: Long) : DateTime()

    fun resolveTimestampMs(startAtMs: Long): Long = when (this) {
        is Absolute -> timestampMs
        is FromStart -> startAtMs + offsetMs
    }
}
