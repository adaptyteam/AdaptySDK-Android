package com.adapty.internal.utils

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import android.os.Process
import android.os.SystemClock
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.data.models.AnalyticsEvent.UserAgentIdleFallbackData
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class UserAgentRetriever(
    private val appContext: Context,
    private val cacheRepository: CacheRepository,
    private val analyticsTracker: AnalyticsTracker,
) {

    private val webViewVersionOnStart = getWebViewVersionIfAvailable()

    private val isRetrievalScheduled = AtomicBoolean(false)

    private val retrievedUserAgent = CompletableDeferred<String?>()

    private val mainHandler = Handler(Looper.getMainLooper())

    private val attemptMarkFile by lazy { File(appContext.noBackupFilesDir, ATTEMPT_MARK_FILE_NAME) }

    fun getUserAgentIfAvailable(): Flow<String?> =
        flow {
            scheduleRetrieval()
            emit(retrievedUserAgent.await())
        }

    private fun scheduleRetrieval() {
        if (!isRetrievalScheduled.compareAndSet(false, true))
            return

        execute {
            val webViewVersion = kotlin.runCatching { getWebViewVersionToRead() }
                .onFailure { e -> Logger.log(ERROR) { "Failed to check user agent attempt: ${e.localizedMessage}" } }
                .getOrNull()

            if (webViewVersion == null) {
                retrievedUserAgent.complete(null)
                return@execute
            }

            val scheduledAt = SystemClock.uptimeMillis()
            val readOnIdleOrFallback = object : Runnable, MessageQueue.IdleHandler {
                override fun queueIdle(): Boolean {
                    mainHandler.removeCallbacks(this)
                    completeRetrievalIfPending(webViewVersion, scheduledAt, isIdleFallback = false)
                    return false
                }

                override fun run() {
                    Looper.myQueue().removeIdleHandler(this)
                    completeRetrievalIfPending(webViewVersion, scheduledAt, isIdleFallback = true)
                }
            }
            mainHandler.postDelayed(readOnIdleOrFallback, IDLE_FALLBACK_DELAY_MILLIS)
            runOnMain { Looper.myQueue().addIdleHandler(readOnIdleOrFallback) }
        }
    }

    private fun getWebViewVersionToRead(): String? {
        val webViewVersion = webViewVersionOnStart ?: return null

        if (readAttemptMark() == webViewVersion)
            return null

        return webViewVersion
    }

    private fun readAttemptMark(): String? {
        cacheRepository.getUserAgentAttempt()?.let { legacyMark ->
            attemptMarkFile.writeText(legacyMark)
            cacheRepository.clearUserAgentAttempt()
        }

        return attemptMarkFile.takeIf(File::exists)?.readText()
    }

    private fun completeRetrievalIfPending(webViewVersion: String, scheduledAt: Long, isIdleFallback: Boolean) {
        if (retrievedUserAgent.isCompleted)
            return

        val startedAt = SystemClock.uptimeMillis()
        val userAgent = kotlin.runCatching { retrieveUserAgent(webViewVersion) }
            .onFailure { e -> Logger.log(ERROR) { "Failed to retrieve user agent: ${e.localizedMessage}" } }
            .getOrNull()
        val waitedMs = startedAt - scheduledAt
        val readMs = SystemClock.uptimeMillis() - startedAt
        Logger.log(VERBOSE) {
            "User agent read on ${if (isIdleFallback) "idle fallback" else "main thread idle"} $waitedMs ms after scheduling, took $readMs ms"
        }
        if (isIdleFallback)
            analyticsTracker.trackSystemEvent(UserAgentIdleFallbackData.create(waitedMs, readMs))
        retrievedUserAgent.complete(userAgent)
    }

    private fun retrieveUserAgent(webViewVersion: String): String? {
        if (providerUpdatedSinceProcessStart())
            return null

        attemptMarkFile.writeText(webViewVersion)
        val userAgent = retrieveFromWebSettings()
        execute { attemptMarkFile.delete() }

        return userAgent
    }

    private fun retrieveFromWebSettings(): String? =
        kotlin.runCatching { WebSettings.getDefaultUserAgent(appContext) }.getOrNull()?.takeIf(String::isNotEmpty)

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

    private companion object {
        const val IDLE_FALLBACK_DELAY_MILLIS = 1500L
        const val ATTEMPT_MARK_FILE_NAME = "AdaptyWV"
    }
}
