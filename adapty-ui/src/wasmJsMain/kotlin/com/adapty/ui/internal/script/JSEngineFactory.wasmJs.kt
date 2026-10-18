package com.adapty.ui.internal.script

import com.adapty.ui.internal.platform.PlatformContextHandle

// An iframe realm does not die on its own, so onEngineDied is never invoked.
internal actual suspend fun createJsEngine(
    ctx: PlatformContextHandle,
    bridge: JSActionBridge,
    onEngineDied: suspend () -> Unit,
): JSEngine = JSEngineBrowser(bridge).also { it.initialize() }

internal actual fun isJsEngineDead(e: Throwable): Boolean = false

internal actual fun closeJsEngine(engine: JSEngine?) {
    (engine as? JSEngineBrowser)?.close()
}
