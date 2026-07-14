@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.attributes

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.internal.ui.element.Condition
import com.adapty.ui.internal.utils.DEFAULT_PRODUCT_GROUP
import com.adapty.ui.internal.utils.Scope

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.toAction(): Action {
    val func = (this["func"] as? String)
        ?: throw adaptyError(
            message = "Couldn't find 'func' for action",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

    val params = (this["params"] as? Map<String, Any?>) ?: emptyMap()
    val scope = this["scope"].toScope(Scope.Screen)

    return Action(func, params, scope)
}

internal fun Map<*, *>.toCondition(): Condition {
    return when(this["type"]) {
        "selected_section" -> {
            val sectionId = (this["section_id"] as? String)
                ?: throw adaptyError(
                    message = "Couldn't find 'section_id' for a 'selected_section' condition",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            val index = (this["index"] as? Number)?.toInt()
                ?: throw adaptyError(
                    message = "Couldn't find 'index' for a 'selected_section' condition",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            Condition.SelectedSection(sectionId, index)
        }
        "selected_product" -> {
            val productId = (this["product_id"] as? String)
                ?: throw adaptyError(
                    message = "Couldn't find 'product_id' for a 'selected_product' condition",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            Condition.SelectedProduct(productId, extractGroupId())
        }
        else -> Condition.Unknown
    }
}

private fun Map<*, *>.extractGroupId() =
    (this["group_id"] as? String) ?: DEFAULT_PRODUCT_GROUP
