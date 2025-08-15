package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.models.AdaptyInstallationDetails
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class InstallationPayloadMapper(
    private val gson: Gson,
) {

    private val dataMapType by lazy {
        object : TypeToken<HashMap<String, Any>>() {}.type
    }

    fun map(json: String) =
        AdaptyInstallationDetails.Payload(
            json,
            kotlin.runCatching { gson.fromJson<Map<String, Any>>(json, dataMapType) }
                .getOrElse { e ->
                    Logger.log(ERROR) {
                        "Couldn't parse installation payload json: ${e.localizedMessage}"
                    }
                    mapOf()
                }
                .orEmpty()
                .immutableWithInterop()
        )
}
