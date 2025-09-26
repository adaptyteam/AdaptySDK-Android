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
import androidx.compose.ui.platform.LocalContext
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
import com.adapty.ui.internal.utils.getAsset

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
        val context = LocalContext.current
        val tint = tint?.assetId?.let { assetId -> resolveAssets().getAsset<Asset.Color>(assetId) }
        val colorFilter = remember(isSystemInDarkTheme) {
            tint?.toComposeFill()?.color?.let { color ->
                ColorFilter.tint(color)
            }
        }
        val image = resolveAssets().getAsset<Asset.Image>(assetId)
        BoxWithConstraints {
            val imageBitmap = remember(constraints.maxWidth, constraints.maxHeight, image?.main?.source?.key, isSystemInDarkTheme) {
                image?.let {
                    getBitmap(context, image, constraints.maxWidth, constraints.maxHeight, if (aspectRatio == AspectRatio.FIT) ScaleType.FIT_MIN else ScaleType.FIT_MAX)
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

    private val Asset.Image.Source.key get() = when (this) {
        is Asset.Image.Source.AndroidAsset -> path
        is Asset.Image.Source.Base64Str -> imageBase64
        is Asset.Image.Source.Bitmap -> "$bitmap"
        is Asset.Image.Source.Uri -> "$uri"
    }
}