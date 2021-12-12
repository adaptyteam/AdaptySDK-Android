package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class SyncedPurchase(
    @SerializedName("purchase_token")
    val purchaseToken: String?,
    @SerializedName("purchase_time")
    val purchaseTime: Long?,
)