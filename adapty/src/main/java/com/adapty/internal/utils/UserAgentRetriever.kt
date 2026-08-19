package com.adapty.internal.utils

import android.content.Context
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class UserAgentRetriever(
    private val appContext: Context,
    private val cacheRepository: CacheRepository,
) {

    private val semaphore = Semaphore(1)

    private val webViewVersionOnStart = getWebViewVersionIfAvailable()

    @Volatile
    private var cachedUserAgent: String? = null

    fun getUserAgentIfAvailable(): Flow<String> =
        flow {
            cachedUserAgent?.let { userAgent ->
                emit(userAgent)
                return@flow
            }

            semaphore.acquire()
            cachedUserAgent?.let { userAgent ->
                semaphore.release()
                emit(userAgent)
                return@flow
            }

            val userAgent = retrieveUserAgent()
            cachedUserAgent = userAgent
            semaphore.release()
            emit(userAgent)
        }

    private fun retrieveUserAgent(): String {
        val webViewVersion = webViewVersionOnStart ?: return ""

        if (providerUpdatedSinceProcessStart())
            return ""

        if (cacheRepository.getUserAgentAttempt() == webViewVersion)
            return ""

        cacheRepository.saveUserAgentAttempt(webViewVersion)
        val userAgent = retrieveFromWebSettings()
        cacheRepository.clearUserAgentAttempt()

        return userAgent
    }

    private fun retrieveFromWebSettings(): String =
        kotlin.runCatching { WebSettings.getDefaultUserAgent(appContext) }.getOrNull().orEmpty()

    private fun providerUpdatedSinceProcessStart(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return false

        val webViewPackage = kotlin.runCatching { WebView.getCurrentWebViewPackage() }.getOrNull()
            ?: return true

        if (webViewPackage.versionName != null && webViewPackage.versionName != webViewVersionOnStart)
            return true

        val processStartWallClock =
            System.currentTimeMillis() - (SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime())
        return webViewPackage.lastUpdateTime > processStartWallClock
    }

    private fun getWebViewVersionIfAvailable(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return Build.VERSION.RELEASE

        val webViewPackage = kotlin.runCatching { WebView.getCurrentWebViewPackage() }
            .getOrNull()
            ?.takeIf { it.applicationInfo?.enabled != false }
            ?: return null

        return webViewPackage.versionName ?: Build.VERSION.RELEASE
    }
}
