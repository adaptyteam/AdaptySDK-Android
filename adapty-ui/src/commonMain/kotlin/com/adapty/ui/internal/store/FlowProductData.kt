package com.adapty.ui.internal.store

import com.adapty.ui.internal.platform.AdaptyFlow
import com.adapty.ui.internal.platform.AdaptyPaywallProduct

// One backend product of a live flow, in flow.paywalls × paywall.products order.
internal class FlowProductRef(
    // flowProductId ?: adaptyProductId — the key products are associated by
    val key: String,
    val flowProductId: String?,
    val adaptyProductId: String,
    val vendorProductId: String,
    val accessLevelId: String,
    val declaredProductType: String,
    val paywallVariationId: String,
    val paywallName: String,
)

internal expect fun flowProductRefs(flow: AdaptyFlow): List<FlowProductRef>

internal expect fun AdaptyPaywallProduct.payloadFlowProductId(): String?

internal expect fun AdaptyPaywallProduct.payloadAdaptyProductId(): String
