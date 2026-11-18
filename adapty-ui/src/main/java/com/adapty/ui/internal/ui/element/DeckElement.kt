@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.DeckSolver
import com.adapty.ui.internal.ui.DeclaringLength
import com.adapty.ui.internal.ui.UnspecifiedAxes
import com.adapty.ui.internal.ui.attributes.DeckItemLength
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.LocalParentImposesHeight
import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
import com.adapty.ui.internal.ui.attributes.Point
import com.adapty.ui.internal.ui.attributes.toExactDp
import kotlin.math.roundToInt

@InternalAdaptyApi
public class DeckElement internal constructor(
    internal val width: MainAxisBehaviour,
    internal val height: MainAxisBehaviour,
    internal val items: List<DeckItem>,
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val density = LocalDensity.current
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val solverItems = items.map { item ->
            DeckSolver.Item(
                width = item.width.toSolverLength(DimSpec.Axis.X, density),
                height = item.height.toSolverLength(DimSpec.Axis.Y, density),
                placement = DeckSolver.placement(item, isRtl),
            )
        }
        val fallbackPx = with(density) { DeckSolver.FALLBACK_LENGTH_DP.dp.toPx() }
        val measurePolicy = remember(width, height, solverItems, fallbackPx) {
            DeckMeasurePolicy(width, height, solverItems, fallbackPx)
        }
        Layout(
            content = {
                CompositionLocalProvider(LocalParentImposesHeight provides true) {
                    items.forEach { item ->
                        Box(contentAlignment = Alignment.Center) {
                            DeclaringLength(
                                unspecifiedHorizontal = item.width.unspecified(),
                                unspecifiedVertical = item.height.unspecified(),
                            ) {
                                item.content.render(dispatch)
                            }
                        }
                    }
                }
            },
            modifier = modifier,
            measurePolicy = measurePolicy,
        )
    }
}

internal class DeckItem(
    val width: DeckItemLength,
    val height: DeckItemLength,
    val anchor: Point,
    val position: Point,
    val mirrorInRtl: Boolean,
    val content: UIElement,
)

private fun DeckItemLength.unspecified(): Boolean? =
    when (this) {
        DeckItemLength.Content -> true
        is DeckItemLength.Fixed -> false
        is DeckItemLength.Parent -> null
    }

@Composable
private fun DeckItemLength.toSolverLength(axis: DimSpec.Axis, density: Density): DeckSolver.Length =
    when (this) {
        DeckItemLength.Content -> DeckSolver.Length.Content
        is DeckItemLength.Parent -> DeckSolver.Length.Parent(fraction)
        is DeckItemLength.Fixed -> DeckSolver.Length.Px(with(density) { value.toExactDp(axis).toPx() })
    }

private class DeckMeasurePolicy(
    private val width: MainAxisBehaviour,
    private val height: MainAxisBehaviour,
    private val items: List<DeckSolver.Item>,
    private val fallbackPx: Float,
) : MeasurePolicy {

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val count = minOf(items.size, measurables.size)
        val placeables = arrayOfNulls<Placeable>(measurables.size)

        val result = DeckSolver.solve(
            width = width,
            height = height,
            items = items.subList(0, count),
            availableWidth = constraints.maxWidth.boundedOrNull(),
            availableHeight = constraints.maxHeight.boundedOrNull(),
            fallbackLength = fallbackPx,
        ) { index, maxWidth, maxHeight ->
            val measurable = measurables[index]
            if (maxWidth == null) {
                val naturalWidth = measurable.safeMaxIntrinsicWidth(Constraints.Infinity)
                if (items[index].height != DeckSolver.Length.Content) {
                    return@solve DeckSolver.Extent(naturalWidth.toFloat(), 0f)
                }
                val naturalHeight = measurable.safeMaxIntrinsicHeight(naturalWidth)
                val placeable = measurable.measure(fit(0, naturalWidth, 0, naturalHeight))
                placeables[index] = placeable
                DeckSolver.Extent(placeable.width.toFloat(), placeable.height.toFloat())
            } else {
                val itemWidth = maxWidth.toPx()
                val naturalHeight = maxHeight?.toPx() ?: measurable.safeMaxIntrinsicHeight(itemWidth)
                val placeable = measurable.measure(fit(itemWidth, itemWidth, 0, naturalHeight))
                placeables[index] = placeable
                DeckSolver.Extent(placeable.width.toFloat(), placeable.height.toFloat())
            }
        }

        for (index in 0 until count) {
            if (placeables[index] == null) {
                val frame = result.frames[index]
                val itemWidth = frame.width.toPx()
                val itemHeight = frame.height.toPx()
                placeables[index] = measurables[index].measure(fit(itemWidth, itemWidth, itemHeight, itemHeight))
            }
        }
        for (index in count until measurables.size) {
            placeables[index] = measurables[index].measure(Constraints.fixed(0, 0))
        }

        return layout(result.width.toPx(), result.height.toPx()) {
            placeables.forEachIndexed { index, placeable ->
                val frame = result.frames.getOrNull(index)
                placeable?.place(frame?.x?.roundToInt() ?: 0, frame?.y?.roundToInt() ?: 0)
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = intrinsicSolve(measurables, availableWidth = null, availableHeight = height.boundedOrNull()).width.toPx()

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = intrinsicSolve(measurables, availableWidth = null, availableHeight = height.boundedOrNull()).width.toPx()

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = intrinsicSolve(measurables, availableWidth = width.boundedOrNull(), availableHeight = null).height.toPx()

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = intrinsicSolve(measurables, availableWidth = width.boundedOrNull(), availableHeight = null).height.toPx()

    private fun intrinsicSolve(
        measurables: List<IntrinsicMeasurable>,
        availableWidth: Float?,
        availableHeight: Float?,
    ): DeckSolver.Result = DeckSolver.solve(
        width = width,
        height = height,
        items = items.subList(0, minOf(items.size, measurables.size)),
        availableWidth = availableWidth,
        availableHeight = availableHeight,
        fallbackLength = fallbackPx,
    ) { index, maxWidth, maxHeight ->
        val measurable = measurables[index]
        val itemWidth = maxWidth?.toPx() ?: measurable.safeMaxIntrinsicWidth(Constraints.Infinity)
        val itemHeight = when {
            maxHeight != null -> maxHeight.toPx()
            maxWidth == null && items[index].height != DeckSolver.Length.Content -> 0
            else -> measurable.safeMaxIntrinsicHeight(itemWidth)
        }
        DeckSolver.Extent(itemWidth.toFloat(), itemHeight.toFloat())
    }
}

private const val MAX_LAYOUT_PX = (1 shl 24) - 1

private fun Int.boundedOrNull(): Float? = if (this == Constraints.Infinity) null else toFloat()

private fun Float.toPx(): Int = roundToInt().coerceIn(0, MAX_LAYOUT_PX)

private fun fit(minWidth: Int, maxWidth: Int, minHeight: Int, maxHeight: Int): Constraints =
    Constraints.fitPrioritizingWidth(
        minWidth = minWidth,
        maxWidth = maxWidth,
        minHeight = minHeight,
        maxHeight = maxHeight,
    )

private fun IntrinsicMeasurable.safeMaxIntrinsicWidth(height: Int): Int =
    try {
        maxIntrinsicWidth(height).coerceIn(0, MAX_LAYOUT_PX)
    } catch (e: IllegalStateException) {
        0
    }

private fun IntrinsicMeasurable.safeMaxIntrinsicHeight(width: Int): Int =
    try {
        maxIntrinsicHeight(width).coerceIn(0, MAX_LAYOUT_PX)
    } catch (e: IllegalStateException) {
        0
    }
