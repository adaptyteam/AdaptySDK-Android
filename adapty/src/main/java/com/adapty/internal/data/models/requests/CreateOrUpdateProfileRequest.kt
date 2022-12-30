package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.InstallationMeta
import com.adapty.models.AdaptyProfileParameters
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CreateOrUpdateProfileRequest(
    @SerializedName("data")
    private val data: Data
) {
    internal class Data(
        @SerializedName("id")
        val id: String,
        @SerializedName("type")
        val type: String = "adapty_analytics_profile",
        @SerializedName("attributes")
        val attributes: Attributes?
    ) {

        internal class Attributes(
            @SerializedName("installation_meta")
            private val installationMeta: InstallationMeta?,
            @SerializedName("customer_user_id")
            private val customerUserId: String?,
            @SerializedName("email")
            private val email: String?,
            @SerializedName("phone_number")
            private val phoneNumber: String?,
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
            @SerializedName("one_signal_player_id")
            private val oneSignalPlayerId: String?,
            @SerializedName("pushwoosh_hwid")
            private val pushwooshHwid: String?,
            @SerializedName("firebase_app_instance_id")
            private val firebaseAppInstanceId: String?,
            @SerializedName("analytics_disabled")
            private val analyticsDisabled: Boolean?,
            @SerializedName("custom_attributes")
            private val customAttributes: Map<String, Any>?,
        ) {

            internal companion object {
                fun create(
                    installationMeta: InstallationMeta?,
                    customerUserId: String?,
                    updateProfileParams: AdaptyProfileParameters?,
                ) = Attributes(
                    installationMeta = installationMeta,
                    customerUserId = customerUserId,
                    email = updateProfileParams?.email,
                    phoneNumber = updateProfileParams?.phoneNumber,
                    facebookAnonymousId = updateProfileParams?.facebookAnonymousId,
                    mixpanelUserId = updateProfileParams?.mixpanelUserId,
                    amplitudeUserId = updateProfileParams?.amplitudeUserId,
                    amplitudeDeviceId = updateProfileParams?.amplitudeDeviceId,
                    appmetricaProfileId = updateProfileParams?.appmetricaProfileId,
                    appmetricaDeviceId = updateProfileParams?.appmetricaDeviceId,
                    oneSignalPlayerId = updateProfileParams?.oneSignalPlayerId,
                    pushwooshHwid = updateProfileParams?.pushwooshHwid,
                    firebaseAppInstanceId = updateProfileParams?.firebaseAppInstanceId,
                    firstName = updateProfileParams?.firstName,
                    lastName = updateProfileParams?.lastName,
                    gender = updateProfileParams?.gender,
                    birthday = updateProfileParams?.birthday,
                    analyticsDisabled = updateProfileParams?.analyticsDisabled,
                    customAttributes = updateProfileParams?.customAttributes
                        ?.map?.mapValues { entry -> entry.value ?: "" }
                )
            }
        }
    }

    internal companion object {
        fun create(
            profileId: String,
            installationMeta: InstallationMeta?,
            updateProfileParams: AdaptyProfileParameters?,
        ) = create(profileId, installationMeta, null, updateProfileParams)

        fun create(
            profileId: String,
            installationMeta: InstallationMeta?,
            customerUserId: String?,
            updateProfileParams: AdaptyProfileParameters?,
        ) = CreateOrUpdateProfileRequest(
            Data(
                id = profileId,
                attributes = Data.Attributes.create(
                    installationMeta,
                    customerUserId,
                    updateProfileParams,
                )
            )
        )
    }
}