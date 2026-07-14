package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyPurchaseParameters
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader

internal class AdaptyPurchaseParametersTypeAdapterFactory :
    BaseTypeAdapterFactory<AdaptyPurchaseParameters>(AdaptyPurchaseParameters::class.java) {

    private companion object {
        const val IS_OFFER_PERSONALIZED = "is_offer_personalized"
    }

    override fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<AdaptyPurchaseParameters>,
        elementAdapter: TypeAdapter<JsonElement>
    ): AdaptyPurchaseParameters {
        val jsonObject = elementAdapter.read(`in`).asJsonObject

        if (jsonObject.isEmpty)
            return AdaptyPurchaseParameters.Empty

        if (!jsonObject.has(IS_OFFER_PERSONALIZED))
            jsonObject.addProperty(IS_OFFER_PERSONALIZED, false)

        return delegateAdapter.fromJsonTree(jsonObject)
    }
}