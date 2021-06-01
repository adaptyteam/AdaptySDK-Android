package com.adapty.internal.utils

import android.util.Log
import androidx.annotation.RestrictTo
import com.adapty.utils.AdaptyLogLevel.NONE
import com.adapty.utils.AdaptyLogLevel.VERBOSE

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object Logger {

    private const val TAG = "Adapty_v${com.adapty.BuildConfig.VERSION_NAME}"

    private fun isErrorLoggable() = logLevel != NONE

    private fun isVerboseLoggable() = logLevel == VERBOSE

    @JvmSynthetic
    @JvmField
    internal var logLevel = NONE

    @JvmSynthetic
    inline fun logVerbose(msg: () -> String) {
        if (isVerboseLoggable())
            Log.d(TAG, msg())
    }

    @JvmSynthetic
    inline fun logError(msg: () -> String) {
        if (isErrorLoggable())
            Log.e(TAG, msg())
    }
}