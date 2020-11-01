package com.adapty.api.entity.validate

import com.adapty.api.entity.purchaserInfo.AttributePurchaserInfoRes
import com.google.gson.annotations.SerializedName

class AttributeValidateReceiptRes : AttributePurchaserInfoRes() {
    @SerializedName("google_validation_result")
    var googleValidationResult: GoogleValidationResult? = null
}