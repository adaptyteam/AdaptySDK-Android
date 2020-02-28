package com.adapty.api.entity.validate

import com.google.gson.annotations.SerializedName

open class AttributeValidateReceiptRes {
    @SerializedName("google_validation_result")
    var googleValidationResult: GoogleValidationResult? = null
}