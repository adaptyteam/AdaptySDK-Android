package com.adapty.utils

class AdaptyLogLevel private constructor(@JvmField @JvmSynthetic internal val value: Int) {

    companion object {

        /**
         * No logs
         */
        @JvmField
        val NONE = AdaptyLogLevel(0)

        /**
         * Logging errors
         */
        @JvmField
        val ERROR = AdaptyLogLevel(1)

        /**
         * Logging network requests
         */
        @JvmField
        val REQUESTS = AdaptyLogLevel(2)

        /**
         * Logging successful network responses
         */
        @JvmField
        val RESPONSES = AdaptyLogLevel(4)

        /**
         * Logging public method calls
         */
        @JvmField
        val PUBLIC_METHOD_CALLS = AdaptyLogLevel(8)

        /**
         * Debug log level
         *
         * Including requests, successful responses and public method calls
         *
         * Excluding errors and analytics
         */
        @JvmField
        val DEBUG = REQUESTS + RESPONSES + PUBLIC_METHOD_CALLS

        /**
         * Verbose log level
         *
         * Including [DEBUG] and [ERROR] log levels
         *
         * Excluding analytics
         */
        @JvmField
        val VERBOSE = DEBUG + ERROR

        /**
         * Logging analytics
         */
        @JvmField
        val ANALYTICS = AdaptyLogLevel(16)

        /**
         * All logs
         */
        @JvmField
        val ALL = VERBOSE + ANALYTICS
    }

    /**
     * Adds [other] log level to this
     */
    @JvmName("with")
    operator fun plus(other: AdaptyLogLevel) = AdaptyLogLevel(value or other.value)

    /**
     * Excludes [other] log level from this
     */
    @JvmName("without")
    operator fun minus(other: AdaptyLogLevel) = AdaptyLogLevel(value and other.value.inv())
}