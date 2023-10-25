package com.adapty.internal.utils

import android.content.Context
import android.os.Build
import android.provider.Settings.Secure
import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class MetaInfoRetriever(
    private val appContext: Context,
    private val crossplatformMetaRetriever: CrossplatformMetaRetriever,
    private val cacheRepository: CacheRepository,
) {

    @get:JvmSynthetic
    val installationMetaId get() = cacheRepository.getInstallationMetaId()

    @get:JvmSynthetic
    val appBuildAndVersion: Pair<String, String> by lazy {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            .let { packageInfo ->
                val appBuild = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    "${packageInfo.longVersionCode}"
                } else {
                    "${packageInfo.versionCode}"
                }
                val appVersion = packageInfo.versionName

                appBuild to appVersion
            }
    }

    @JvmSynthetic
    val deviceName =
        (if (Build.MODEL.startsWith(Build.MANUFACTURER)) Build.MODEL else "${Build.MANUFACTURER} ${Build.MODEL}")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }

    @JvmSynthetic
    val adaptySdkVersion = com.adapty.BuildConfig.VERSION_NAME

    @get:JvmSynthetic
    val crossplatformNameAndVersion by lazy {
        crossplatformMetaRetriever.crossplatformNameAndVersion
    }

    @get:JvmSynthetic
    val currentLocale get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appContext.resources.configuration.locales.get(0)
        } else {
            appContext.resources.configuration.locale
        }

    @get:JvmSynthetic
    val currentLocaleFormatted get() =
        currentLocale?.let { locale ->
            if (locale.country.isNullOrEmpty()) locale.language else "${locale.language}-${locale.country}"
        }

    @JvmSynthetic
    val os = Build.VERSION.RELEASE

    @JvmSynthetic
    val platform = "Android"

    @get:JvmSynthetic
    val androidId get() = Secure.getString(appContext.contentResolver, Secure.ANDROID_ID)

    @get:JvmSynthetic
    val timezone get() = TimeZone.getDefault().id

    @JvmSynthetic
    val builderVersion = "2.0.0"
}