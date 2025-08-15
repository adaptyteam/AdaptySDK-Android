package com.adapty.internal.utils

import android.content.Context
import android.os.Build
import android.provider.Settings.Secure
import android.util.DisplayMetrics
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cache.INSTALLATION_META_ID
import com.adapty.internal.data.cache.PreferenceManager
import com.adapty.utils.AdaptyLogLevel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class MetaInfoRetriever(
    private val appContext: Context,
    private val crossplatformMetaRetriever: CrossplatformMetaRetriever,
    private val adaptyUiAccessor: AdaptyUiAccessor,
    private val userAgentRetriever: UserAgentRetriever,
    private val cacheRepository: CacheRepository,
    private val preferenceManager: PreferenceManager,
) {

    @get:JvmSynthetic
    val installationMetaId get() = cacheRepository.getInstallationMetaId()

    val applicationId = appContext.packageName

    fun isJustInstalled(): Boolean = preferenceManager.getString(INSTALLATION_META_ID) == null

    @get:JvmSynthetic
    val appBuildAndVersion: Pair<String, String> by lazy {
        appContext.packageManager.getPackageInfo(applicationId, 0)
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

    val displayMetrics: DisplayMetrics by lazy {
        appContext.resources.displayMetrics
    }

    @JvmSynthetic
    val deviceName =
        (if (Build.MODEL.startsWith(Build.MANUFACTURER)) Build.MODEL else "${Build.MANUFACTURER} ${Build.MODEL}")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }

    @JvmSynthetic
    val adaptySdkVersion = VERSION_NAME

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

    @JvmSynthetic
    val store = "play_store"

    @get:JvmSynthetic
    val userAgent get() = userAgentRetriever.userAgent

    @get:JvmSynthetic
    val androidId get() = Secure.getString(appContext.contentResolver, Secure.ANDROID_ID)

    @get:JvmSynthetic
    val timezone get() = TimeZone.getDefault().id

    @get:JvmSynthetic
    val adaptyUiVersionOrNull by lazy {
        adaptyUiAccessor.adaptyUiVersion
    }

    @get:JvmSynthetic
    val adaptyUiVersion get() = adaptyUiVersionOrNull
        ?: throwWrongParamError("Unable to retrieve the version of Adapty UI. Please ensure that the dependency is added to the project.")

    @get:JvmSynthetic
    val builderVersion by lazy {
        adaptyUiAccessor.builderVersion
    }

    fun formatDateTimeGMT(timestampMillis: Long = -1): String =
        dateFormatter.format(getDate(timestampMillis))

    fun getDate(timestampMillis: Long = -1) =
        if (timestampMillis == -1L) Calendar.getInstance().time
        else Date(timestampMillis)

    private val dateFormatter: DateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }

    private fun throwWrongParamError(message: String): Nothing {
        Logger.log(AdaptyLogLevel.ERROR) { message }
        throw AdaptyError(
            message = message,
            adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER
        )
    }
}