@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image.ScaleType
import com.adapty.ui.internal.ui.fitImageWithinBounds
import com.adapty.ui.internal.ui.attributes.AspectRatio
import com.adapty.ui.internal.ui.attributes.LocalContentAlignment
import com.adapty.ui.internal.ui.attributes.evaluateComposeImageAlignment
import com.adapty.ui.internal.ui.attributes.toComposeContentScale
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.getBitmap
import com.adapty.ui.internal.utils.getImageNaturalSize
import com.adapty.ui.internal.utils.resolve
import com.adapty.ui.internal.utils.resolveAsset
import com.adapty.ui.internal.utils.resolveColorFilter

@InternalAdaptyApi
public class ImageElement internal constructor(
    internal val assetId: VisualValue,
    internal val aspectRatio: AspectRatio,
    internal val tint: VisualValue?,
    override val baseProps: BaseProps,
) : UIElement, SizeDrivenElement {

    public constructor(
        assetId: String,
        aspectRatio: AspectRatio,
        baseProps: BaseProps,
    ): this(VisualValue.assetId(assetId), aspectRatio, null, baseProps)

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = composable@{
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val context = LocalContext.current
        val colorFilter: ColorFilter? = tint?.resolveColorFilter()
        val image = assetId.resolveAsset<Asset.Image>()
        if (image == null && assetId.resolveAsset<Asset.Filling>() != null) {
            val resolvedValue = assetId.source.resolve()
            LaunchedEffect(resolvedValue) {
                dispatch(Message.UIError(
                    "expected an image, but '$resolvedValue' is not an image asset",
                    AdaptyErrorCode.WRONG_ASSET_TYPE,
                ))
            }
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(if (isSystemInDarkTheme) Color.White else Color.Black),
            )
            return@composable
        }
        var naturalSize by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        val headerSize = remember(image?.main?.source?.key) {
            image?.let { getImageNaturalSize(context, it) }
        }
        if (headerSize != null && naturalSize == null) {
            naturalSize = headerSize
        }
        BoxWithConstraints(
            modifier = Modifier
                .then(if (aspectRatio == AspectRatio.FILL) Modifier else Modifier.clipToBounds())
                .then(ImageIntrinsicsModifier(
                    naturalWidth = naturalSize?.first ?: 0,
                    naturalHeight = naturalSize?.second ?: 0,
                ))
        ) {
            val imageBitmap = remember(constraints.maxWidth, constraints.maxHeight, image?.main?.source?.key, isSystemInDarkTheme) {
                image?.let {
                    getBitmap(context, image, constraints.maxWidth, constraints.maxHeight, if (aspectRatio == AspectRatio.FIT) ScaleType.FIT_MIN else ScaleType.FIT_MAX)
                        ?.asImageBitmap()
                }
            }

            if (imageBitmap == null) return@BoxWithConstraints

            SideEffect {
                if (naturalSize == null) {
                    naturalSize = imageBitmap.width to imageBitmap.height
                }
            }

            val bitmapRatio = imageBitmap.width.toFloat() / imageBitmap.height.toFloat()
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                alignment = aspectRatio.evaluateComposeImageAlignment(LocalContentAlignment.current),
                contentScale = aspectRatio.toComposeContentScale(),
                colorFilter = colorFilter,
                modifier = modifier
                    .then(
                        when {
                            aspectRatio == AspectRatio.FIT && bitmapRatio > 0f ->
                                Modifier.fitImageWithinBounds(bitmapRatio, constraints)
                            aspectRatio == AspectRatio.FILL && bitmapRatio > 0f -> {
                                val fillsWidth = !constraints.hasBoundedHeight ||
                                    constraints.maxWidth.toFloat() / bitmapRatio >= constraints.maxHeight
                                if (fillsWidth)
                                    Modifier
                                        .wrapContentHeight(Alignment.Top, unbounded = true)
                                        .fillMaxWidth()
                                        .aspectRatio(bitmapRatio)
                                else
                                    Modifier
                                        .wrapContentWidth(Alignment.CenterHorizontally, unbounded = true)
                                        .fillMaxHeight()
                                        .aspectRatio(bitmapRatio)
                            }
                            else ->
                                Modifier.fillMaxWidth().fillMaxHeight()
                        }
                    ),
            )
        }
    }

    private val Asset.Image.Source.key get() = when (this) {
        is Asset.Image.Source.AndroidAsset -> path
        is Asset.Image.Source.Base64Str -> imageBase64
        is Asset.Image.Source.Bitmap -> "$bitmap"
        is Asset.Image.Source.Uri -> "$uri"
    }
}

private data class ImageIntrinsicsModifier(
    val naturalWidth: Int,
    val naturalHeight: Int,
) : ModifierNodeElement<ImageIntrinsicsNode>() {
    override fun create() = ImageIntrinsicsNode(naturalWidth, naturalHeight)

    override fun update(node: ImageIntrinsicsNode) {
        if (node.naturalWidth == naturalWidth && node.naturalHeight == naturalHeight) return
        node.naturalWidth = naturalWidth
        node.naturalHeight = naturalHeight
        node.invalidateMeasurement()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "imageIntrinsics"
        properties["naturalWidth"] = naturalWidth
        properties["naturalHeight"] = naturalHeight
    }
}

private class ImageIntrinsicsNode(
    var naturalWidth: Int,
    var naturalHeight: Int,
) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        if (naturalWidth <= 0 || naturalHeight <= 0) return 0
        return (width.toLong() * naturalHeight / naturalWidth).toInt()
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int = minIntrinsicHeight(measurable, width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        if (naturalWidth <= 0 || naturalHeight <= 0) return 0
        return (height.toLong() * naturalWidth / naturalHeight).toInt()
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int = minIntrinsicWidth(measurable, height)
}
