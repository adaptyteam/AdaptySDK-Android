package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.utils.AdaptyLogHandler
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.AdaptyLogLevel.Companion.NONE
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object Logger {

    @JvmSynthetic
    @JvmField
    var logLevel = NONE

    @JvmSynthetic
    @JvmField
    @Volatile
    var logHandler: AdaptyLogHandler = DefaultLogHandler()

    private val logExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    @JvmSynthetic
    inline fun log(
        messageLogLevel: AdaptyLogLevel,
        msg: () -> String,
    ) {
        if (canLog(messageLogLevel.value)) {
            val message = msg()
            logExecutor.execute { logHandler.onLogMessageReceived(messageLogLevel, message) }
        }
    }

    private fun canLog(messageLogLevelValue: Int) =
        logLevel.value and messageLogLevelValue == messageLogLevelValue
}