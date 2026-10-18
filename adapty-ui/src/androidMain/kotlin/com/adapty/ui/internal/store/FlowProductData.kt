package com.adapty.ui.internal.store

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
