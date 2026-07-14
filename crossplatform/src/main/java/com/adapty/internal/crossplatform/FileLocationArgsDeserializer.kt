package com.adapty.internal.crossplatform

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

internal class FileLocationArgsDeserializer : JsonDeserializer<FileLocationArgs> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): FileLocationArgs? {
        return (json as? JsonObject)?.let { jsonObject ->
            val assetId = runCatching {
                jsonObject.getAsJsonPrimitive("asset_id").asString.takeIf(String::isNotEmpty)
            }.getOrNull()

            if (assetId != null)
                return FileLocationArgs(assetId)

            val path = runCatching {
                jsonObject.getAsJsonPrimitive("path").asString.takeIf(String::isNotEmpty)
            }.getOrNull() ?: return null

            return FileLocationArgs(path)
        }
    }
}