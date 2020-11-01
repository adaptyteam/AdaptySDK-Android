package com.adapty.utils

internal object LogHelper {

    private var logger: DefaultLogger? = null

    internal fun setLogLevel(logLevel: AdaptyLogLevel) {
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

    fun getLogger(logLevel: AdaptyLogLevel): DefaultLogger = when (logLevel) {
        AdaptyLogLevel.NONE -> DefaultLogger()
        AdaptyLogLevel.ERROR -> ErrorLogger()
        AdaptyLogLevel.VERBOSE -> VerboseLogger()
    }
}

enum class AdaptyLogLevel {
    NONE, VERBOSE, ERROR
}