package com.adapty.utils

public class AdaptyLogLevel private constructor(@JvmField @JvmSynthetic internal val value: Int) {

    public companion object {

        /**
         * No logs
         */
        @JvmField
        public val NONE: AdaptyLogLevel = AdaptyLogLevel(0)

        /**
         * Logging errors
         */
        @JvmField
        public val ERROR: AdaptyLogLevel = AdaptyLogLevel(1)

        /**
         * Logging network requests
         */
        @JvmField
        public val REQUESTS: AdaptyLogLevel = AdaptyLogLevel(2)

        /**
         * Logging successful network responses
         */
        @JvmField
        public val RESPONSES: AdaptyLogLevel = AdaptyLogLevel(4)

        /**
         * Logging public method calls
         */
        @JvmField
        public val PUBLIC_METHOD_CALLS: AdaptyLogLevel = AdaptyLogLevel(8)

        /**
         * Debug log level
         *
         * Including requests, successful responses and public method calls
         *
         * Excluding errors and analytics
         */
        @JvmField
        public val DEBUG: AdaptyLogLevel = REQUESTS + RESPONSES + PUBLIC_METHOD_CALLS

        /**
         * Verbose log level
         *
         * Including [DEBUG] and [ERROR] log levels
         *
         * Excluding analytics
         */
        @JvmField
        public val VERBOSE: AdaptyLogLevel = DEBUG + ERROR

        /**
         * Logging analytics
         */
        @JvmField
        public val ANALYTICS: AdaptyLogLevel = AdaptyLogLevel(16)

        /**
         * All logs
         */
        @JvmField
        public val ALL: AdaptyLogLevel = VERBOSE + ANALYTICS
    }

    /**
     * Adds [other] log level to this
     */
    @JvmName("with")
    public operator fun plus(other: AdaptyLogLevel): AdaptyLogLevel = AdaptyLogLevel(value or other.value)

    /**
     * Excludes [other] log level from this
     */
    @JvmName("without")
    public operator fun minus(other: AdaptyLogLevel): AdaptyLogLevel = AdaptyLogLevel(value and other.value.inv())
}