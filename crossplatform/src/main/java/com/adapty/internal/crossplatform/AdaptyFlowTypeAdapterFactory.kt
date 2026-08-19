package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyFlow
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyFlowTypeAdapterFactory :
    BaseTypeAdapterFactory<AdaptyFlow>(AdaptyFlow::class.java) {

    private companion object {
        const val LAYOUTS_CONFIGURATION = "layouts_configuration"
        const val VERSION_ID = "version_id"
        const val FLOW_VERSION_ID = "flow_version_id"
        const val UI_SCHEMA = "ui_schema"
        const val LAYOUTS = "layouts"
        const val GRIDS = "grids"
        const val PLATFORMS = "platforms"
        const val DEVICES = "devices"
        const val H_BREAKPOINTS = "h_breakpoints"
        const val V_BREAKPOINTS = "v_breakpoints"
        const val ALL = "all"
    }

    override fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<AdaptyFlow>,
        elementAdapter: TypeAdapter<JsonElement>
    ): AdaptyFlow? {
        val jsonObject = elementAdapter.read(`in`).asJsonObject

        kotlin.runCatching {
            val versionId = jsonObject.remove(FLOW_VERSION_ID)
                ?.takeIf(JsonElement::isJsonPrimitive)?.asString
            val uiSchema = jsonObject.remove(UI_SCHEMA)
                ?.takeIf(JsonElement::isJsonObject)?.asJsonObject

            if (versionId != null && uiSchema != null) {
                uiSchema.getAsJsonArray(GRIDS)?.forEach { element ->
                    element.takeIf(JsonElement::isJsonObject)?.asJsonObject?.let { grid ->
                        readTriState(grid, PLATFORMS)
                        readTriState(grid, DEVICES)
                        if (grid.get(H_BREAKPOINTS) == null) grid.add(H_BREAKPOINTS, JsonArray())
                        if (grid.get(V_BREAKPOINTS) == null) grid.add(V_BREAKPOINTS, JsonArray())
                    }
                }
                jsonObject.add(LAYOUTS_CONFIGURATION, JsonObject().apply {
                    addProperty(VERSION_ID, versionId)
                    uiSchema.get(LAYOUTS)?.let { add(LAYOUTS, it) }
                    uiSchema.get(GRIDS)?.let { add(GRIDS, it) }
                })
            }
        }

        return delegateAdapter.fromJsonTree(jsonObject)
    }

    override fun write(
        out: JsonWriter,
        value: AdaptyFlow,
        delegateAdapter: TypeAdapter<AdaptyFlow>,
        elementAdapter: TypeAdapter<JsonElement>
    ) {
        val jsonObject = delegateAdapter.toJsonTree(value).asJsonObject

        jsonObject.remove(LAYOUTS_CONFIGURATION)
            ?.takeIf(JsonElement::isJsonObject)?.asJsonObject?.let { config ->
                config.get(VERSION_ID)?.takeIf(JsonElement::isJsonPrimitive)?.asString
                    ?.let { versionId -> jsonObject.addProperty(FLOW_VERSION_ID, versionId) }

                val uiSchema = JsonObject()
                config.get(LAYOUTS)?.let { uiSchema.add(LAYOUTS, it) }
                config.get(GRIDS)?.takeIf(JsonElement::isJsonArray)?.asJsonArray?.let { grids ->
                    grids.forEach { element ->
                        element.takeIf(JsonElement::isJsonObject)?.asJsonObject?.let { grid ->
                            writeTriState(grid, PLATFORMS)
                            writeTriState(grid, DEVICES)
                            dropIfEmpty(grid, H_BREAKPOINTS)
                            dropIfEmpty(grid, V_BREAKPOINTS)
                        }
                    }
                    uiSchema.add(GRIDS, grids)
                }
                jsonObject.add(UI_SCHEMA, uiSchema)
            }

        elementAdapter.write(out, jsonObject)
    }

    private fun readTriState(grid: JsonObject, key: String) {
        val element = grid.get(key)
        when {
            element == null || element.isJsonNull -> grid.add(key, JsonArray())
            element.isJsonPrimitive && element.asString == ALL -> grid.remove(key)
        }
    }

    private fun dropIfEmpty(grid: JsonObject, key: String) {
        grid.get(key)?.takeIf { it.isJsonArray && it.asJsonArray.size() == 0 }?.let { grid.remove(key) }
    }

    private fun writeTriState(grid: JsonObject, key: String) {
        val element = grid.get(key)
        when {
            element == null || element.isJsonNull -> grid.addProperty(key, ALL)
            element.isJsonArray && element.asJsonArray.size() == 0 -> grid.remove(key)
        }
    }
}
