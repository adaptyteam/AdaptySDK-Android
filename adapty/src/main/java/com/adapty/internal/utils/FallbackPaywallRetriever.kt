package com.adapty.internal.utils

import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.FallbackPaywallsInfo
import com.adapty.internal.data.models.FallbackVariations
import com.adapty.utils.AdaptyLogLevel
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FallbackPaywallRetriever(
    private val appContext: Context,
    private val gson: Gson,
) {

    fun getMetaInfo(source: Uri): FallbackPaywallsInfo {
        return try {
            appContext.contentResolver.openInputStream(source)?.reader()?.use { reader ->
                val fallbackPaywallsInfo = gson.fromJson<FallbackPaywallsInfo>(reader, FallbackPaywallsInfo::class.java)
                val version = fallbackPaywallsInfo.meta.version
                if (version < CURRENT_FALLBACK_PAYWALL_VERSION) {
                    throw AdaptyError(
                        message = "The fallback paywalls version is not correct. Download a new one from the Adapty Dashboard.",
                        adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER,
                    )
                } else if (version > CURRENT_FALLBACK_PAYWALL_VERSION) {
                    throw AdaptyError(
                        message = "The fallback paywalls version is not correct. Please update the AdaptySDK.",
                        adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER,
                    )
                }

                fallbackPaywallsInfo.copy(uri = source)
            } ?: throw AdaptyError(
                message = "Couldn't open file with fallback paywalls.",
                adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER,
            )
        } catch (e: Exception) {
            if (e is AdaptyError) {
                Logger.log(AdaptyLogLevel.ERROR) { "${e.message}" }
                throw e
            } else {
                val message = "Couldn't set fallback paywalls. $e"
                Logger.log(AdaptyLogLevel.ERROR) { message }
                throw AdaptyError(e, message, AdaptyErrorCode.WRONG_PARAMETER)
            }
        }
    }

    fun getPaywall(source: Uri, placementId: String): FallbackVariations? {
        return try {
            appContext.contentResolver.openInputStream(source)?.reader()?.use { reader ->
                val jsonReader = object : JsonReader(reader) {
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
                        skippingMode = (currentDepth == 1 && name != "data") || (currentDepth == 2 && name != placementId)
                        return name
                    }
                }

                jsonReader.use {
                    jsonReader.isLenient = true
                    val fallbackPaywall = gson.fromJson<FallbackVariations>(jsonReader, FallbackVariations::class.java)
                    fallbackPaywall.takeIf {
                        placementId == fallbackPaywall.placementId && fallbackPaywall.data.isNotEmpty()
                    } ?: throw AdaptyError(
                        message = "Couldn't parse fallback paywall (placementId: $placementId).${if (fallbackPaywall.data.isEmpty()) " Data is empty." else ""}${if (placementId != fallbackPaywall.placementId) " id (${fallbackPaywall.placementId}) != $placementId." else ""}",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
                    )
                }
            } ?: throw AdaptyError(
                message = "Couldn't open file with fallback paywalls.",
                adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER,
            )
        } catch (e: Exception) {
            if (e is AdaptyError) {
                Logger.log(AdaptyLogLevel.ERROR) { "${e.message}" }
            } else {
                Logger.log(AdaptyLogLevel.ERROR) { "Couldn't retrieve fallback paywall (placementId: $placementId). $e" }
            }
            null
        }
    }

    private companion object {
        private const val CURRENT_FALLBACK_PAYWALL_VERSION = 5
    }
}