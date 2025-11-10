package com.adapty.models

import com.adapty.internal.utils.InternalAdaptyApi

public class AdaptyConfig private constructor(
    @get:JvmSynthetic internal val apiKey: String,
    @get:JvmSynthetic internal val observerMode: Boolean,
    @get:JvmSynthetic internal val customerUserId: String?,
    @get:JvmSynthetic internal val gpObfuscatedAccountId: String?,
    @get:JvmSynthetic internal val enablePendingPrepaidPlans: Boolean,
    @get:JvmSynthetic internal val ipAddressCollectionDisabled: Boolean,
    @get:JvmSynthetic internal val adIdCollectionDisabled: Boolean,
    @get:JvmSynthetic internal val backendBaseUrl: String,
    @get:JvmSynthetic internal val customProcessName: String?,
    @get:JvmSynthetic internal val allowLocalPAL: Boolean,
) {

    /**
     * @property[apiKey] You can find it in your app settings
     * in [Adapty Dashboard](https://app.adapty.io/) _App settings_ > _General_.
     */
    public class Builder(private val apiKey: String) {

        private var customerUserId: String? = null

        private var gpObfuscatedAccountId: String? = null

        private var observerMode = false

        private var enablePendingPrepaidPlans = false

        private var ipAddressCollectionDisabled = false

        private var adIdCollectionDisabled = false

        private var allowLocalPAL = false

        private var backendBaseUrl = ServerCluster.DEFAULT.url

        private var customProcessName: String? = null

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
         * @param[gpObfuscatedAccountId] The obfuscated account identifier, [read more](https://developer.android.com/google/play/billing/developer-payload#attribute).
         *
         * Default value is `null`.
         */
        public fun withGPObfuscatedAccountId(gpObfuscatedAccountId: String?): Builder {
            this.gpObfuscatedAccountId = gpObfuscatedAccountId
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

        /**
         * @param[enabled] A boolean value that indicates if pending transactions for [prepaid plans](https://developer.android.com/google/play/billing/subscriptions#prepaid-plans)
         * are enabled, [read more](https://developer.android.com/google/play/billing/subscriptions#pending).
         *
         * Default value is `false`.
         */
        public fun withPendingPrepaidPlansEnabled(enabled: Boolean): Builder {
            this.enablePendingPrepaidPlans = enabled
            return this
        }

        public fun withIpAddressCollectionDisabled(disabled: Boolean): Builder {
            this.ipAddressCollectionDisabled = disabled
            return this
        }

        public fun withAdIdCollectionDisabled(disabled: Boolean): Builder {
            this.adIdCollectionDisabled = disabled
            return this
        }

        public fun withServerCluster(serverCluster: ServerCluster): Builder {
            this.backendBaseUrl = serverCluster.url
            return this
        }

        /**
         * @param[processName] The name of the process where Adapty SDK should operate.
         * Adapty restricts its usage to a single process. If the desired process name differs from
         * the standard main process name, specify it explicitly using this method.
         *
         * Uses the standard main process name by default.
         */
        public fun withProcessName(processName: String): Builder {
            this.customProcessName = processName
            return this
        }

        /**
         * @param[allowed] A boolean value that enables [local access levels](https://adapty.io/docs/local-access-levels).
         * When enabled, the SDK will grant access levels locally when Adapty servers are unavailable.
         * If a user makes a purchase but the SDK cannot receive a response from Adapty servers,
         * the SDK switches to verifying purchases directly in the store, and the access level is granted locally in the app.
         *
         * Default value is `false`.
         */
        public fun withLocalAccessLevelAllowed(allowed: Boolean): Builder {
            this.allowLocalPAL = allowed
            return this
        }

        /**
         * @suppress
         */
        @InternalAdaptyApi
        public fun withBackendBaseUrl(url: String): Builder {
            if (url.isNotEmpty())
                this.backendBaseUrl = url
            return this
        }

        public fun build(): AdaptyConfig {
            return AdaptyConfig(
                apiKey,
                observerMode,
                customerUserId,
                gpObfuscatedAccountId,
                enablePendingPrepaidPlans,
                ipAddressCollectionDisabled,
                adIdCollectionDisabled,
                backendBaseUrl,
                customProcessName,
                allowLocalPAL,
            )
        }
    }

    public enum class ServerCluster(@get:JvmSynthetic internal val url: String) {
        DEFAULT("https://api.adapty.io/api/v1"),
        EU("https://api-eu.adapty.io/api/v1"),
        CN("https://api-cn.adapty.io/api/v1"),
    }
}