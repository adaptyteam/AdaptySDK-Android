package com.adapty.internal.data.models

import android.net.Uri
import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FallbackPaywallsInfo(
    val meta: Meta,
    val source: Uri,
) {
    class Meta(
        @SerializedName("developer_ids")
        val developerIds: List<String>,
        @SerializedName("response_created_at")
        val snapshotAt: Long,
        val version: Int,
    )

    fun copy(uri: Uri) = FallbackPaywallsInfo(meta, uri)
}