package com.adapty.internal.crossplatform

import com.adapty.internal.crossplatform.ui.CreateFlowViewArgs
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader

internal class CreateFlowViewArgsTypeAdapterFactory :
    BaseTypeAdapterFactory<CreateFlowViewArgs>(CreateFlowViewArgs::class.java) {

    private companion object {
        const val PRELOAD_PRODUCTS = "preload_products"
        const val ENABLE_SAFE_AREA_PADDINGS = "enable_safe_area_paddings"
    }

    override fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<CreateFlowViewArgs>,
        elementAdapter: TypeAdapter<JsonElement>
    ): CreateFlowViewArgs? {
        val jsonObject = elementAdapter.read(`in`).asJsonObject

        if (!jsonObject.has(PRELOAD_PRODUCTS))
            jsonObject.addProperty(PRELOAD_PRODUCTS, false)
        if (!jsonObject.has(ENABLE_SAFE_AREA_PADDINGS))
            jsonObject.addProperty(ENABLE_SAFE_AREA_PADDINGS, true)

        return delegateAdapter.fromJsonTree(jsonObject)
    }
}