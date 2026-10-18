package com.adapty.ui.internal.store

import com.adapty.ui.internal.platform.AdaptyFlow
import com.adapty.ui.internal.platform.AdaptyPaywallProduct

// Web never runs a live flow (no FlowMode.Live): no backend products.
internal actual fun flowProductRefs(flow: AdaptyFlow): List<FlowProductRef> = emptyList()

internal actual fun AdaptyPaywallProduct.payloadFlowProductId(): String? = flowProductId

internal actual fun AdaptyPaywallProduct.payloadAdaptyProductId(): String = adaptyProductId
