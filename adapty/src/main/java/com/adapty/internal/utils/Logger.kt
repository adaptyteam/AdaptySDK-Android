package com.adapty.internal.utils

import android.util.Log
import androidx.annotation.RestrictTo
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.AdaptyLogLevel.Companion.ANALYTICS
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.NONE
import com.adapty.utils.AdaptyLogLevel.Companion.PUBLIC_METHOD_CALLS
import com.adapty.utils.AdaptyLogLevel.Companion.REQUESTS
import com.adapty.utils.AdaptyLogLevel.Companion.RESPONSES

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object Logger {

    private const val TAG = "Adapty_v${com.adapty.BuildConfig.VERSION_NAME}"

    @JvmSynthetic
    @JvmField
    var logLevel = NONE.value

    @JvmSynthetic
    inline fun logError(msg: () -> String) {
        logInternal(ERROR, msg, isErrorLog = true)
    }

    @JvmSynthetic
    inline fun logMethodCall(msg: () -> String) {
        logInternal(PUBLIC_METHOD_CALLS, msg)
    }

    @JvmSynthetic
    inline fun logRequest(msg: () -> String) {
        logInternal(REQUESTS, msg)
    }

    @JvmSynthetic
    inline fun logResponse(msg: () -> String) {
        logInternal(RESPONSES, msg)
    }

    @JvmSynthetic
    inline fun logAnalytics(msg: () -> String) {
        logInternal(ANALYTICS, msg)
    }

    @JvmSynthetic
    private inline fun logInternal(
        messageLogLevel: AdaptyLogLevel,
        msg: () -> String,
        isErrorLog: Boolean = false
    ) {
        when {
            !canLog(messageLogLevel.value) -> return
            isErrorLog -> Log.e(TAG, msg())
            else -> Log.d(TAG, msg())
        }
    }

    private fun canLog(messageLogLevelValue: Int) =
        logLevel and messageLogLevelValue == messageLogLevelValue
}