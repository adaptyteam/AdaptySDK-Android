package com.adapty.internal.crossplatform.ui

import com.adapty.models.AdaptyFlow
import java.util.UUID

class AdaptyUiFlowView(
    val id: String,
    val placementId: String,
    val variationId: String,
    val locale: String? = null,
) {
    constructor(
        flow: AdaptyFlow,
        id: String = UUID.randomUUID().toString(),
        locale: String? = null,
    ): this(
        id = id,
        placementId = flow.placement.id,
        variationId = flow.variationId,
        locale = locale,
    )
}