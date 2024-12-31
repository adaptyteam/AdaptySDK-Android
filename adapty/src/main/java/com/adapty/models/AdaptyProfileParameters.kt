package com.adapty.models

import com.adapty.internal.utils.immutableWithInterop
import com.adapty.utils.ImmutableMap

public class AdaptyProfileParameters private constructor(
    public val email: String?,
    public val phoneNumber: String?,
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