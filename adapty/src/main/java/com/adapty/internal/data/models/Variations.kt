package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class Variations(
    val data: List<PaywallDto>,
    @SerializedName("response_created_at")
    val snapshotAt: Long,
    val version: Int,
)