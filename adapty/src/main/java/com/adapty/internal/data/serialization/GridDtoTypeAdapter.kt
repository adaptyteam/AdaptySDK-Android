package com.adapty.internal.data.serialization

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.GridDto
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

internal class GridDtoTypeAdapter : JsonDeserializer<GridDto>, JsonSerializer<GridDto> {

    private companion object {
        const val PLATFORMS = "platforms"
        const val DEVICES = "devices"
        const val CUSTOM_ID = "custom_id"
        const val H_BREAKPOINTS = "h_breakpoints"
        const val V_BREAKPOINTS = "v_breakpoints"
        const val CELLS = "cells"
        const val ALL = "all"
    }

    override fun deserialize(
        jsonElement: JsonElement,
        type: Type,
        context: JsonDeserializationContext,
    ): GridDto {
        val obj = jsonElement as? JsonObject
            ?: throw AdaptyError(
                message = "Grid should be an object",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )

        val cellsEl = obj.get(CELLS)
        if (cellsEl == null || !cellsEl.isJsonArray) {
            throw AdaptyError(
                message = "cells in Grid should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )
        }

        return GridDto(
            platforms = decodeAllOrArray(obj, PLATFORMS),
            devices = decodeAllOrArray(obj, DEVICES),
            customId = obj.get(CUSTOM_ID)?.takeIf { !it.isJsonNull }?.asString,
            hBreakpoints = decodeIntArray(obj, H_BREAKPOINTS),
            vBreakpoints = decodeIntArray(obj, V_BREAKPOINTS),
            cells = cellsEl.asJsonArray.map { it.asInt },
        )
    }

    override fun serialize(
        src: GridDto,
        type: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        val obj = JsonObject()

        encodeAllOrArray(obj, PLATFORMS, src.platforms, context)
        encodeAllOrArray(obj, DEVICES, src.devices, context)
        src.customId?.let { obj.addProperty(CUSTOM_ID, it) }
        if (src.hBreakpoints.isNotEmpty()) obj.add(H_BREAKPOINTS, context.serialize(src.hBreakpoints))
        if (src.vBreakpoints.isNotEmpty()) obj.add(V_BREAKPOINTS, context.serialize(src.vBreakpoints))
        obj.add(CELLS, context.serialize(src.cells))

        return obj
    }

    private fun decodeAllOrArray(obj: JsonObject, key: String): List<String>? {
        val el = obj.get(key)
        return when {
            el == null || el.isJsonNull -> emptyList()
            el.isJsonPrimitive && el.asString == ALL -> null
            el.isJsonArray -> el.asJsonArray.map { it.asString }
            else -> emptyList()
        }
    }

    private fun decodeIntArray(obj: JsonObject, key: String): List<Int> {
        val el = obj.get(key)
        return if (el != null && el.isJsonArray) el.asJsonArray.map { it.asInt } else emptyList()
    }

    private fun encodeAllOrArray(
        obj: JsonObject,
        key: String,
        value: List<String>?,
        context: JsonSerializationContext,
    ) {
        when {
            value == null -> obj.addProperty(key, ALL)
            value.isNotEmpty() -> obj.add(key, context.serialize(value))
        }
    }
}
