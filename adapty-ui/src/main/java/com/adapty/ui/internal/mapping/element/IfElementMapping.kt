@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import android.os.Build
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.utils.CONFIGURATION_FORMAT_VERSION
import com.adapty.ui.internal.utils.isSameOrNewerVersionThan

internal fun Map<*, *>.toIfElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val platform = this["platform"]
    val version = this["version"]
    val toVersion = this["to_version"]
    val androidOsVersion = androidMinOsVersion()
    val key = when {
        (platform != null && platform != "android")
                || (version is String && !CONFIGURATION_FORMAT_VERSION.isSameOrNewerVersionThan(version))
                || (toVersion is String && CONFIGURATION_FORMAT_VERSION.isSameOrNewerVersionThan(toVersion))
                || (androidOsVersion != null && !isAndroidOsAtLeast(androidOsVersion)) -> "else"
        else -> {
            listOf("then", "else").firstOrNull { key ->
                hasVideoSupport || (this[key] as? Map<*, *>)?.get("type") != "video"
            } ?: "then"
        }
    }
    return (this[key] as? Map<*, *>)?.let { item -> childMapper(item, inheritShrink) }
        ?: throw adaptyError(
            message = "$key in If must not be empty",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
}

private fun isAndroidOsAtLeast(minVersion: String): Boolean {
    val release = Build.VERSION.RELEASE
    if (release.isEmpty() || !release[0].isDigit()) return true
    return release.isSameOrNewerVersionThan(minVersion)
}

private fun Map<*, *>.androidMinOsVersion(): String? {
    val available = this["available"] as? List<*> ?: return null
    return available.firstNotNullOfOrNull { item ->
        (item as? Map<*, *>)?.takeIf { it["os_name"] == "Android" }?.get("os_version") as? String
    }
}

internal var hasVideoSupport: Boolean = false
