package com.adapty.ui.internal.utils

import com.adapty.models.AdaptyFlow
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal sealed class FlowMode {

    abstract val placementId: String

    class Live(val flow: AdaptyFlow) : FlowMode() {
        override val placementId: String get() = flow.placement.id
    }
    
    object Preview : FlowMode() {
        override val placementId: String = "preview"
    }
}

@OptIn(ExperimentalContracts::class)
internal fun FlowMode.isLive(): Boolean {
    contract {
        returns(true) implies (this@isLive is FlowMode.Live)
    }
    return this is FlowMode.Live
}
