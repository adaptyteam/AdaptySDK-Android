@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.Container
import com.adapty.ui.internal.ui.element.MultiContainer
import com.adapty.ui.internal.ui.element.ReferenceElement
import com.adapty.ui.internal.ui.element.SingleContainer
import com.adapty.ui.internal.ui.element.UIElement

@InternalAdaptyApi
public abstract class BaseUIElementMapper(
    private val elementTypeStr: String,
    protected val commonAttributeMapper: CommonAttributeMapper,
): UIElementMapper {

    override fun canMap(config: Map<*, *>): Boolean = config["type"] == elementTypeStr

    protected fun Map<*, *>.extractBaseProps(): BaseProps {
        return BaseProps(
            this["width"]?.let { item -> commonAttributeMapper.mapDimSpec(item, DimSpec.Axis.X) },
            this["height"]?.let { item -> commonAttributeMapper.mapDimSpec(item, DimSpec.Axis.Y) },
            this["weight"]?.toFloatOrNull(),
            this["decorator"]?.let(commonAttributeMapper::mapShape),
            this["padding"]?.let(commonAttributeMapper::mapEdgeEntities),
            this["offset"]?.let(commonAttributeMapper::mapOffset),
            (this["visibility"] as? Boolean) ?: true,
            (this["transition_in"] as? List<*>)?.mapNotNull { item ->
                (item as? Map<*, *>)?.let(commonAttributeMapper::mapTransition)
            }?.takeIf { it.isNotEmpty() }
                ?: (this["transition_in"] as? Map<*, *>)?.let(commonAttributeMapper::mapTransition)?.let { listOf(it) }
        )
    }

    protected fun Any.toFloatOrNull(): Float? = (this as? Number)?.toFloat()

    protected fun Map<*, *>.extractSpacingOrNull(): Float? =
        this["spacing"]?.toFloatOrNull()?.takeIf { it > 0f }

    protected fun addToReferenceTargetsIfNeeded(
        rawElement: Map<*, *>,
        element: UIElement,
        refBundles: ReferenceBundles,
    ) {
        (rawElement["element_id"] as? String)?.let { elementId ->
            refBundles.targetElements[elementId] = element

            processThoseAwaitingReferences(elementId, element, refBundles.awaitingElements)
        }
    }

    private fun processThoseAwaitingReferences(
        elementId: String,
        actualElement: UIElement,
        referenceAwaitingMap: MutableMap<String, MutableList<Container<*>>>,
    ) {
        referenceAwaitingMap.remove(elementId)?.forEach { container ->
            when (container) {
                is SingleContainer -> {
                    container.content = actualElement
                }
                is MultiContainer -> {
                    container.content = container.content.map { item ->
                        if (item is ReferenceElement && item.id == elementId)
                            actualElement
                        else
                            item
                    }
                }
                else -> Unit
            }
        }
    }

    protected fun addToAwaitingReferencesIfNeeded(
        referenceIds: Iterable<String>,
        container: Container<*>,
        referenceAwaitingMap: MutableMap<String, MutableList<Container<*>>>,
    ) {
        referenceIds.forEach { id ->
            referenceAwaitingMap.getOrPut(id) { mutableListOf() }.add(container)
        }
    }

    protected fun checkAsset(assetId: String, assets: Assets) {
        if (assets[assetId] == null)
            throw adaptyError(
                message = "asset_id ($assetId) does not exist",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
    }
}