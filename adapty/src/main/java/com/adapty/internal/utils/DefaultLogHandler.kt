package com.adapty.internal.utils

import android.util.Log
import androidx.annotation.RestrictTo
import com.adapty.utils.AdaptyLogHandler
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.INFO
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultLogHandler : AdaptyLogHandler {

    private companion object {
        private const val TAG = "Adapty_v${com.adapty.BuildConfig.VERSION_NAME}"
        private const val CHUNK_MAX_LENGTH = 4000
        private const val MAX_CHUNKS = 5
    }

    override fun onLogMessageReceived(level: AdaptyLogLevel, message: String) {
        when (level) {
            ERROR -> log(level, message) { str -> Log.e(TAG, str) }
            WARN -> log(level, message) { str -> Log.w(TAG, str) }
            INFO -> log(level, message) { str -> Log.i(TAG, str) }
            VERBOSE -> log(level, message) { str -> Log.v(TAG, str) }
        }
    }

    private inline fun log(level: AdaptyLogLevel, originalMessage: String, logAction: (String) -> Unit) {
        if (originalMessage.length > CHUNK_MAX_LENGTH) {
            var i = 0
            val totalLogLength = originalMessage.length
                .coerceAtMost(CHUNK_MAX_LENGTH * MAX_CHUNKS)
            while (i < totalLogLength) {
                val chunkNumber = (i / CHUNK_MAX_LENGTH) + 1
                val endExclusive: Int
                val currentMessage: String

                when {
                    totalLogLength == originalMessage.length -> {
                        endExclusive = (i + CHUNK_MAX_LENGTH).coerceAtMost(totalLogLength)
                        currentMessage =
                            "$level: (chunk ${chunkNumber}) ${originalMessage.substring(i, endExclusive)}"
                    }
                    chunkNumber == MAX_CHUNKS -> {
                        val suffix = " (total length: ${originalMessage.length})"
                        endExclusive = i + CHUNK_MAX_LENGTH - suffix.length
                        currentMessage =
                            "$level: (chunk ${chunkNumber}) ${originalMessage.substring(i, endExclusive)}${suffix}"
                    }
                    else -> {
                        endExclusive = i + CHUNK_MAX_LENGTH
                        currentMessage =
                            "$level: (chunk ${chunkNumber}) ${originalMessage.substring(i, endExclusive)}"
                    }
                }
                logAction(currentMessage)
                i+= CHUNK_MAX_LENGTH
            }
        } else {
            logAction("$level: $originalMessage")
        }
    }
}