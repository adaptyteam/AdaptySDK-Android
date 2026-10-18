package com.adapty.ui.internal.ui

import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.RemoteImage
import java.math.BigDecimal
import java.util.Locale

// Narrow seams between the flow renderer and the core SDK; Android adapters live in FlowDepsAdapters.kt

internal interface TimerValueStore {
    fun getLong(key: String, isPersisted: Boolean): Long?
    fun setLong(key: String, value: Long, isPersisted: Boolean)
}

internal interface SdkMetaInfo {
    val appBuildAndVersion: Pair<String, String>
    val currentLocale: Locale?
    val userLocales: List<String>
    val adaptySdkVersion: String
    val os: String
    val deviceName: String
    val applicationId: String
    val manufacturer: String
    val is24HourFormat: Boolean
}

internal interface PriceFormatting {
    fun format(newPriceValue: BigDecimal, originalFormattedPrice: String): String
}

internal interface MediaSource {
    fun loadImage(
        remoteImage: RemoteImage,
        handlePreview: ((preview: Image) -> Unit)?,
        handleResult: ((image: Image) -> Unit)?,
    )
}
