package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class InstallRegistrationResponseData(
    @SerializedName("install_id")
    val installId: String,
    @SerializedName("payload")
    val payload: String?,
)