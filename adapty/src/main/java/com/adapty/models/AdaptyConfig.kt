package com.adapty.models

public class AdaptyConfig private constructor(
    @get:JvmSynthetic internal val apiKey: String,
    @get:JvmSynthetic internal val observerMode: Boolean,
    @get:JvmSynthetic internal val customerUserId: String?,
    @get:JvmSynthetic internal val gpObfuscatedAccountId: String?,
    @get:JvmSynthetic internal val enablePendingPrepaidPlans: Boolean,
    @get:JvmSynthetic internal val ipAddressCollectionDisabled: Boolean,
    @get:JvmSynthetic internal val adIdCollectionDisabled: Boolean,
    @get:JvmSynthetic internal val adaptyAttributionEnabled: Boolean,
    @get:JvmSynthetic internal val serverCluster: ServerCluster,
    @get:JvmSynthetic internal val customProcessName: String?,
    @get:JvmSynthetic internal val allowLocalPAL: Boolean,
    @get:JvmSynthetic internal val storeMessagesHandling: StoreMessagesHandling,
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

        private var adaptyAttributionEnabled = false

        private var allowLocalPAL = false

        private var serverCluster = ServerCluster.DEFAULT

        private var customProcessName: String? = null

        private var storeMessagesHandling = StoreMessagesHandling.AUTO

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

        /**
         * @param[enabled] A boolean value that enables the [Adapty Attribution](https://adapty.io/docs/user-acquisition) service (install registration).
         *
         * Default value is `false`.
         */
        public fun withAdaptyAttributionEnabled(enabled: Boolean): Builder {
            this.adaptyAttributionEnabled = enabled
            return this
        }

        public fun withServerCluster(serverCluster: ServerCluster): Builder {
            this.serverCluster = serverCluster
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
         * @param[storeMessagesHandling] Controls whether Adapty requests Google Play in-app messages
         * (for example, payment decline notices) automatically, see [StoreMessagesHandling].
         *
         * Default value is [StoreMessagesHandling.AUTO].
         */
        public fun withStoreMessagesHandling(storeMessagesHandling: StoreMessagesHandling): Builder {
            this.storeMessagesHandling = storeMessagesHandling
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
                adaptyAttributionEnabled,
                serverCluster,
                customProcessName,
                allowLocalPAL,
                storeMessagesHandling,
            )
        }
    }

    public enum class ServerCluster(
        @get:JvmSynthetic internal val baseUrl: String,
        @get:JvmSynthetic internal val fallbackBaseUrl: String,
        @get:JvmSynthetic internal val configsCdnBaseUrl: String,
        @get:JvmSynthetic internal val uaBaseUrl: String,
    ) {
        DEFAULT(
            "https://api.adapty.io/api/v1",
            "https://fallback.adapty.io/api/v1",
            "https://configs-cdn.adapty.io/api/v1",
            "https://api-ua.adapty.io/api/v1",
        ),
        EU(
            "https://api-eu.adapty.io/api/v1",
            "https://fallback.adapty.io/api/v1",
            "https://configs-cdn.adapty.io/api/v1",
            "https://api-ua.adapty.io/api/v1",
        ),
        CN(
            "https://api-cn.adapty.io/api/v1",
            "https://fallback-cn.adapty.io/api/v1",
            "https://configs-cdn-cn.adapty.io/api/v1",
            "https://api-ua-cn.adapty.io/api/v1",
        ),
    }

    /**
     * Defines when Adapty asks Google Play to show its in-app messages, such as payment decline notices.
     * Google Play decides whether an applicable message exists, so a request does not guarantee any UI.
     */
    public enum class StoreMessagesHandling {
        /**
         * Default. After the SDK is activated, Adapty requests Google Play in-app messages
         * every time an `Activity` is started. [com.adapty.Adapty.showStoreMessages] stays available.
         */
        AUTO,

        /**
         * Adapty never requests Google Play in-app messages on its own.
         * The app decides when to call [com.adapty.Adapty.showStoreMessages] with the `Activity` that should host the message.
         */
        MANUAL,
    }

    @get:JvmSynthetic
    internal val apiKeyPrefix: String = apiKey.split(".").getOrNull(0).orEmpty()
}