package com.adapty.models

import com.adapty.internal.utils.immutableWithInterop
import com.adapty.utils.ImmutableMap

public class AdaptyProfileParameters private constructor(
    public val email: String?,
    public val phoneNumber: String?,
    public val facebookAnonymousId: String?,
    public val mixpanelUserId: String?,
    public val amplitudeUserId: String?,
    public val amplitudeDeviceId: String?,
    public val appmetricaProfileId: String?,
    public val appmetricaDeviceId: String?,
    public val oneSignalPlayerId: String?,
    public val pushwooshHwid: String?,
    public val firebaseAppInstanceId: String?,
    public val airbridgeDeviceId: String?,
    public val firstName: String?,
    public val lastName: String?,
    public val gender: String?,
    public val birthday: String?,
    public val analyticsDisabled: Boolean?,
    public val customAttributes: ImmutableMap<String, Any?>?,
) {

    public fun builder(): Builder = Builder.from(this)

    public class Builder private constructor(
        private var email: String? = null,
        private var phoneNumber: String? = null,
        private var facebookAnonymousId: String? = null,
        private var mixpanelUserId: String? = null,
        private var amplitudeUserId: String? = null,
        private var amplitudeDeviceId: String? = null,
        private var appmetricaProfileId: String? = null,
        private var appmetricaDeviceId: String? = null,
        private var oneSignalPlayerId: String? = null,
        private var pushwooshHwid: String? = null,
        private var firebaseAppInstanceId: String? = null,
        private var airbridgeDeviceId: String? = null,
        private var firstName: String? = null,
        private var lastName: String? = null,
        private var gender: String? = null,
        private var birthday: String? = null,
        private var analyticsDisabled: Boolean? = null,
        private val customAttributes: MutableMap<String, Any?> = hashMapOf()
    ) {

        public constructor(): this(null)

        public fun withEmail(email: String?): Builder {
            this.email = email
            return this
        }

        public fun withPhoneNumber(phoneNumber: String?): Builder {
            this.phoneNumber = phoneNumber
            return this
        }

        public fun withFacebookAnonymousId(facebookAnonymousId: String?): Builder {
            this.facebookAnonymousId = facebookAnonymousId
            return this
        }

        public fun withMixpanelUserId(mixpanelUserId: String?): Builder {
            this.mixpanelUserId = mixpanelUserId
            return this
        }

        public fun withAmplitudeUserId(amplitudeUserId: String?): Builder {
            this.amplitudeUserId = amplitudeUserId
            return this
        }

        public fun withAmplitudeDeviceId(amplitudeDeviceId: String?): Builder {
            this.amplitudeDeviceId = amplitudeDeviceId
            return this
        }

        public fun withAppmetricaProfileId(appmetricaProfileId: String?): Builder {
            this.appmetricaProfileId = appmetricaProfileId
            return this
        }

        public fun withAppmetricaDeviceId(appmetricaDeviceId: String?): Builder {
            this.appmetricaDeviceId = appmetricaDeviceId
            return this
        }

        public fun withOneSignalPlayerId(oneSignalPlayerId: String?): Builder {
            this.oneSignalPlayerId = oneSignalPlayerId
            return this
        }

        public fun withPushwooshHwid(pushwooshHwid: String?): Builder {
            this.pushwooshHwid = pushwooshHwid
            return this
        }

        public fun withFirebaseAppInstanceId(firebaseAppInstanceId: String?): Builder {
            this.firebaseAppInstanceId = firebaseAppInstanceId
            return this
        }

        public fun withAirbridgeDeviceId(airbridgeDeviceId: String?): Builder {
            this.airbridgeDeviceId = airbridgeDeviceId
            return this
        }

        public fun withFirstName(firstName: String?): Builder {
            this.firstName = firstName
            return this
        }

        public fun withLastName(lastName: String?): Builder {
            this.lastName = lastName
            return this
        }

        public fun withGender(gender: AdaptyProfile.Gender?): Builder {
            this.gender = gender?.toString()
            return this
        }

        public fun withBirthday(birthday: AdaptyProfile.Date?): Builder {
            this.birthday = birthday?.toString()
            return this
        }

        public fun withExternalAnalyticsDisabled(disabled: Boolean?): Builder {
            this.analyticsDisabled = disabled
            return this
        }

        public fun withCustomAttribute(key: String, value: String): Builder {
            customAttributes[key] = value
            return this
        }

        public fun withCustomAttribute(key: String, value: Double): Builder {
            customAttributes[key] = value
            return this
        }

        public fun withRemovedCustomAttribute(key: String): Builder {
            customAttributes[key] = null
            return this
        }

        public fun build(): AdaptyProfileParameters {
            return AdaptyProfileParameters(
                this.email,
                this.phoneNumber,
                this.facebookAnonymousId,
                this.mixpanelUserId,
                this.amplitudeUserId,
                this.amplitudeDeviceId,
                this.appmetricaProfileId,
                this.appmetricaDeviceId,
                this.oneSignalPlayerId,
                this.pushwooshHwid,
                this.firebaseAppInstanceId,
                this.airbridgeDeviceId,
                this.firstName,
                this.lastName,
                this.gender,
                this.birthday,
                this.analyticsDisabled,
                this.customAttributes.takeIf { attrs -> attrs.isNotEmpty() }?.immutableWithInterop(),
            )
        }

        internal companion object {
            @JvmSynthetic
            fun from(params: AdaptyProfileParameters) = Builder(
                params.email,
                params.phoneNumber,
                params.facebookAnonymousId,
                params.mixpanelUserId,
                params.amplitudeUserId,
                params.amplitudeDeviceId,
                params.appmetricaProfileId,
                params.appmetricaDeviceId,
                params.oneSignalPlayerId,
                params.pushwooshHwid,
                params.firebaseAppInstanceId,
                params.airbridgeDeviceId,
                params.firstName,
                params.lastName,
                params.gender,
                params.birthday,
                params.analyticsDisabled,
                params.customAttributes?.map?.toMutableMap() ?: mutableMapOf()
            )
        }
    }
}