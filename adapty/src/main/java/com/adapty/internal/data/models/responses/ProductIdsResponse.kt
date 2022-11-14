package com.adapty.internal.data.models.responses

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductIdsResponse(
    @SerializedName("data")
    val data: ArrayList<String>?,
)