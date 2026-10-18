@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.store

import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.ui.FlowViewModel
import com.adapty.ui.internal.ui.FlowViewModelArgs
import kotlinx.coroutines.CoroutineScope

// Handlers backed by the core SDK (store purchases, product/media loading, analytics).
// Each handles its own Effect subtypes only, so their position in the handler list is irrelevant.
internal fun platformEffectHandlers(
    vm: FlowViewModel,
    args: FlowViewModelArgs,
    scope: CoroutineScope,
): List<(Effect, (Message) -> Unit) -> Unit> = listOf(
    PurchaseEffectHandler(scope, args.flowKey, { vm.activityProvider?.invoke() }, { vm.contextAwareListener })::handle,
    DataLoadingEffectHandler(scope, args.flowKey, args.media)::handle,
    AnalyticsEffectHandler(args.flowKey)::handle,
)
