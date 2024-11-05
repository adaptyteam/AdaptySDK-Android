package com.adapty.ui.listeners

import com.adapty.ui.internal.utils.HOUR_MILLIS
import java.util.Date

/**
 * Implement this interface to to use custom timer functionality
 */
public fun interface AdaptyUiTimerResolver {
    /**
     * A function that returns the date the timer with [timerId] should end at.
     *
     * @param[timerId] ID of the timer.
     *
     * @return The date the timer with [timerId] should end at.
     */
    public fun timerEndAtDate(timerId: String): Date

    public companion object {
        @JvmField
        public val DEFAULT: AdaptyUiTimerResolver =
            AdaptyUiTimerResolver { _ -> Date(System.currentTimeMillis() + HOUR_MILLIS) }
    }
}