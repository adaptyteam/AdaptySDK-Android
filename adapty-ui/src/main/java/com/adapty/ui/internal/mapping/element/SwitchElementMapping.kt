@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toTransition
import com.adapty.ui.internal.ui.element.SwitchElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toSwitchElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val deviceKind = DeviceKind.current()
    val runtimeCases = ArrayList<SwitchElement.Case>()
    var alwaysTrueContent: UIElement? = null

    for (rawCase in (this["cases"] as? List<*>).orEmpty()) {
        if (alwaysTrueContent != null) break
        val caseMap = rawCase as? Map<*, *> ?: continue
        val conditions = (caseMap["condition"] as? List<*>).parseConditionGroup(deviceKind)
            ?: continue
        val content = (caseMap["content"] as? Map<*, *>)?.let { childMapper(it, inheritShrink) }
            ?: throw adaptyError(
                message = "content in switch case must not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )
        if (conditions.isEmpty()) {
            alwaysTrueContent = content
        } else {
            runtimeCases.add(SwitchElement.Case(conditions, content))
        }
    }

    val default = alwaysTrueContent
        ?: (this["default"] as? Map<*, *>)?.let { childMapper(it, inheritShrink) }
        ?: throw adaptyError(
            message = "default in switch must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
        )

    val transition = if (runtimeCases.isNotEmpty() && this["duration"] != null) this.toTransition() else null

    return SwitchElement(
        cases = runtimeCases,
        default = default,
        transition = transition,
        baseProps = this.extractBaseProps(),
    )
}
