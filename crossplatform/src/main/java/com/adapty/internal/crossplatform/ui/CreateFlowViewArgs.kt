package com.adapty.internal.crossplatform.ui

import com.adapty.models.AdaptyFlow
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.ui.AdaptyCustomAssets
import com.adapty.utils.TimeInterval

class CreateFlowViewArgs(
    val flow: AdaptyFlow,
    val loadTimeout: TimeInterval?,
    val preloadProducts: Boolean,
    val customTags: Map<String, String>?,
    val customTimers: Map<String, String>?,
    val customAssets: AdaptyCustomAssets?,
    val productPurchaseParameters: Map<String, AdaptyPurchaseParameters>?,
    val enableSafeAreaPaddings: Boolean,
)