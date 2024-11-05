@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.mapping.attributes.InteractiveAttributeMapper
import com.adapty.ui.internal.ui.element.ButtonElement
import com.adapty.ui.internal.ui.element.UIElement

internal class ButtonElementMapper(
    private val interactiveAttributeMapper: InteractiveAttributeMapper,
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIComplexElementMapper("button", commonAttributeMapper), UIComplexElementMapper {
    override fun map(
        config: Map<*, *>,
        assets: Assets,
        refBundles: ReferenceBundles,
        stateMap: MutableMap<String, Any>,
        inheritShrink: Int,
        childMapper: ChildMapper
    ): UIElement {
        val referenceIds = mutableSetOf<String>()
        return ButtonElement(
            (config["action"] as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.let(interactiveAttributeMapper::mapAction) }
                ?: (config["action"] as? Map<*, *>)?.let(interactiveAttributeMapper::mapAction)?.let { action -> listOf(action) }.orEmpty(),
            processContentItem(
                (config["normal"] as? Map<*, *>)?.let { content -> childMapper(content) },
                referenceIds,
                refBundles.targetElements,
            )
                ?: throw adaptyError(
                    message = "normal state in Button must not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                ),
            processContentItem(
                (config["selected"] as? Map<*, *>)?.let { content -> childMapper(content) },
                referenceIds,
                refBundles.targetElements,
            ),
            (config["selected_condition"] as? Map<*, *>)?.let(interactiveAttributeMapper::mapCondition),
            config.extractBaseProps(),
        )
            .also { element ->
                addToReferenceTargetsIfNeeded(config, element, refBundles)
            }
    }
}