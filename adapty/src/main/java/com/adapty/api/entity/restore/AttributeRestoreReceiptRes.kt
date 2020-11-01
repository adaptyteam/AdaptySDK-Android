package com.adapty.api.entity.validate

import com.adapty.api.entity.purchaserInfo.AttributePurchaserInfoRes
import com.google.gson.annotations.SerializedName

class AttributeRestoreReceiptRes : AttributePurchaserInfoRes() {
    @SerializedName("google_validation_result")
    var googleValidationResult: ArrayList<GoogleValidationResult>? = null
}