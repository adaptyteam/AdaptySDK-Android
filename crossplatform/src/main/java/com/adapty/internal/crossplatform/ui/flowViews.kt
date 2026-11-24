package com.adapty.internal.crossplatform.ui

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.adapty.ui.AdaptyFlowView

private class EmbeddedFlowViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

internal fun AdaptyFlowView.useOwnViewModelStore() {
    if (isAttachedToWindow || ownViewModelStoreOwner() != null) return
    setViewTreeViewModelStoreOwner(EmbeddedFlowViewModelStoreOwner())
}

internal fun AdaptyFlowView.clearOwnViewModelStore() {
    ownViewModelStoreOwner()?.viewModelStore?.clear()
}

private fun AdaptyFlowView.ownViewModelStoreOwner() =
    findViewTreeViewModelStoreOwner() as? EmbeddedFlowViewModelStoreOwner
