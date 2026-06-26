@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.text

import android.content.Context
import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.FlowConfiguration.RichText
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.ui.element.BaseTextElement
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.parseColorInt
import com.adapty.ui.internal.utils.resolve

internal class ComposeTextAttrs(
    val textColor: Color?,
    val backgroundColor: Color?,
    val fontSize: Float?,
    val textDecoration: TextDecoration?,
    val fontFamily: FontFamily?,
    val typeface: android.graphics.Typeface?,
    val letterSpacing: Float?,
    val lineHeight: Float?,
) {
    companion object {
        @Composable
        fun from(
            richTextAttrs: RichText.Attributes,
            textElementAttrs: BaseTextElement.Attributes?,
            assets: Assets,
        ): ComposeTextAttrs {
            return from(
                textColor = richTextAttrs.textColor
                    ?: textElementAttrs?.textColor,
                backgroundColor = richTextAttrs.background,
                fontAssetId = richTextAttrs.fontAssetId,
                fontSize = richTextAttrs.size
                    ?: Float.NaN,
                underline = richTextAttrs.underline,
                strikethrough = richTextAttrs.strikethrough,
                letterSpacing = richTextAttrs.letterSpacing
                    ?: textElementAttrs?.letterSpacing,
                lineHeight = textElementAttrs?.lineHeight,
                assets = assets,
            )
        }

        @Composable
        fun from(elementAttrs: BaseTextElement.Attributes, assets: Assets): ComposeTextAttrs {
            return from(
                textColor = elementAttrs.textColor,
                backgroundColor = elementAttrs.background,
                fontAssetId = elementAttrs.fontId,
                fontSize = elementAttrs.fontSize,
                underline = elementAttrs.underline,
                strikethrough = elementAttrs.strikethrough,
                letterSpacing = elementAttrs.letterSpacing,
                lineHeight = elementAttrs.lineHeight,
                assets = assets,
            )
        }

        @Composable
        private fun from(
            textColor: VisualValue?,
            backgroundColor: VisualValue?,
            fontAssetId: VisualValue?,
            fontSize: Float?,
            underline: Boolean,
            strikethrough: Boolean,
            letterSpacing: Float? = null,
            lineHeight: Float? = null,
            assets: Assets,
        ): ComposeTextAttrs {
            val context = LocalContext.current
            val fontAsset = resolveFontAsset(fontAssetId, assets)
            val typeface = resolveTypeface(fontAsset, context)
            return ComposeTextAttrs(
                resolveColor(textColor, assets) ?: resolveColor(fontAsset?.color),
                resolveColor(backgroundColor, assets),
                (fontSize ?: fontAsset?.size)?.takeIf { !it.isNaN() },
                resolveTextDecoration(underline, strikethrough),
                typeface?.let { FontFamily(it) },
                typeface,
                letterSpacing ?: fontAsset?.letterSpacing,
                lineHeight ?: fontAsset?.lineHeight,
            )
        }
        @Composable
        private fun resolveColor(visualValue: VisualValue?, assets: Assets): Color? {
            if (visualValue == null) return null
            return visualValue.source.resolve()
                ?.let { value ->
                    visualValue.orderedTypes
                        .firstOrNull { it.condition(value) }
                        ?.let { type ->
                            when (type) {
                                VisualValue.Type.ColorLiteral -> remember(value) {
                                    Color(value.parseColorInt())
                                }
                                VisualValue.Type.AssetId -> {
                                    assets.getAsset<AdaptyUI.FlowConfiguration.Asset.Color>(value)
                                        ?.let { asset -> resolveColor(asset.main.value) }
                                }
                            }
                        }
                }
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

        @Composable
        private fun resolveFontAsset(fontAssetId: VisualValue?, assets: Assets): AdaptyUI.FlowConfiguration.Asset.Font? {
            if (fontAssetId == null) return null
            return fontAssetId.source.resolve()
                ?.let { assetId -> assets[assetId] as? AdaptyUI.FlowConfiguration.Asset.Font }
        }

        private fun resolveTypeface(font: AdaptyUI.FlowConfiguration.Asset.Font?, context: Context): android.graphics.Typeface? {
            return font?.let { TypefaceHolder.getOrPut(context, font) }
        }
    }
}