package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset.Image.ScaleType
import com.adapty.ui.internal.ui.attributes.AspectRatio
import com.adapty.ui.internal.ui.attributes.LocalContentAlignment
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.evaluateComposeImageAlignment
import com.adapty.ui.internal.ui.attributes.toComposeContentScale
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.utils.EventCallback
import com.adapty.ui.internal.utils.getBitmap
import com.adapty.ui.internal.utils.getForCurrentSystemTheme

@InternalAdaptyApi
public class ImageElement internal constructor(
    internal val assetId: String,
    internal val aspectRatio: AspectRatio,
    internal val tint: Shape.Fill?,
    override val baseProps: BaseProps,
) : UIElement {

    public constructor(
        assetId: String,
        aspectRatio: AspectRatio,
        baseProps: BaseProps,
    ): this(assetId, aspectRatio, null, baseProps)

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val tint = tint?.assetId?.let { assetId -> resolveAssets().getForCurrentSystemTheme(assetId) }
        val colorFilter = remember(isSystemInDarkTheme) {
            (tint as? Asset.Color)?.toComposeFill()?.color?.let { color ->
                ColorFilter.tint(color)
            }
        }
        val image = resolveAssets().getForCurrentSystemTheme(assetId) as? Asset.Image
        BoxWithConstraints {
            val imageBitmap = remember(constraints.maxWidth, constraints.maxHeight, image?.source?.javaClass, isSystemInDarkTheme) {
                image?.let {
                    getBitmap(image, constraints.maxWidth, constraints.maxHeight, if (aspectRatio == AspectRatio.FIT) ScaleType.FIT_MIN else ScaleType.FIT_MAX)
                        ?.asImageBitmap()
                }
            }

            if (imageBitmap == null) return@BoxWithConstraints

            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                alignment = aspectRatio.evaluateComposeImageAlignment(LocalContentAlignment.current),
                contentScale = aspectRatio.toComposeContentScale(),
                colorFilter = colorFilter,
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            )
        }
    }
}