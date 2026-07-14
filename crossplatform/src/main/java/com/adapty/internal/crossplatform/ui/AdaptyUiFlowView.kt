package com.adapty.internal.crossplatform.ui

import com.adapty.models.AdaptyFlow
import java.util.UUID

class AdaptyUiFlowView(
    val id: String,
    val placementId: String,
    val variationId: String,
) {
    constructor(flow: AdaptyFlow, id: String = UUID.randomUUID().toString()): this(
        id = id,
        placementId = flow.placement.id,
        variationId = flow.variationId
    )
}