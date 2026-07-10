package com.adapty.internal.crossplatform

import com.adapty.ui.AdaptyUI.MediaCacheConfiguration
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

internal class AdaptyUIMediaCacheConfigurationDeserializer : JsonDeserializer<MediaCacheConfiguration> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): MediaCacheConfiguration {
        val diskCacheSizeLimit = kotlin.runCatching {
            (json as? JsonObject)?.getAsJsonPrimitive("disk_storage_size_limit")?.asNumber?.toLong()
        }.getOrNull()
        return MediaCacheConfiguration.Builder()
            .run {
                if (diskCacheSizeLimit == null)
                    this
                else
                    this.overrideDiskStorageSizeLimit(diskCacheSizeLimit)
            }
            .build()
    }
}