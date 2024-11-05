package com.adapty.ui.internal.text

internal enum class TimerSegment(val strValue: String) {
    DAYS("d"),
    HOURS("h"),
    MINUTES("m"),
    SECONDS("s"),
    DECISECONDS("ds"),
    CENTISECONDS("cs"),
    MILLISECONDS("ms"),
    UNKNOWN("");

    companion object {
        fun from(string: String) = values().firstOrNull { it.strValue == string } ?: UNKNOWN
    }
}