package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class AwsRecordModel(
    @SerializedName("Data")
    val data: String,
    @SerializedName("PartitionKey")
    val partitionKey: String
)