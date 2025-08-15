package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.utils.FileLocation
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FallbackPaywallsInfo(
    val meta: Meta,
    val source: FileLocation,
) {
    class Meta(
        @SerializedName("developer_ids")
        val developerIds: List<String>,
        @SerializedName("response_created_at")
        val snapshotAt: Long,
        val version: Int,
    )

    fun copy(location: FileLocation) = FallbackPaywallsInfo(meta, location)
}