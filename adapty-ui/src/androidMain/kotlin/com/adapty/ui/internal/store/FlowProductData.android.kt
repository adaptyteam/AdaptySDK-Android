@file:OptIn(com.adapty.internal.utils.InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.store

import com.adapty.internal.domain.models.BackendProduct
import com.adapty.ui.internal.platform.AdaptyFlow
import com.adapty.ui.internal.platform.AdaptyPaywallProduct

// Core-internal product/flow data (BackendProduct, AdaptyFlowPaywall.products, payloadData).
// Plain typed code only: `?.let` on core-internal types can compile to error/NonExistentClass.

internal actual fun flowProductRefs(flow: AdaptyFlow): List<FlowProductRef> {
    val refs = mutableListOf<FlowProductRef>()
    for (paywall in flow.paywalls) {
        for (product in paywall.products) {
            val backendProduct: BackendProduct = product
            refs.add(
                FlowProductRef(
                    key = resolveProductKey(backendProduct),
                    flowProductId = backendProduct.flowProductId,
                    adaptyProductId = backendProduct.id,
                    vendorProductId = backendProduct.vendorProductId,
                    accessLevelId = backendProduct.accessLevelId,
                    declaredProductType = backendProduct.declaredProductType,
                    paywallVariationId = paywall.variationId,
                    paywallName = paywall.name,
                )
            )
        }
    }
    return refs
}

internal actual fun AdaptyPaywallProduct.payloadFlowProductId(): String? = payloadData.flowProductId

internal actual fun AdaptyPaywallProduct.payloadAdaptyProductId(): String = payloadData.adaptyProductId

private fun resolveProductKey(product: BackendProduct): String =
    product.flowProductId ?: product.id
