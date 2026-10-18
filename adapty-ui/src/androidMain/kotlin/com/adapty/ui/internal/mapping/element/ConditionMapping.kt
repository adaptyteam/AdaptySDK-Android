@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import android.content.res.Resources
import android.os.Build
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.ui.attributes.Condition
import com.adapty.ui.internal.utils.CONFIGURATION_FORMAT_VERSION
import com.adapty.ui.internal.utils.isSameOrNewerVersionThan

private const val PLATFORM = "android"

private const val TABLET_MIN_SW_DP = 600

internal enum class DeviceKind(val rawValue: String) {
    PHONE("phone"),
    TAB("tab");

    companion object {
        fun current(): DeviceKind =
            if (Resources.getSystem().configuration.smallestScreenWidthDp >= TABLET_MIN_SW_DP) TAB else PHONE
    }
}

internal fun Map<*, *>.matchesStaticConditionFields(deviceKind: DeviceKind): Boolean =
    platformMatches() && deviceMatches(deviceKind) && versionMatches() && availableMatches()

internal fun List<*>?.parseConditionGroup(deviceKind: DeviceKind): List<Condition>? {
    this ?: return emptyList()
    val runtime = ArrayList<Condition>(size)
    for (raw in this) {
        val map = raw as? Map<*, *> ?: continue
        when (val resolution = map.resolveCondition(deviceKind)) {
            ConditionResolution.AlwaysTrue -> {}
            ConditionResolution.AlwaysFalse -> return null
            is ConditionResolution.Runtime -> runtime.add(resolution.condition)
        }
    }
    return runtime
}

private sealed interface ConditionResolution {
    object AlwaysTrue : ConditionResolution
    object AlwaysFalse : ConditionResolution
    data class Runtime(val condition: Condition) : ConditionResolution
}

private fun Map<*, *>.resolveCondition(deviceKind: DeviceKind): ConditionResolution {
    if (containsKey("metric")) {
        val metric = parseMetric(this["metric"])
        val min = (this["min"] as? Number)?.toFloat()
        val max = (this["max"] as? Number)?.toFloat()
        return if (min == null && max == null) ConditionResolution.AlwaysTrue
        else ConditionResolution.Runtime(Condition.SizeThreshold(metric, min, max))
    }
    if (containsKey("orientation")) {
        return ConditionResolution.Runtime(Condition.OrientationIs(parseOrientation(this["orientation"])))
    }
    if (containsKey("platform")) return staticResolution(platformMatches())
    if (containsKey("device")) return staticResolution(deviceMatches(deviceKind))
    if (containsKey("version") || containsKey("to_version")) return staticResolution(versionMatches())
    if (containsKey("available")) return staticResolution(availableMatches())
    throw adaptyError(
        message = "Condition must contain one of: metric, orientation, platform, device, version, to_version, available",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
    )
}

private fun staticResolution(matches: Boolean): ConditionResolution =
    if (matches) ConditionResolution.AlwaysTrue else ConditionResolution.AlwaysFalse

private fun parseMetric(value: Any?): Condition.Metric =
    when (value) {
        "available_width" -> Condition.Metric.AvailableWidth
        "available_height" -> Condition.Metric.AvailableHeight
        "screen_width" -> Condition.Metric.ScreenWidth
        "screen_height" -> Condition.Metric.ScreenHeight
        else -> throw adaptyError(
            message = "unknown metric: $value",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
        )
    }

private fun parseOrientation(value: Any?): Condition.Orientation =
    when (value) {
        "landscape" -> Condition.Orientation.Landscape
        "portrait" -> Condition.Orientation.Portrait
        else -> throw adaptyError(
            message = "unknown orientation: $value",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
        )
    }

private fun Map<*, *>.platformMatches(): Boolean {
    val platforms = when (val value = this["platform"]) {
        null -> return true
        is String -> listOf(value)
        is List<*> -> value
        else -> return true
    }
    return platforms.any { it == PLATFORM }
}

private fun Map<*, *>.deviceMatches(deviceKind: DeviceKind): Boolean {
    val devices = this["device"] as? List<*> ?: return true
    return devices.any { it == deviceKind.rawValue }
}

private fun Map<*, *>.versionMatches(): Boolean {
    val version = this["version"]
    if (version is String && !CONFIGURATION_FORMAT_VERSION.isSameOrNewerVersionThan(version)) return false
    val toVersion = this["to_version"]
    if (toVersion is String && CONFIGURATION_FORMAT_VERSION.isSameOrNewerVersionThan(toVersion)) return false
    return true
}

private fun Map<*, *>.availableMatches(): Boolean {
    val minOsVersion = androidMinOsVersion() ?: return true
    return isAndroidOsAtLeast(minOsVersion)
}

private fun Map<*, *>.androidMinOsVersion(): String? {
    val available = this["available"] as? List<*> ?: return null
    return available.firstNotNullOfOrNull { item ->
        (item as? Map<*, *>)?.takeIf { it["os_name"] == "Android" }?.get("os_version") as? String
    }
}

private fun isAndroidOsAtLeast(minVersion: String): Boolean {
    val release = Build.VERSION.RELEASE
    if (release.isEmpty() || !release[0].isDigit()) return true
    return release.isSameOrNewerVersionThan(minVersion)
}
