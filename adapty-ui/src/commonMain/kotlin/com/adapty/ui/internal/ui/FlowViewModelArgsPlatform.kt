@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.ui

import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.InternalAdaptyUiApi

/** Live-SDK view-model args (locale taken from the platform context); null when the SDK isn't ready. */
internal expect fun FlowViewModelArgs.Companion.create(
    flowKey: String,
    ctx: PlatformContextHandle,
): FlowViewModelArgs?
