package com.adapty.internal.crossplatform

import com.adapty.internal.crossplatform.ui.CreateFlowViewArgs
import com.adapty.internal.crossplatform.ui.LocalizationStrategyArgs
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader

internal class CreateFlowViewArgsTypeAdapterFactory :
    BaseTypeAdapterFactory<CreateFlowViewArgs>(CreateFlowViewArgs::class.java) {

    private companion object {
        const val PRELOAD_PRODUCTS = "preload_products"
        const val ENABLE_SAFE_AREA_PADDINGS = "enable_safe_area_paddings"
        const val PREFERRED_LOCALES = "preferred_locales"
        const val LOCALIZATION_STRATEGY = "localization_strategy"
        const val RESOLVER = "resolver"
        const val CUSTOM_ID = "custom_id"
    }

    override fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<CreateFlowViewArgs>,
        elementAdapter: TypeAdapter<JsonElement>
    ): CreateFlowViewArgs? {
        val jsonObject = elementAdapter.read(`in`).asJsonObject

        validatePreferredLocales(jsonObject.get(PREFERRED_LOCALES))
        validateLocalizationStrategy(jsonObject.get(LOCALIZATION_STRATEGY))

        if (!jsonObject.has(PRELOAD_PRODUCTS))
            jsonObject.addProperty(PRELOAD_PRODUCTS, false)
        if (!jsonObject.has(ENABLE_SAFE_AREA_PADDINGS))
            jsonObject.addProperty(ENABLE_SAFE_AREA_PADDINGS, true)

        return delegateAdapter.fromJsonTree(jsonObject)
    }

    private fun validatePreferredLocales(element: JsonElement?) {
        val array = element?.takeIf(JsonElement::isJsonArray)?.asJsonArray
            ?: throw JsonParseException("$PREFERRED_LOCALES must be an array")
        if (!array.all { it.isJsonPrimitive && it.asJsonPrimitive.isString })
            throw JsonParseException("$PREFERRED_LOCALES must contain only strings")
    }

    private fun validateLocalizationStrategy(element: JsonElement?) {
        val strategy = element?.takeIf(JsonElement::isJsonObject)?.asJsonObject
            ?: throw JsonParseException("$LOCALIZATION_STRATEGY must be an object")
        val resolver = strategy.get(RESOLVER)
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
        if (resolver !in LocalizationStrategyArgs.RESOLVERS)
            throw JsonParseException("Unsupported localization resolver: ${strategy.get(RESOLVER)}")
        val customId = strategy.get(CUSTOM_ID)
        if (customId != null && !customId.isJsonNull && !(customId.isJsonPrimitive && customId.asJsonPrimitive.isString))
            throw JsonParseException("$CUSTOM_ID must be a string")
    }
}
