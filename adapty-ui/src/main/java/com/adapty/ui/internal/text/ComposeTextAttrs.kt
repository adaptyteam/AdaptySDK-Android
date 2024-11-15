@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.text

import android.content.Context
import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.RichText
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.ui.element.BaseTextElement
import com.adapty.ui.internal.utils.getForCurrentSystemTheme

internal class ComposeTextAttrs(
    val textColor: Color?,
    val backgroundColor: Color?,
    val fontSize: Float?,
    val textDecoration: TextDecoration?,
    val fontFamily: FontFamily?,
) {
    companion object {
        @Composable
        fun from(attrs: RichText.Attributes, assets: Assets): ComposeTextAttrs {
            return from(
                textColorAssetId = attrs.textColorAssetId,
                backgroundColorAssetId = attrs.backgroundAssetId,
                fontAssetId = attrs.fontAssetId,
                fontSize = attrs.size,
                underline = attrs.underline,
                strikethrough = attrs.strikethrough,
                assets = assets,
            )
        }

        @Composable
        fun from(elementAttrs: BaseTextElement.Attributes, assets: Assets): ComposeTextAttrs {
            return from(
                textColorAssetId = elementAttrs.textColor?.assetId,
                backgroundColorAssetId = elementAttrs.background?.assetId,
                fontAssetId = elementAttrs.fontId,
                fontSize = elementAttrs.fontSize,
                underline = elementAttrs.underline,
                strikethrough = elementAttrs.strikethrough,
                assets = assets,
            )
        }

        @Composable
        private fun from(
            textColorAssetId: String?,
            backgroundColorAssetId: String?,
            fontAssetId: String?,
            fontSize: Float?,
            underline: Boolean,
            strikethrough: Boolean,
            assets: Assets,
        ): ComposeTextAttrs {
            val context = LocalContext.current
            val fontAsset = resolveFontAsset(fontAssetId, assets)
            return ComposeTextAttrs(
                resolveColorAsset(textColorAssetId, assets) ?: resolveColor(fontAsset?.color),
                resolveColorAsset(backgroundColorAssetId, assets),
                fontSize ?: fontAsset?.size,
                resolveTextDecoration(underline, strikethrough),
                resolveFontFamily(fontAsset, context),
            )
        }

        @Composable
        private fun resolveColorAsset(assetId: String?, assets: Assets): Color? {
            return assetId
                ?.let { assets.getForCurrentSystemTheme(assetId) as? AdaptyUI.LocalizedViewConfiguration.Asset.Color }
                ?.let { asset -> Color(asset.value) }
        }

        private fun resolveColor(@ColorInt color: Int?): Color? {
            return color?.let { Color(color) }
        }

        private fun resolveTextDecoration(underline: Boolean, strikethrough: Boolean): TextDecoration? {
            val textDecorations = listOfNotNull(
                underline.takeIf { it }?.let { TextDecoration.Underline },
                strikethrough.takeIf { it }?.let { TextDecoration.LineThrough },
            )
            return when(textDecorations.size) {
                0 -> null
                1 -> textDecorations.first()
                else -> TextDecoration.combine(textDecorations)
            }
        }

        private fun resolveFontAsset(assetId: String?, assets: Assets): AdaptyUI.LocalizedViewConfiguration.Asset.Font? {
            return assetId
                ?.let { assets[assetId] as? AdaptyUI.LocalizedViewConfiguration.Asset.Font }
        }

        private fun resolveFontFamily(font: AdaptyUI.LocalizedViewConfiguration.Asset.Font?, context: Context): FontFamily? {
            return font?.let { FontFamily(TypefaceHolder.getOrPut(context, font)) }
        }
    }
}