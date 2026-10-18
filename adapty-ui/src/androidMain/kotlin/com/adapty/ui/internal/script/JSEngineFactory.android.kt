@file:OptIn(InternalAdaptyUiApi::class, com.adapty.internal.utils.InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.script

import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.data.models.AnalyticsEvent
import com.adapty.internal.di.Dependencies
import com.adapty.ui.internal.platform.AtomicBool
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.WARN
import com.adapty.ui.internal.platform.platformMessage
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log

// androidx.javascriptengine sandbox first, WebView fallback (reported once per process) otherwise.
internal actual suspend fun createJsEngine(
    ctx: PlatformContextHandle,
    bridge: JSActionBridge,
    onEngineDied: suspend () -> Unit,
): JSEngine {
    val context = ctx.context
    val gson = PlatformJson.gson
    val androidx = JSEngineAndroidx(context, bridge, gson)
    return try {
        androidx.initialize()
        androidx.also { js ->
            js.onSandboxDied = {
                log(WARN) { "$LOG_PREFIX Sandbox died — re-setting up bridge functions" }
                onEngineDied()
            }
        }
    } catch (e: Throwable) {
        log(WARN) { "$LOG_PREFIX JSEngineAndroidx unavailable, falling back to JSEngineWebView: ${e.platformMessage}" }
        reportWebViewFallback(e)
        try { androidx.close() } catch (_: Throwable) {}
        JSEngineWebView(context, bridge, gson).also { js ->
            js.onRendererGone = {
                log(WARN) { "$LOG_PREFIX WebView renderer died — re-setting up bridge functions" }
                onEngineDied()
            }
            js.initialize()
        }
    }
}

// Sandbox death is reported by the engine itself (onEngineDied), not as a script error
internal actual fun isJsEngineDead(e: Throwable): Boolean = isSandboxDead(e)

internal actual fun closeJsEngine(engine: JSEngine?) {
    when (engine) {
        is JSEngineAndroidx -> engine.close()
        is JSEngineWebView -> engine.close()
    }
}

private val webViewFallbackReported = AtomicBool(false)

private fun reportWebViewFallback(cause: Throwable) {
    if (!webViewFallbackReported.compareAndSet(false, true)) return
    runCatching {
        Dependencies.injectInternal<AnalyticsTracker>(named = "base").trackSystemEvent(
            AnalyticsEvent.InternalEventData.create(
                eventName = "webview_js_engine_fallback",
                error = cause.toString().take(200),
            )
        )
    }
}
