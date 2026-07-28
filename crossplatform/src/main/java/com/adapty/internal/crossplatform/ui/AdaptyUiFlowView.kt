package com.adapty.internal.crossplatform.ui

import com.adapty.models.AdaptyFlow
import java.util.UUID

class AdaptyUiFlowView(
    val id: String,
    val placementId: String,
    val variationId: String,
    /**
     * The localization the view was actually built with: the requested locale when one was
     * passed and resolved, and the flow's default localization otherwise.
     */
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