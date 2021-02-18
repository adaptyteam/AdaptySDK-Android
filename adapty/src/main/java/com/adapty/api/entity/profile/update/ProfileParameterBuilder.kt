package com.adapty.api.entity.profile.update

class ProfileParameterBuilder {

    internal var email: String? = null
    internal var phoneNumber: String? = null
    internal var facebookUserId: String? = null
    internal var facebookAnonymousId: String? = null
    internal var mixpanelUserId: String? = null
    internal var amplitudeUserId: String? = null
    internal var amplitudeDeviceId: String? = null
    internal var appmetricaProfileId: String? = null
    internal var appmetricaDeviceId: String? = null
    internal var firstName: String? = null
    internal var lastName: String? = null
    internal var gender: String? = null
    internal var birthday: String? = null
    internal var customAttributes: Map<String, Any>? = null

    fun withEmail(email: String): ProfileParameterBuilder {
        this.email = email
        return this
    }

    fun withPhoneNumber(phoneNumber: String): ProfileParameterBuilder {
        this.phoneNumber = phoneNumber
        return this
    }

    fun withFacebookUserId(facebookUserId: String): ProfileParameterBuilder {
        this.facebookUserId = facebookUserId
        return this
    }

    fun withFacebookAnonymousId(facebookAnonymousId: String): ProfileParameterBuilder {
        this.facebookAnonymousId = facebookAnonymousId
        return this
    }

    fun withMixpanelUserId(mixpanelUserId: String): ProfileParameterBuilder {
        this.mixpanelUserId = mixpanelUserId
        return this
    }

    fun withAmplitudeUserId(amplitudeUserId: String): ProfileParameterBuilder {
        this.amplitudeUserId = amplitudeUserId
        return this
    }

    fun withAmplitudeDeviceId(amplitudeDeviceId: String): ProfileParameterBuilder {
        this.amplitudeDeviceId = amplitudeDeviceId
        return this
    }

    fun withAppmetricaProfileId(appmetricaProfileId: String): ProfileParameterBuilder {
        this.appmetricaProfileId = appmetricaProfileId
        return this
    }

    fun withAppmetricaDeviceId(appmetricaDeviceId: String): ProfileParameterBuilder {
        this.appmetricaDeviceId = appmetricaDeviceId
        return this
    }

    fun withFirstName(firstName: String): ProfileParameterBuilder {
        this.firstName = firstName
        return this
    }

    fun withLastName(lastName: String): ProfileParameterBuilder {
        this.lastName = lastName
        return this
    }

    fun withGender(gender: Gender): ProfileParameterBuilder {
        this.gender = gender.toString()
        return this
    }

    fun withBirthday(birthday: Date): ProfileParameterBuilder {
        this.birthday = birthday.toString()
        return this
    }

    fun withCustomAttributes(customAttributes: Map<String, Any>): ProfileParameterBuilder {
        this.customAttributes = customAttributes
        return this
    }
}