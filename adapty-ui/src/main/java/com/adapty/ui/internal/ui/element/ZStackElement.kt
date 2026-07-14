@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntSize
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.Align
import com.adapty.ui.internal.ui.attributes.AspectRatio
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.LocalParentImposesHeight
import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
import com.adapty.ui.internal.ui.attributes.PageSize
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.store.Message

@InternalAdaptyApi
public class ZStackElement internal constructor(
    override var content: List<UIElement>,
    internal val align: Align,
    override val baseProps: BaseProps,
) : UIElement, MultiContainer {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val alignment = align.toComposeAlignment()
        val hasGreedyChild = content.any { it.isHeightGreedy() }
        Layout(
            content = {
                CompositionLocalProvider(LocalParentImposesHeight provides true) {
                    content.forEach { item ->
                        item.render(dispatch)
                    }
                }
            },
            modifier = modifier,
        ) { measurables, constraints ->
            val heightBound = if (constraints.hasFixedHeight || (hasGreedyChild && constraints.hasBoundedHeight)) {
                constraints.maxHeight
            } else {
                measurables
                    .maxOfOrNull {
                        try {
                            it.minIntrinsicHeight(constraints.maxWidth)
                        } catch (e: IllegalStateException) {
                            0
                        }
                    }
                    ?.takeIf { it > 0 }
                    ?.coerceIn(constraints.minHeight, constraints.maxHeight)
                    ?: constraints.maxHeight
            }
            val childConstraints = constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxHeight = heightBound,
            )
            val placeables = measurables.map { it.measure(childConstraints) }
            val width = (placeables.maxOfOrNull { it.width } ?: 0)
                .coerceIn(constraints.minWidth, constraints.maxWidth)
            val height = (placeables.maxOfOrNull { it.height } ?: 0)
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                placeables.forEach { placeable ->
                    val position = alignment.align(
                        IntSize(placeable.width, placeable.height),
                        IntSize(width, height),
                        layoutDirection,
                    )
                    placeable.place(position)
                }
            }
        }
    }
}

@OptIn(InternalAdaptyApi::class)
private fun UIElement.isHeightGreedy(): Boolean {
    when (val spec = baseProps.heightSpec) {
        is DimSpec.Specified, is DimSpec.Shrink -> return false
        is DimSpec.FillMax -> return true
        is DimSpec.Min -> return spec.maxValue == null
        null -> Unit
    }
    return when (this) {
        is PagerElement -> pageHeight is PageSize.PageFraction
        is ColumnElement -> (height ?: MainAxisBehaviour.FILL) == MainAxisBehaviour.FILL
        is RowElement -> false
        is ImageElement -> aspectRatio != AspectRatio.FIT
        is SingleContainer -> content.isHeightGreedy()
        is MultiContainer -> content.any { it.isHeightGreedy() }
        else -> false
    }
}
