@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.store

import com.adapty.ui.internal.ui.FlowViewModel
import com.adapty.ui.internal.ui.FlowViewModelArgs
import kotlinx.coroutines.CoroutineScope
import com.adapty.ui.internal.platform.InternalAdaptyUiApi

// Platform-backed handlers (store purchases, product/media loading, analytics); each handles its own Effect subtypes.
internal expect fun platformEffectHandlers(
    vm: FlowViewModel,
    args: FlowViewModelArgs,
    scope: CoroutineScope,
): List<(Effect, (Message) -> Unit) -> Unit>
