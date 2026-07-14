package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyPaywallProduct
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyPaywallProductTypeAdapterFactory :
    BaseTypeAdapterFactory<AdaptyPaywallProduct>(AdaptyPaywallProduct::class.java) {

    private companion object {
        const val PAYLOAD_DATA = "payload_data"
        const val ADAPTY_PRODUCT_ID = "adapty_product_id"
        const val PAYWALL_PRODUCT_INDEX = "paywall_product_index"
        const val FLOW_PRODUCT_ID = "flow_product_id"
    }

    override fun write(
        out: JsonWriter,
        value: AdaptyPaywallProduct,
        delegateAdapter: TypeAdapter<AdaptyPaywallProduct>,
        elementAdapter: TypeAdapter<JsonElement>
    ) {
        val jsonObject = delegateAdapter.toJsonTree(value).asJsonObject
        val payloadData = jsonObject.getAsJsonObject(PAYLOAD_DATA)
        payloadData.moveNode(jsonObject, PAYWALL_PRODUCT_INDEX, JsonPrimitive(-1))
        payloadData.moveNode(jsonObject, ADAPTY_PRODUCT_ID, JsonPrimitive("unknown"))
        payloadData.moveNodeIfExists(jsonObject, FLOW_PRODUCT_ID)
        val payloadDataStr = elementAdapter.toJson(payloadData).toBase64()
        jsonObject.addProperty(PAYLOAD_DATA, payloadDataStr)
        elementAdapter.write(out, jsonObject)
    }

    override fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<AdaptyPaywallProduct>,
        elementAdapter: TypeAdapter<JsonElement>
    ): AdaptyPaywallProduct? {
        val jsonObject = elementAdapter.read(`in`).asJsonObject
        val payloadDataStr = jsonObject.get(PAYLOAD_DATA).asString.fromBase64()
        val payloadData = elementAdapter.fromJson(payloadDataStr).asJsonObject
        jsonObject.moveNode(payloadData, PAYWALL_PRODUCT_INDEX, JsonPrimitive(-1))
        jsonObject.moveNode(payloadData, ADAPTY_PRODUCT_ID, JsonPrimitive("unknown"))
        jsonObject.moveNodeIfExists(payloadData, FLOW_PRODUCT_ID)
        jsonObject.add(PAYLOAD_DATA, payloadData)
        return delegateAdapter.fromJsonTree(jsonObject)
    }
}