package com.adapty.internal.data.models.responses

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.ProfileResponseData
import com.adapty.internal.data.models.ProfileResponseData.Attributes.*
import com.adapty.models.GoogleValidationResult
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class RestoreReceiptResponse(
    @SerializedName("data")
    val data: Data?
) {
    internal class Data(
        @SerializedName("id")
        val id: String?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("attributes")
        val attributes: Attributes?
    ) {

        internal class Attributes(
            @SerializedName("google_validation_result")
            val googleValidationResult: ArrayList<GoogleValidationResult>?,
            @SerializedName("profile_id")
            val profileId: String?,
            @SerializedName("customer_user_id")
            val customerUserId: String?,
            @SerializedName("paid_access_levels")
            val accessLevels: HashMap<String, AccessLevelInfo>?,
            @SerializedName("subscriptions")
            val subscriptions: HashMap<String, SubscriptionsInfo>?,
            @SerializedName("non_subscriptions")
            val nonSubscriptions: HashMap<String, ArrayList<NonSubscriptionsInfo>>?,
        ) {
            fun extractPurchaserInfo() = ProfileResponseData.Attributes(
                profileId,
                customerUserId,
                accessLevels,
                subscriptions,
                nonSubscriptions,
            )
        }
    }
}