package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FlowViewConfig(
    @SerializedName("view_configuration_id")
    val viewConfigurationId: String,
    @SerializedName("config")
    val config: Map<String, Any>,
)
