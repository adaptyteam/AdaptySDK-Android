package com.adapty.utils

public class AdaptyLogLevel private constructor(
    private val name: String,
    @JvmField @JvmSynthetic internal val value: Int
) {

    public companion object {

        /**
         * No logs
         */
        @JvmField
        public val NONE: AdaptyLogLevel = AdaptyLogLevel("NONE", 0b0)

        /**
         * Only errors will be logged
         */
        @JvmField
        public val ERROR: AdaptyLogLevel = AdaptyLogLevel("ERROR", 0b1)

        /**
         * [ERROR] + messages from the SDK that do not cause critical errors,
         * but are worth paying attention to
         */
        @JvmField
        public val WARN: AdaptyLogLevel = AdaptyLogLevel("WARN", 0b11)

        /**
         * [WARN] + information messages, such as those that log the lifecycle of various modules
         */
        @JvmField
        public val INFO: AdaptyLogLevel = AdaptyLogLevel("INFO", 0b111)

        /**
         * [INFO] + any additional information such as function calls, API queries, etc.
         */
        @JvmField
        public val VERBOSE: AdaptyLogLevel = AdaptyLogLevel("VERBOSE", 0b1111)
    }

    override fun toString(): String {
        return name
    }
}