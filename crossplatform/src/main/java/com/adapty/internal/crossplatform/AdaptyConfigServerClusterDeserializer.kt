package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyConfig.ServerCluster
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

internal class AdaptyConfigServerClusterDeserializer : JsonDeserializer<ServerCluster> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ServerCluster {
        return runCatching { json.asJsonPrimitive.asString }.getOrNull().let { value ->
            when(value) {
                "cn" -> ServerCluster.CN
                "eu" -> ServerCluster.EU
                else -> ServerCluster.DEFAULT
            }
        }
    }
}