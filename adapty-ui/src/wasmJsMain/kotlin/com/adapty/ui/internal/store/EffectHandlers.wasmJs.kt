@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.store

import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.webStub
import com.adapty.ui.internal.ui.FlowViewModel
import com.adapty.ui.internal.ui.FlowViewModelArgs
import kotlinx.coroutines.CoroutineScope

internal actual fun platformEffectHandlers(
    vm: FlowViewModel,
    args: FlowViewModelArgs,
    scope: CoroutineScope,
): List<(Effect, (Message) -> Unit) -> Unit> {
    webStub("platformEffectHandlers")
    return emptyList()
}
