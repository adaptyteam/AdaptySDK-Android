@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.element.SectionElement
import com.adapty.ui.internal.ui.element.UIElement

internal class SectionElementMapper(
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIComplexElementMapper("section", commonAttributeMapper), UIComplexElementMapper {

    override fun map(
        config: Map<*, *>,
        assets: Assets,
        refBundles: ReferenceBundles,
        stateMap: MutableMap<String, Any>,
        inheritShrink: Int,
        childMapper: ChildMapper,
    ): UIElement {
        val referenceIds = mutableSetOf<String>()
        return SectionElement(
            (config["id"] as? String)?.takeIf { it.isNotEmpty() }
                ?: throw adaptyError(
                    message = "id in Section must not be empty",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                ),
            (config["index"] as? Number)?.toInt() ?: 0,
            (config["content"] as? List<*>)?.mapNotNull { item ->
                processContentItem(
                    (item as? Map<*, *>)?.let { content -> childMapper(content) },
                    referenceIds,
                    refBundles.targetElements,
                )
            } ?: throw adaptyError(
                message = "content in Section must not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
        ).also { section ->
            stateMap[section.key] = section.index
            addToAwaitingReferencesIfNeeded(referenceIds, section, refBundles.awaitingElements)
            addToReferenceTargetsIfNeeded(config, section, refBundles)
        }
    }
}