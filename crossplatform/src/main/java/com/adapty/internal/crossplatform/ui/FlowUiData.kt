@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.adapty.internal.crossplatform.ui

import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyUI

internal class FlowUiData(
    val config: AdaptyUI.FlowConfiguration,
    val products: List<AdaptyPaywallProduct>?,
    val productPurchaseParams: Map<String, AdaptyPurchaseParameters>?,
    val customTags: Map<String, String>?,
    val customTimers: Map<String, String>?,
    val customAssets: AdaptyCustomAssets?,
    val enableSafeAreaPaddings: Boolean,
    val view: AdaptyUiFlowView,
) {
    constructor(
        config: AdaptyUI.FlowConfiguration,
        products: List<AdaptyPaywallProduct>?,
        createFlowViewArgs: CreateFlowViewArgs,
        view: AdaptyUiFlowView,
    ) : this(
        config,
        products,
        createFlowViewArgs.productPurchaseParameters,
        createFlowViewArgs.customTags,
        createFlowViewArgs.customTimers,
        createFlowViewArgs.customAssets,
        createFlowViewArgs.enableSafeAreaPaddings,
        view,
    )

    operator fun component1() = config

    operator fun component2() = products

    operator fun component3() = customTags

    operator fun component4() = customTimers

    operator fun component5() = customAssets
}
