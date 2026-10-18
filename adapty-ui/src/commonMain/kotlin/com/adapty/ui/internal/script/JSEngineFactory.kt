@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.script

import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.InternalAdaptyUiApi

// JS engine seams.

internal expect suspend fun createJsEngine(
    ctx: PlatformContextHandle,
    bridge: JSActionBridge,
    onEngineDied: suspend () -> Unit,
): JSEngine

/** Engine death is reported by the engine itself (onEngineDied), not as a script error. */
internal expect fun isJsEngineDead(e: Throwable): Boolean

internal expect fun closeJsEngine(engine: JSEngine?)
