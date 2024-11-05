@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.element.RowElement
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement

internal class RowElementMapper(
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIComplexElementMapper("row", commonAttributeMapper), UIComplexElementMapper {
    override fun map(
        config: Map<*, *>,
        assets: Assets,
        refBundles: ReferenceBundles,
        stateMap: MutableMap<String, Any>,
        inheritShrink: Int,
        childMapper: ChildMapper,
    ): UIElement {
        val referenceIds = mutableSetOf<String>()
        return RowElement(
            content = (config["items"] as? List<*>)?.mapNotNull { item ->
                processContentItem((item as? Map<*, *>)?.asGridItem(DimSpec.Axis.X, refBundles, childMapper), referenceIds, refBundles.targetElements)
            }
                ?.takeIf { it.isNotEmpty() }
                ?: return SkippedElement,
            spacing = config.extractSpacingOrNull(),
            baseProps = config.extractBaseProps(),
        )
            .also { container ->
                addToAwaitingReferencesIfNeeded(referenceIds, container, refBundles.awaitingElements)
                addToReferenceTargetsIfNeeded(config, container, refBundles)
            }
    }
}