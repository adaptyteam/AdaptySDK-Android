@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.EdgeEntities
import com.adapty.ui.internal.ui.attributes.Offset
import com.adapty.ui.internal.ui.attributes.horizontalSumOrDefault
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.attributes.toComposeShape
import com.adapty.ui.internal.ui.attributes.toExactDp
import com.adapty.ui.internal.ui.attributes.verticalSumOrDefault
import com.adapty.ui.internal.ui.element.ResolveAssets
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.utils.getForCurrentSystemTheme

@InternalAdaptyApi
@Composable
public fun Modifier.fillWithBaseParams(element: UIElement, resolveAssets: ResolveAssets): Modifier {
    return this
        .sizeAndMarginsOrSkip(element)
        .offsetOrSkip(element.baseProps.offset)
        .backgroundOrSkip(element.baseProps.shape, resolveAssets)
}

@Composable
internal fun Modifier.backgroundOrSkip(
    decorator: com.adapty.ui.internal.ui.attributes.Shape?,
    resolveAssets: ResolveAssets,
): Modifier {
    val decorator = decorator ?: return this
    var modifier = this
    val backgroundShape = decorator.type.toComposeShape()
    modifier = modifier.clipToShape(backgroundShape)
    if (decorator.fill != null) {
        val backgroundAsset = resolveAssets().getForCurrentSystemTheme(decorator.fill.assetId) as? Asset.Filling.Local
        if (backgroundAsset != null)
            modifier = modifier.background(backgroundAsset, backgroundShape)
    }

    if (decorator.border != null) {
        when (val borderAsset = resolveAssets().getForCurrentSystemTheme(decorator.border.color) as? Asset.Filling.Local) {
            is Asset.Color -> {
                modifier = modifier.border(
                    decorator.border.thickness.dp,
                    borderAsset.toComposeFill().color,
                    decorator.border.shapeType.toComposeShape(),
                )
            }
            is Asset.Gradient -> {
                modifier = modifier.border(
                    decorator.border.thickness.dp,
                    borderAsset.toComposeFill().shader,
                    decorator.border.shapeType.toComposeShape(),
                )
            }
            else -> Unit
        }
    }
    return modifier
}

private fun Modifier.clipToShape(
    shape: Shape,
) = composed {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val useCustomClip = remember(shape) {
        shape.createOutline(Size(100f, 100f), layoutDirection, density) is Outline.Generic
    }

    if (useCustomClip) {
        drawWithContent {
            val outline = shape.createOutline(size, layoutDirection, density)
            val path = (outline as Outline.Generic).path

            val canvas = drawContext.canvas
            canvas.save()
            canvas.clipPath(path)
            drawContent()
            canvas.restore()
        }
    } else {
        clip(shape)
    }
}

private fun Modifier.background(
    asset: Asset.Filling.Local,
    shape: Shape,
): Modifier =
    when (asset) {
        is Asset.Color -> {
            val fill = asset.toComposeFill()
            background(color = fill.color, shape = shape)
        }
        is Asset.Gradient -> {
            val fill = asset.toComposeFill()
            background(brush = fill.shader, shape = shape)
        }
        is Asset.Image -> drawBehind {
            val fill = asset.toComposeFill(size) ?: return@drawBehind
            drawIntoCanvas { canvas ->
                canvas.save()
                if (shape != RectangleShape) {
                    val path = Path()
                    shape.createOutline(size, layoutDirection, this).let { outline ->
                        when (outline) {
                            is Outline.Rectangle -> path.addRect(outline.rect)
                            is Outline.Rounded -> path.addRoundRect(outline.roundRect)
                            is Outline.Generic -> path.addPath(outline.path)
                        }
                    }
                    canvas.clipPath(path)
                }
                canvas.nativeCanvas.drawBitmap(fill.image, fill.matrix, fill.paint)
                canvas.restore()
            }
        }
    }

@Composable
internal fun Modifier.sizeAndMarginsOrSkip(element: UIElement): Modifier {
    val baseProps = element.baseProps
    val margins = baseProps.padding
    return this
        .sideDimensionOrSkip(baseProps.widthSpec, margins)
        .sideDimensionOrSkip(baseProps.heightSpec, margins)
        .marginsOrSkip(margins)
}

@Composable
internal fun Modifier.sideDimensionOrSkip(sideDimension: DimSpec?, margins: EdgeEntities?): Modifier {
    return when (sideDimension) {
        null -> this
        is DimSpec.FillMax -> when (sideDimension.axis) {
            DimSpec.Axis.X -> this.fillMaxWidth()
            DimSpec.Axis.Y -> this.fillMaxHeight()
        }
        is DimSpec.Min -> when (val axis = sideDimension.axis) {
            DimSpec.Axis.X -> this.widthIn(min = sideDimension.value.toExactDp(axis) + margins.horizontalSumOrDefault)
            DimSpec.Axis.Y -> this.heightIn(min = sideDimension.value.toExactDp(axis) + margins.verticalSumOrDefault)
        }
        is DimSpec.Specified -> when (val axis = sideDimension.axis) {
            DimSpec.Axis.X -> this.width(sideDimension.value.toExactDp(axis) + margins.horizontalSumOrDefault)
            DimSpec.Axis.Y -> this.height(sideDimension.value.toExactDp(axis) + margins.verticalSumOrDefault)
        }
        is DimSpec.Shrink -> when (val axis = sideDimension.axis) {
            DimSpec.Axis.X -> this
                .widthIn(min = (sideDimension.min.toExactDp(axis) + margins.horizontalSumOrDefault).takeIf { it > 0.dp }
                    ?: Dp.Unspecified)
                .width(IntrinsicSize.Min)
            DimSpec.Axis.Y -> this
                .heightIn(min = (sideDimension.min.toExactDp(axis) + margins.verticalSumOrDefault).takeIf { it > 0.dp }
                    ?: Dp.Unspecified)
                .height(IntrinsicSize.Min)
        }
    }
}

@Composable
internal fun Modifier.marginsOrSkip(margins: EdgeEntities?): Modifier {
    if (margins == null)
        return this
    val (start, top, end, bottom) = margins
    val paddingValues = listOf(start, top, end, bottom).mapIndexed { i, dimUnit ->
        dimUnit.toExactDp(if (i % 2 == 0) DimSpec.Axis.X else DimSpec.Axis.Y)
    }.let { values ->
        PaddingValues(values[0], values[1], values[2], values[3])
    }
    return this
        .padding(paddingValues)
}

internal fun Modifier.offsetOrSkip(offset: Offset?): Modifier {
    if (offset == null || offset.consumed)
        return this
    return this.offset(offset.x.dp, offset.y.dp)
}