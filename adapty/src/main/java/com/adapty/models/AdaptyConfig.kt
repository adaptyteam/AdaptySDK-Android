package com.adapty.models

public class AdaptyConfig private constructor(
    @get:JvmSynthetic internal val apiKey: String,
    @get:JvmSynthetic internal val observerMode: Boolean,
    @get:JvmSynthetic internal val customerUserId: String?,
    @get:JvmSynthetic internal val ipAddressCollectionDisabled: Boolean,
) {

    /**
     * @property[apiKey] You can find it in your app settings
     * in [Adapty Dashboard](https://app.adapty.io/) _App settings_ > _General_.
     */
    public class Builder(private val apiKey: String) {

        private var customerUserId: String? = null

        private var observerMode = false

        private var ipAddressCollectionDisabled = false

        /**
         * @param[customerUserId] User identifier in your system.
         *
         * Default value is `null`.
         */
        public fun withCustomerUserId(customerUserId: String?): Builder {
            this.customerUserId = customerUserId
            return this
        }

        /**
         * @param[observerMode] A boolean value controlling [Observer mode](https://adapty.io/docs/observer-vs-full-mode).
         * Turn it on if you handle purchases and subscription status yourself and use Adapty for sending
         * subscription events and analytics.
         *
         * Default value is `false`.
         */
        public fun withObserverMode(observerMode: Boolean): Builder {
            this.observerMode = observerMode
            return this
        }

        public fun withIpAddressCollectionDisabled(disabled: Boolean): Builder {
            this.ipAddressCollectionDisabled = disabled
            return this
        }

        public fun build(): AdaptyConfig {
            return AdaptyConfig(
                apiKey,
                observerMode,
                customerUserId,
                ipAddressCollectionDisabled,
            )
        }
    }
}