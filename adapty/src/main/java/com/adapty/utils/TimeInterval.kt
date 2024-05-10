@file:OptIn(InternalAdaptyApi::class)

package com.adapty.utils

import com.adapty.internal.utils.InternalAdaptyApi
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

public class TimeInterval private constructor(
    /**
     * @suppress
     */
    @property:InternalAdaptyApi @get:JvmSynthetic public val duration: kotlin.time.Duration
): Comparable<TimeInterval> {

    public companion object {
        @JvmStatic
        public fun days(days: Int): TimeInterval = if (days < Int.MAX_VALUE) TimeInterval(days.days) else INFINITE

        @JvmStatic
        public fun hours(hours: Int): TimeInterval = if (hours < Int.MAX_VALUE) TimeInterval(hours.hours) else INFINITE

        @JvmStatic
        public fun minutes(minutes: Int): TimeInterval = if (minutes < Int.MAX_VALUE) TimeInterval(minutes.minutes) else INFINITE

        @JvmStatic
        public fun seconds(seconds: Int): TimeInterval = if (seconds < Int.MAX_VALUE) TimeInterval(seconds.seconds) else INFINITE

        @JvmStatic
        public fun millis(millis: Int): TimeInterval = if (millis < Int.MAX_VALUE) TimeInterval(millis.milliseconds) else INFINITE

        @JvmSynthetic
        public fun from(duration: kotlin.time.Duration): TimeInterval = TimeInterval(duration)

        @JvmField
        public val INFINITE: TimeInterval = TimeInterval(kotlin.time.Duration.INFINITE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimeInterval

        return duration == other.duration
    }

    override fun hashCode(): Int {
        return duration.hashCode()
    }

    override fun toString(): String {
        return duration.toString()
    }

    override fun compareTo(other: TimeInterval): Int {
        return duration.compareTo(other.duration)
    }
}

@get:JvmSynthetic
public val Int.days : TimeInterval get() = TimeInterval.days(this)

@get:JvmSynthetic
public val Int.hours : TimeInterval get() = TimeInterval.hours(this)

@get:JvmSynthetic
public val Int.minutes : TimeInterval get() = TimeInterval.minutes(this)

@get:JvmSynthetic
public val Int.seconds : TimeInterval get() = TimeInterval.seconds(this)

@get:JvmSynthetic
public val Int.millis : TimeInterval get() = TimeInterval.millis(this)

@JvmSynthetic
public fun kotlin.time.Duration.asAdaptyTimeInterval() : TimeInterval = TimeInterval.from(this)