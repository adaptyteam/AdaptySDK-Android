package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.requests.ValidateReceiptRequest
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PendingPurchase(
    @SerializedName("validate_data")
    val validateData: ValidateReceiptRequest,
    @SerializedName("saved_at")
    val savedAt: Long,
)
