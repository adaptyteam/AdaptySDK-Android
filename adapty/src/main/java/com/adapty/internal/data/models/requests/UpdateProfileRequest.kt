package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.adapty.utils.ProfileParameterBuilder
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class UpdateProfileRequest(
    @SerializedName("data")
    private val data: Data
) {
    internal class Data(
        @SerializedName("id")
        val id: String,
        @SerializedName("type")
        val type: String = "adapty_analytics_profile",
        @SerializedName("attributes")
        val attributes: Attributes
    ) {

        internal class Attributes(
            @SerializedName("email")
            private val email: String?,
            @SerializedName("phone_number")
            private val phoneNumber: String?,
            @SerializedName("facebook_user_id")
            private val facebookUserId: String?,
            @SerializedName("facebook_anonymous_id")
            private val facebookAnonymousId: String?,
            @SerializedName("first_name")
            private val firstName: String?,
            @SerializedName("last_name")
            private val lastName: String?,
            @SerializedName("gender")
            private val gender: String?,
            @SerializedName("birthday")
            private val birthday: String?,
            @SerializedName("amplitude_user_id")
            private val amplitudeUserId: String?,
            @SerializedName("amplitude_device_id")
            private val amplitudeDeviceId: String?,
            @SerializedName("appmetrica_profile_id")
            private val appmetricaProfileId: String?,
            @SerializedName("appmetrica_device_id")
            private val appmetricaDeviceId: String?,
            @SerializedName("mixpanel_user_id")
            private val mixpanelUserId: String?,
            @SerializedName("custom_attributes")
            private val customAttributes: Map<String, Any>?,
        ) {
            companion object {
                fun from(params: ProfileParameterBuilder) = Attributes(
                    email = params.email,
                    phoneNumber = params.phoneNumber,
                    facebookUserId = params.facebookUserId,
                    facebookAnonymousId = params.facebookAnonymousId,
                    mixpanelUserId = params.mixpanelUserId,
                    amplitudeUserId = params.amplitudeUserId,
                    amplitudeDeviceId = params.amplitudeDeviceId,
                    appmetricaProfileId = params.appmetricaProfileId,
                    appmetricaDeviceId = params.appmetricaDeviceId,
                    firstName = params.firstName,
                    lastName = params.lastName,
                    gender = params.gender,
                    birthday = params.birthday,
                    customAttributes = params.customAttributes
                )
            }
        }
    }

    internal companion object {
        fun create(profileId: String, params: ProfileParameterBuilder) = UpdateProfileRequest(
            Data(
                id = profileId,
                attributes = Data.Attributes.from(params)
            )
        )
    }
}