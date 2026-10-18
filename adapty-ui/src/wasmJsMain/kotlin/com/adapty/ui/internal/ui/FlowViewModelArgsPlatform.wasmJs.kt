package com.adapty.ui.internal.ui

import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.webStub

internal actual fun FlowViewModelArgs.Companion.create(
    flowKey: String,
    ctx: PlatformContextHandle,
): FlowViewModelArgs? {
    webStub("FlowViewModelArgs.create")
    return null
}
