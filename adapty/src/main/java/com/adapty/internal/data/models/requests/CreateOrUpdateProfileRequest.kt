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
            @SerializedName("first_name")
            private val firstName: String?,
            @SerializedName("last_name")
            private val lastName: String?,
            @SerializedName("gender")
            private val gender: String?,
            @SerializedName("birthday")
            private val birthday: String?,
            @SerializedName("analytics_disabled")
            private val analyticsDisabled: Boolean?,
            @SerializedName("ip_v4_address")
            private val ipv4Address: String?,
            @SerializedName("custom_attributes")
            private val customAttributes: Map<String, Any>?,
        ) {

            internal companion object {
                fun create(
                    installationMeta: InstallationMeta?,
                    customerUserId: String?,
                    updateProfileParams: AdaptyProfileParameters?,
                    ipv4Address: String?,
                ) = Attributes(
                    installationMeta = installationMeta,
                    customerUserId = customerUserId,
                    email = updateProfileParams?.email,
                    phoneNumber = updateProfileParams?.phoneNumber,
                    firstName = updateProfileParams?.firstName,
                    lastName = updateProfileParams?.lastName,
                    gender = updateProfileParams?.gender,
                    birthday = updateProfileParams?.birthday,
                    analyticsDisabled = updateProfileParams?.analyticsDisabled,
                    ipv4Address = ipv4Address,
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
            ipv4Address: String?,
        ) = create(profileId, installationMeta, null, updateProfileParams, ipv4Address)

        fun create(
            profileId: String,
            installationMeta: InstallationMeta?,
            customerUserId: String?,
            updateProfileParams: AdaptyProfileParameters?,
        ) = create(profileId, installationMeta, customerUserId, updateProfileParams, null)

        private fun create(
            profileId: String,
            installationMeta: InstallationMeta?,
            customerUserId: String?,
            updateProfileParams: AdaptyProfileParameters?,
            ipv4Address: String?,
        ) = CreateOrUpdateProfileRequest(
            Data(
                id = profileId,
                attributes = Data.Attributes.create(
                    installationMeta,
                    customerUserId,
                    updateProfileParams,
                    ipv4Address,
                )
            )
        )
    }
}