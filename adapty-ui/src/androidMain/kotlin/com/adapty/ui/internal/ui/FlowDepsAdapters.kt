@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui

import android.content.Context
import android.os.Build
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.MetaInfoRetriever
import com.adapty.internal.utils.PriceFormatter
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.RemoteImage
import com.adapty.ui.internal.cache.MediaFetchService
import java.math.BigDecimal
import java.util.Locale

internal class MediaFetchServiceSource(
    private val mediaFetchService: MediaFetchService,
) : MediaSource {
    override fun loadImage(
        remoteImage: RemoteImage,
        handlePreview: ((preview: Image) -> Unit)?,
        handleResult: ((image: Image) -> Unit)?,
    ) = mediaFetchService.loadImage(remoteImage, handlePreview, handleResult)
}

internal class CacheRepositoryTimerStore(
    private val cacheRepository: CacheRepository,
) : TimerValueStore {
    override fun getLong(key: String, isPersisted: Boolean): Long? =
        cacheRepository.getLongValue(key, isPersisted)

    override fun setLong(key: String, value: Long, isPersisted: Boolean) {
        cacheRepository.setLongValue(key, value, isPersisted)
    }
}

internal class CorePriceFormatting(
    private val priceFormatter: PriceFormatter,
) : PriceFormatting {
    override fun format(newPriceValue: BigDecimal, originalFormattedPrice: String): String =
        priceFormatter.format(newPriceValue, originalFormattedPrice)
}

// Getters delegate on every access: currentLocale and is24HourFormat are dynamic in the core/framework
internal class MetaInfoRetrieverSdkMetaInfo(
    private val metaInfoRetriever: MetaInfoRetriever,
    private val context: Context,
) : SdkMetaInfo {
    override val appBuildAndVersion: Pair<String, String> get() = metaInfoRetriever.appBuildAndVersion
    override val currentLocale: Locale? get() = metaInfoRetriever.currentLocale
    override val adaptySdkVersion: String get() = metaInfoRetriever.adaptySdkVersion
    override val os: String get() = metaInfoRetriever.os
    override val deviceName: String get() = metaInfoRetriever.deviceName
    override val applicationId: String get() = metaInfoRetriever.applicationId
    override val manufacturer: String get() = Build.MANUFACTURER
    override val is24HourFormat: Boolean get() = android.text.format.DateFormat.is24HourFormat(context)

    override val userLocales: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = android.os.LocaleList.getDefault()
            (0 until localeList.size()).map { i -> formatLocale(localeList[i]) }
        } else {
            val current: Locale? = currentLocale
            listOf(if (current != null) formatLocale(current) else Locale.getDefault().language)
        }

    private fun formatLocale(loc: Locale): String =
        if (loc.country.isNullOrEmpty()) loc.language else "${loc.language}-${loc.country}"
}
