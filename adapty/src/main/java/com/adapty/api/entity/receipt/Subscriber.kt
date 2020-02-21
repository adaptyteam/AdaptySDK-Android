package com.adapty.api.entity.receipt

import com.google.gson.annotations.SerializedName

class Subscriber {
    @SerializedName("promotionalOfferEligibility")
    var promotionalOfferEligibility: Boolean? = null

    @SerializedName("introductoryOfferEligibility")
    var introductoryOfferEligibility: Boolean? = null
}