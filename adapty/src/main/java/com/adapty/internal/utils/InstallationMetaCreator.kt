package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.InstallationMeta

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class InstallationMetaCreator(
    private val metaInfoRetriever: MetaInfoRetriever,
) {

    fun create(adId: String, appSetId: String, storeCountry: String): InstallationMeta {
        val (appBuild, appVersion) = metaInfoRetriever.appBuildAndVersion

        return InstallationMeta(
            deviceId = metaInfoRetriever.installationMetaId,
            adaptySdkVersion = metaInfoRetriever.adaptySdkVersion,
            advertisingId = adId,
            appSetId = appSetId,
            androidId = metaInfoRetriever.androidId,
            appBuild = appBuild,
            appVersion = appVersion,
            device = metaInfoRetriever.deviceName,
            locale = metaInfoRetriever.currentLocaleFormatted,
            os = metaInfoRetriever.os,
            platform = metaInfoRetriever.platform,
            storeCountry = storeCountry.takeIf(String::isNotEmpty),
            timezone = metaInfoRetriever.timezone,
            userAgent = metaInfoRetriever.userAgent,
        )
    }
}