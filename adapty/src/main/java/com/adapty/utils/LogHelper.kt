package com.adapty.utils

internal object LogHelper {

    private var logger: DefaultLogger? = null

    internal fun setLogLevel(logLevel: LogLevel) {
        logger = LoggerFactory.getLogger(logLevel)
    }

    internal fun logVerbose(log: String) {
        logger?.logVerbose(log)
    }

    internal fun logError(log: String) {
        logger?.logError(log)
    }
}

internal object LoggerFactory {

    fun getLogger(logLevel: LogLevel): DefaultLogger = when (logLevel) {
        LogLevel.NONE -> DefaultLogger()
        LogLevel.ERROR -> ErrorLogger()
        LogLevel.VERBOSE -> VerboseLogger()
    }
}

enum class LogLevel {
    NONE, VERBOSE, ERROR
}