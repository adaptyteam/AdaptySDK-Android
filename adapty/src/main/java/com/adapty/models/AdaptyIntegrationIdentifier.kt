package com.adapty.models

/**
 * An identifier of a third-party integration to be associated with the profile.
 *
 * The set of possible keys is not limited to the predefined constants: new keys may be
 * introduced over time. Instances are compared by [key] and [value], so the predefined
 * constants can be used for comparison.
 *
 * @property[key] The [Key] of the integration identifier.
 * @property[value] The raw value of the integration identifier.
 */
public class AdaptyIntegrationIdentifier(
    public val key: Key,
    value: String,
) {

    public val value: String = value.trim()

    /**
     * A key of an integration identifier.
     *
     * The set of possible values is not limited to the predefined constants: new keys may be
     * introduced over time. Instances are compared by [value], so the predefined constants
     * can be used for comparison.
     *
     * @property[value] The raw string key of the integration identifier.
     */
    public class Key(
        value: String,
    ) {

        public val value: String = value.trim()

        public companion object {
            @JvmField
            public val ADJUST_DEVICE_ID: Key = Key("adjust_device_id")
            @JvmField
            public val AIRBRIDGE_DEVICE_ID: Key = Key("airbridge_device_id")
            @JvmField
            public val AMPLITUDE_USER_ID: Key = Key("amplitude_user_id")
            @JvmField
            public val AMPLITUDE_DEVICE_ID: Key = Key("amplitude_device_id")
            @JvmField
            public val APPMETRICA_DEVICE_ID: Key = Key("appmetrica_device_id")
            @JvmField
            public val APPMETRICA_PROFILE_ID: Key = Key("appmetrica_profile_id")
            @JvmField
            public val APPSFLYER_ID: Key = Key("appsflyer_id")
            @JvmField
            public val BRANCH_ID: Key = Key("branch_id")
            @JvmField
            public val FACEBOOK_ANONYMOUS_ID: Key = Key("facebook_anonymous_id")
            @JvmField
            public val FIREBASE_APP_INSTANCE_ID: Key = Key("firebase_app_instance_id")
            @JvmField
            public val MIXPANEL_USER_ID: Key = Key("mixpanel_user_id")
            @JvmField
            public val ONE_SIGNAL_SUBSCRIPTION_ID: Key = Key("one_signal_subscription_id")
            @JvmField
            public val ONE_SIGNAL_PLAYER_ID: Key = Key("one_signal_player_id")
            @JvmField
            public val POSTHOG_DISTINCT_USER_ID: Key = Key("posthog_distinct_user_id")
            @JvmField
            public val PUSHWOOSH_HWID: Key = Key("pushwoosh_hwid")
            @JvmField
            public val TENJIN_ANALYTICS_INSTALLATION_ID: Key = Key("tenjin_analytics_installation_id")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Key

            return value == other.value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }

        override fun toString(): String {
            return value
        }
    }

    public companion object {
        @JvmStatic
        public fun adjustDeviceId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.ADJUST_DEVICE_ID, value)

        @JvmStatic
        public fun airbridgeDeviceId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.AIRBRIDGE_DEVICE_ID, value)

        @JvmStatic
        public fun amplitudeUserId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.AMPLITUDE_USER_ID, value)

        @JvmStatic
        public fun amplitudeDeviceId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.AMPLITUDE_DEVICE_ID, value)

        @JvmStatic
        public fun appmetricaDeviceId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.APPMETRICA_DEVICE_ID, value)

        @JvmStatic
        public fun appmetricaProfileId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.APPMETRICA_PROFILE_ID, value)

        @JvmStatic
        public fun appsflyerId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.APPSFLYER_ID, value)

        @JvmStatic
        public fun branchId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.BRANCH_ID, value)

        @JvmStatic
        public fun facebookAnonymousId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.FACEBOOK_ANONYMOUS_ID, value)

        @JvmStatic
        public fun firebaseAppInstanceId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.FIREBASE_APP_INSTANCE_ID, value)

        @JvmStatic
        public fun mixpanelUserId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.MIXPANEL_USER_ID, value)

        @JvmStatic
        public fun oneSignalSubscriptionId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.ONE_SIGNAL_SUBSCRIPTION_ID, value)

        @JvmStatic
        public fun oneSignalPlayerId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.ONE_SIGNAL_PLAYER_ID, value)

        @JvmStatic
        public fun posthogDistinctUserId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.POSTHOG_DISTINCT_USER_ID, value)

        @JvmStatic
        public fun pushwooshHWID(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.PUSHWOOSH_HWID, value)

        @JvmStatic
        public fun tenjinAnalyticsInstallationId(value: String): AdaptyIntegrationIdentifier =
            AdaptyIntegrationIdentifier(Key.TENJIN_ANALYTICS_INSTALLATION_ID, value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyIntegrationIdentifier

        if (key != other.key) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String {
        return "AdaptyIntegrationIdentifier(key=$key, value=$value)"
    }
}
