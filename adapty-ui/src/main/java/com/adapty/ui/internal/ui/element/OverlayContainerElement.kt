@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.LocalBlocksTouchesBelow
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.LocalOverflowAnchorHorizontal
import com.adapty.ui.internal.ui.attributes.LocalOverflowAnchorVertical
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.attributes.toComposeHorizontalAlignment
import com.adapty.ui.internal.ui.attributes.toComposeVerticalAlignment
import com.adapty.ui.internal.ui.overlayMainDecoratorOrSkip
import com.adapty.ui.internal.ui.overflowExtent
import com.adapty.ui.internal.ui.rememberOverflowExtent
import com.adapty.ui.internal.ui.tracksOverflow
import com.adapty.ui.internal.ui.blockTouchesBelowIfPainted
import com.adapty.ui.internal.ui.blurOrSkip
import com.adapty.ui.internal.ui.elementBlendOrSkip
import com.adapty.ui.internal.ui.resolveMainDecorator
import com.adapty.ui.internal.ui.rememberBlurProvider
import com.adapty.ui.internal.ui.rememberBoxProvider
import com.adapty.ui.internal.ui.rememberRotationProvider
import com.adapty.ui.internal.ui.rememberScaleProvider
import com.adapty.ui.internal.ui.sideDimensionOrSkip

@InternalAdaptyApi
public class OverlayContainerElement internal constructor(
    internal val main: UIElement,
    internal val overlays: List<OverlayItem> = emptyList(),
    internal val backgrounds: List<OverlayItem> = emptyList(),
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val backgroundProps = backgrounds.map { it.content.layoutRelevantPropsResolved() }
        val overlayProps = overlays.map { it.content.layoutRelevantPropsResolved() }
        Layout(
            modifier = modifier,
            content = {
                backgrounds.forEachIndexed { i, bg ->
                    Box {
                        CompositionLocalProvider(LocalBlocksTouchesBelow provides true) {
                            provideOverflowAnchors(bg, backgroundProps[i]) { bg.content.render(dispatch) }
                        }
                    }
                }
                val mainDecorator = main.baseProps.resolveMainDecorator()
                val mainRotation = rememberRotationProvider(main.baseProps).rotation.value
                val mainScale = rememberScaleProvider(main.baseProps).scale.value
                val mainBoxProvider = rememberBoxProvider(main.baseProps)
                val mainBlurProvider = rememberBlurProvider(main.baseProps)
                val mainOverflow = rememberOverflowExtent(mainDecorator)
                main.toComposable(
                    dispatch,
                    Modifier
                        .sideDimensionOrSkip(main.baseProps.widthSpec, null, mainBoxProvider)
                        .sideDimensionOrSkip(main.baseProps.heightSpec, null, mainBoxProvider)
                        .elementBlendOrSkip(main.baseProps.blend, mainDecorator, mainBlurProvider.radius.value.dp, mainOverflow)
                        .blurOrSkip(mainBlurProvider, mainDecorator?.shadowCaptureExpand ?: 0.dp)
                        .overlayMainDecoratorOrSkip(mainDecorator, mainRotation, mainScale, mainOverflow)
                        .overflowExtent(
                            mainOverflow,
                            tracks = main.baseProps.tracksOverflow(mainDecorator, alpha = 1f),
                            reach = (mainDecorator?.shadowCaptureExpand ?: 0.dp) + mainBlurProvider.radius.value.dp,
                        )
                        .blockTouchesBelowIfPainted(main),
                ).invoke()
                overlays.forEachIndexed { i, overlay ->
                    Box {
                        CompositionLocalProvider(LocalBlocksTouchesBelow provides true) {
                            provideOverflowAnchors(overlay, overlayProps[i]) { overlay.content.render(dispatch) }
                        }
                    }
                }
            },
            measurePolicy = object : MeasurePolicy {
                private val mainIndex get() = backgrounds.size

                private fun hasMain(count: Int) = count > backgrounds.size + overlays.size

                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints,
                ): MeasureResult {
                    if (!hasMain(measurables.size)) return layout(0, 0) {}
                    val direction = layoutDirection
                    val overlayStart = measurables.size - overlays.size
                    val mainPlaceable = measurables[mainIndex].measure(constraints)
                    val width = mainPlaceable.width
                    val height = mainPlaceable.height

                    fun alignedConstraints(props: BaseProps): Constraints {
                        val widthSpec = props.widthSpec
                        val heightSpec = props.heightSpec
                        val widthIsFlexible = widthSpec is DimSpec.FillMax || widthSpec == null
                        return Constraints(
                            minWidth = if (widthSpec is DimSpec.FillMax) width else 0,
                            maxWidth = if (widthIsFlexible) width else Constraints.Infinity,
                            minHeight = if (heightSpec is DimSpec.FillMax) height else 0,
                            maxHeight = if (heightSpec is DimSpec.FillMax) height else Constraints.Infinity,
                        )
                    }

                    fun OverlayItem.placeAndOffset(
                        measurable: Measurable,
                        props: BaseProps,
                    ): Pair<Placeable, IntOffset> {
                        val placeable = measurable.measure(alignedConstraints(props))
                        val offset = align.toComposeAlignment().align(
                            IntSize(placeable.width, placeable.height),
                            IntSize(width, height),
                            direction,
                        )
                        return placeable to offset
                    }

                    val backgroundPlacements = backgrounds.mapIndexed { i, bg ->
                        bg.placeAndOffset(measurables[i], backgroundProps[i])
                    }
                    val overlayPlacements = overlays.mapIndexed { i, overlay ->
                        overlay.placeAndOffset(measurables[overlayStart + i], overlayProps[i])
                    }

                    return layout(width, height) {
                        backgroundPlacements.forEach { (placeable, offset) -> placeable.place(offset) }
                        mainPlaceable.place(0, 0)
                        overlayPlacements.forEach { (placeable, offset) -> placeable.place(offset) }
                    }
                }

                private fun List<IntrinsicMeasurable>.mainOrNull() =
                    if (hasMain(size)) this[mainIndex] else null

                override fun IntrinsicMeasureScope.minIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int,
                ): Int = measurables.mainOrNull()?.minIntrinsicWidth(height) ?: 0

                override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int,
                ): Int = measurables.mainOrNull()?.maxIntrinsicWidth(height) ?: 0

                override fun IntrinsicMeasureScope.minIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int,
                ): Int = measurables.mainOrNull()?.minIntrinsicHeight(width) ?: 0

                override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int,
                ): Int = measurables.mainOrNull()?.maxIntrinsicHeight(width) ?: 0
            },
        )
    }

    @Composable
    private fun provideOverflowAnchors(item: OverlayItem, props: BaseProps, body: @Composable () -> Unit) {
        val provided = buildList {
            if (props.widthSpec is DimSpec.FillMax || props.widthSpec == null)
                add(LocalOverflowAnchorHorizontal provides item.align.toComposeHorizontalAlignment())
            if (props.heightSpec is DimSpec.FillMax)
                add(LocalOverflowAnchorVertical provides item.align.toComposeVerticalAlignment())
        }
        if (provided.isEmpty()) body() else CompositionLocalProvider(*provided.toTypedArray(), content = body)
    }
}
