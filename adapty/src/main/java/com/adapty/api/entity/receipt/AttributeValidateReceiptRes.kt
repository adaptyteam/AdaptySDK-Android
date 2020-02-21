package com.adapty.api.entity.receipt

import com.google.gson.annotations.SerializedName

open class AttributeValidateReceiptRes {
    @SerializedName("subscriber")
    var subscriber: Subscriber? = null
}