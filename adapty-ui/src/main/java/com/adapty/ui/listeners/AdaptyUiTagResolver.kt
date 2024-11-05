package com.adapty.ui.listeners

/**
 * Implement this interface to specify the string values with which custom tags should be replaced
 */
public fun interface AdaptyUiTagResolver {
    /**
     * A function that maps a custom tag to the string value it should be replaced with.
     * If `null` is returned, the tag will not be replaced.
     *
     * @param[tag] The custom tag to be replaced.
     *
     * @return The value the [tag] should be replaced with, or `null` if there is no mapping for the [tag].
     */
    public fun replacement(tag: String): String?

    public companion object {
        /**
         * The default implementation that has no replacements.
         */
        @JvmField
        public val DEFAULT: AdaptyUiTagResolver =
            AdaptyUiTagResolver { _ -> null }
    }
}