package com.adapty.models

/**
 * Specifies how the web paywall should be presented.
 */
public sealed class AdaptyWebPresentation {

    /**
     * Opens the web paywall in Chrome Custom Tabs (or similar in-app browser).
     * This keeps the user within the app context.
     */
    internal class InAppBrowser private constructor() : AdaptyWebPresentation() {

        internal companion object {
            fun create() = InAppBrowser()
        }

        override fun toString(): String = "InAppBrowser"
    }

    /**
     * Opens the web paywall in the device's default external browser.
     */
    internal class ExternalBrowser private constructor() : AdaptyWebPresentation() {

        internal companion object {
            fun create() = ExternalBrowser()
        }

        override fun toString(): String = "ExternalBrowser"
    }

    public companion object {

        /**
         * Opens the web paywall in Chrome Custom Tabs (or similar in-app browser).
         * This keeps the user within the app context.
         */
        @JvmField
        public val InAppBrowser: AdaptyWebPresentation = AdaptyWebPresentation.InAppBrowser.create()

        /**
         * Opens the web paywall in the device's default external browser.
         */
        @JvmField
        public val ExternalBrowser: AdaptyWebPresentation = AdaptyWebPresentation.ExternalBrowser.create()
    }
}

