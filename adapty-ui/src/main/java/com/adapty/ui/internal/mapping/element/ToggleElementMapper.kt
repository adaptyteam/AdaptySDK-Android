@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.mapping.attributes.InteractiveAttributeMapper
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.internal.ui.element.Condition
import com.adapty.ui.internal.ui.element.ToggleElement
import com.adapty.ui.internal.ui.element.UIElement

internal class ToggleElementMapper(
    private val interactiveAttributeMapper: InteractiveAttributeMapper,
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIElementMapper("toggle", commonAttributeMapper), UIPlainElementMapper {
    override fun map(config: Map<*, *>, assets: Assets, refBundles: ReferenceBundles): UIElement {
        val onActions: List<Action>
            val offActions: List<Action>
            val onCondition: Condition
            val sectionId = config["section_id"] as? String
            if (sectionId != null) {
                if (sectionId.isEmpty())
                    throw adaptyError(
                        message = "section_id in Toggle must not be empty",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                val onIndex = (config["on_index"] as? Number)?.toInt() ?: 0
                val offIndex = (config["off_index"] as? Number)?.toInt() ?: -1
                onCondition = Condition.SelectedSection(sectionId, onIndex)
                onActions = listOf(Action.SwitchSection(sectionId, onIndex))
                offActions = listOf(Action.SwitchSection(sectionId, offIndex))
            } else {
                onCondition = (config["on_condition"] as? Map<*, *>)?.let(interactiveAttributeMapper::mapCondition)
                    ?: throw adaptyError(
                        message = "on_condition in Toggle must not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                onActions = config["on_action"].asActions()
                offActions = config["off_action"].asActions()
            }

            return ToggleElement(
                onActions,
                offActions,
                onCondition,
                (config["color"] as? String)?.let { assetId -> Shape.Fill(assetId) },
                config.extractBaseProps(),
            )
                .also { element ->
                    addToReferenceTargetsIfNeeded(config, element, refBundles)
                }
    }

    private fun Any?.asActions() =
        (this as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.let(interactiveAttributeMapper::mapAction) }
            ?: (this as? Map<*, *>)?.let(interactiveAttributeMapper::mapAction)?.let { action -> listOf(action) }
                .orEmpty()
}