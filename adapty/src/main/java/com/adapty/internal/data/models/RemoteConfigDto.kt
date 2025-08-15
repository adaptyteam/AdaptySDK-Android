package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class RemoteConfigDto(
    @SerializedName("lang")
    val lang: String,
    @SerializedName("data")
    val data: String,
    val dataMap: Map<String, Any>,
)