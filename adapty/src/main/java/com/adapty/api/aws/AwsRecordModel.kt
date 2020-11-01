package com.adapty.api.aws

import com.google.gson.annotations.SerializedName

data class AwsRecordModel(
    @SerializedName("Data")
    var Data: String,

    @SerializedName("PartitionKey")
    var PartitionKey: String
)