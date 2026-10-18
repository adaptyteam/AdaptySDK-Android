package com.adapty.ui.listeners

import com.adapty.ui.internal.platform.PlatformDate
import com.adapty.ui.internal.platform.currentTimeMillis
import com.adapty.ui.internal.platform.platformDate
import com.adapty.ui.internal.utils.HOUR_MILLIS

/**
 * Implement this interface to use custom timer functionality
 */
public fun interface AdaptyUiTimerResolver {
    /**
     * A function that returns the date the timer with [timerId] should end at.
     *
     * @param[timerId] ID of the timer.
     *
     * @return The date the timer with [timerId] should end at.
     */
    public fun timerEndAtDate(timerId: String): PlatformDate

    public companion object {
        @JvmField
        public val Default: AdaptyUiTimerResolver =
            AdaptyUiTimerResolver { _ -> platformDate(currentTimeMillis() + HOUR_MILLIS) }
    }
}