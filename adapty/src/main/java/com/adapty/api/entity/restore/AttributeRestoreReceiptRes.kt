package com.adapty.api.entity.validate

import com.google.gson.annotations.SerializedName

open class AttributeRestoreReceiptRes {
    @SerializedName("google_validation_result")
    var googleValidationResult: ArrayList<GoogleValidationResult>? = null
}