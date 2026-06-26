@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.FallbackPaywallsInfo
import com.adapty.internal.data.models.FallbackVariations
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.FileLocation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.InputStream
import java.io.Reader

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FallbackPaywallRetriever(
    private val appContext: Context,
    private val gson: Gson,
) {
    fun getMetaInfo(source: FileLocation): FallbackPaywallsInfo {
        return when (source) {
            is FileLocation.Uri -> getMetaInfo(source) {
                appContext.contentResolver.openInputStream(source.uri)
            }
            is FileLocation.Asset -> getMetaInfo(source) {
                appContext.assets?.open(source.relativePath)
            }
        }
    }

    private fun getMetaInfo(source: FileLocation, createInputStream: () -> InputStream?): FallbackPaywallsInfo {
        return try {
            createInputStream()?.reader()?.use { reader ->
                val fallbackPaywallsInfo = gson.fromJson<FallbackPaywallsInfo>(reader, FallbackPaywallsInfo::class.java)
                val version = fallbackPaywallsInfo.meta.version
                if (version < CURRENT_FALLBACK_PAYWALL_VERSION) {
                    throw AdaptyError(
                        message = "The fallback file version is not correct. Download a new one from the Adapty Dashboard.",
                        adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER,
                    )
                } else if (version > CURRENT_FALLBACK_PAYWALL_VERSION) {
                    throw AdaptyError(
                        message = "The fallback file version is not correct. Please update the AdaptySDK.",
                        adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER,
                    )
                }

                fallbackPaywallsInfo.copy(location = source)
            } ?: throw AdaptyError(
                message = "Couldn't open fallback file.",
                adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER,
            )
        } catch (e: Exception) {
            if (e is AdaptyError) {
                Logger.log(AdaptyLogLevel.ERROR) { "${e.message}" }
                throw e
            } else {
                val message = "Couldn't set fallback file. $e"
                Logger.log(AdaptyLogLevel.ERROR) { message }
                throw AdaptyError(e, message, AdaptyErrorCode.WRONG_PARAMETER)
            }
        }
    }

    fun getPaywall(source: FileLocation, placementId: String): FallbackVariations? {
        return when (source) {
            is FileLocation.Uri -> getPaywall(placementId) {
                appContext.contentResolver.openInputStream(source.uri)
            }
            is FileLocation.Asset -> getPaywall(placementId) {
                appContext.assets?.open(source.relativePath)
            }
        }
    }

    private fun getPaywall(placementId: String, createInputStream: () -> InputStream?): FallbackVariations? {
        return try {
            createInputStream()?.reader()?.use { reader ->
                val jsonReader = createFilteringReader(reader, "data", placementId)

                jsonReader.use {
                    jsonReader.isLenient = true
                    val fallbackPaywall = gson.fromJson<FallbackVariations>(jsonReader, FallbackVariations::class.java)
                    fallbackPaywall.takeIf {
                        placementId == fallbackPaywall.placementId && fallbackPaywall.data.isNotEmpty()
                    } ?: throw AdaptyError(
                        message = "Couldn't parse fallback variation (placementId: $placementId).${if (fallbackPaywall.data.isEmpty()) " Data is empty." else ""}${if (placementId != fallbackPaywall.placementId) " id (${fallbackPaywall.placementId}) != $placementId." else ""}",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
                    )
                }
            } ?: throw AdaptyError(
                message = "Couldn't open fallback file.",
                adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER,
            )
        } catch (e: Exception) {
            if (e is AdaptyError) {
                Logger.log(AdaptyLogLevel.ERROR) { "${e.message}" }
            } else {
                Logger.log(AdaptyLogLevel.ERROR) { "Couldn't retrieve fallback variation (placementId: $placementId). $e" }
            }
            null
        }
    }

    fun getUiSchema(source: FileLocation, viewConfigurationId: String): Map<String, Any>? {
        return when (source) {
            is FileLocation.Uri -> getUiSchema(viewConfigurationId) {
                appContext.contentResolver.openInputStream(source.uri)
            }
            is FileLocation.Asset -> getUiSchema(viewConfigurationId) {
                appContext.assets?.open(source.relativePath)
            }
        }
    }

    private fun getUiSchema(viewConfigurationId: String, createInputStream: () -> InputStream?): Map<String, Any>? {
        return try {
            createInputStream()?.reader()?.use { reader ->
                val jsonReader = createFilteringReader(reader, "ui_builder", viewConfigurationId)

                jsonReader.use {
                    jsonReader.isLenient = true
                    val type = object : TypeToken<Map<String, Any>>() {}.type
                    val root: Map<String, Any> = gson.fromJson(jsonReader, type)
                    @Suppress("UNCHECKED_CAST")
                    (root["ui_builder"] as? Map<*, *>)?.get(viewConfigurationId) as? Map<String, Any>
                }
            }
        } catch (e: Exception) {
            Logger.log(AdaptyLogLevel.ERROR) { "Couldn't retrieve fallback ui schema (viewConfigurationId: $viewConfigurationId). $e" }
            null
        }
    }

    private fun createFilteringReader(reader: Reader, topLevelName: String, nestedName: String): JsonReader =
        object : JsonReader(reader) {
            var currentDepth = 0
            var skippingMode = false

            override fun beginObject() {
                super.beginObject()
                currentDepth++

                if (skippingMode) {
                    var nextToken = peek()
                    while (nextToken != JsonToken.END_OBJECT) {
                        skipValue()
                        nextToken = peek()
                    }
                    skippingMode = false
                }
            }

            override fun beginArray() {
                super.beginArray()
                currentDepth++

                if (skippingMode) {
                    var nextToken = peek()
                    while (nextToken != JsonToken.END_ARRAY) {
                        skipValue()
                        nextToken = peek()
                    }
                    skippingMode = false
                }
            }

            override fun endObject() {
                super.endObject()
                currentDepth--
            }

            override fun endArray() {
                super.endArray()
                currentDepth--
            }

            override fun nextName(): String {
                val name = super.nextName()
                skippingMode = (currentDepth == 1 && name != topLevelName) || (currentDepth == 2 && name != nestedName)
                return name
            }

            private fun consumeSkippingMode() { skippingMode = false }

            override fun nextString(): String = super.nextString().also { consumeSkippingMode() }
            override fun nextBoolean(): Boolean = super.nextBoolean().also { consumeSkippingMode() }
            override fun nextInt(): Int = super.nextInt().also { consumeSkippingMode() }
            override fun nextLong(): Long = super.nextLong().also { consumeSkippingMode() }
            override fun nextDouble(): Double = super.nextDouble().also { consumeSkippingMode() }
            override fun nextNull() { super.nextNull(); consumeSkippingMode() }
        }

    private companion object {
        private const val CURRENT_FALLBACK_PAYWALL_VERSION = 10
    }
}