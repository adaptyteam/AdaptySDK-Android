package com.adapty.internal.utils

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.models.InstallationMeta
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class InstallationMetaCreator(
    private val appContext: Context,
    private val cacheRepository: CacheRepository,
) {

    fun create(adId: String?): InstallationMeta {
        val appBuild: String
        val appVersion: String
        appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            .let { packageInfo ->
                appBuild = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    "${packageInfo.longVersionCode}"
                } else {
                    "${packageInfo.versionCode}"
                }
                appVersion = packageInfo.versionName
            }

        return InstallationMeta(
            deviceId = cacheRepository.getInstallationMetaId(),
            adaptySdkVersion = com.adapty.BuildConfig.VERSION_NAME,
            advertisingId = adId,
            appBuild = appBuild,
            appVersion = appVersion,
            device = cacheRepository.deviceName,
            locale = getCurrentLocale(appContext)?.let { "${it.language}_${it.country}" },
            os = Build.VERSION.RELEASE,
            platform = "Android",
            timezone = TimeZone.getDefault().id,
        )
    }
}