@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.text

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Rect
import android.text.TextPaint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyProductDiscountPhase.PaymentMode
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUI.FlowConfiguration.RichText
import com.adapty.ui.AdaptyUI.FlowConfiguration.TextItem
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.mapping.element.Products
import com.adapty.ui.internal.mapping.element.Texts
import com.adapty.ui.internal.ui.element.BaseTextElement
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.utils.firstDiscountOfferOrNull
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.getBitmap
import com.adapty.ui.internal.utils.resolve
import com.adapty.ui.internal.utils.resolveColorFilter
import com.adapty.ui.listeners.AdaptyUiTagResolver
import java.util.Locale

internal class TextResolver(
    private val tagResolver: TagResolver,
) {

    @Composable
    fun resolve(
        stringId: StringId,
        textElementAttrs: Attributes?,
        texts: Texts,
        products: Products,
        assets: Assets,
        locale: Locale,
    ): StringWrapper? {
        when (stringId) {
            is StringId.Str -> return texts[stringId.source.resolve()]?.toComposeString(textElementAttrs, assets, products, locale, tagValues = stringId.tagValues)
                ?: StringWrapper.EMPTY
            is StringId.Product -> {
                val desiredProductId = stringId.productIdSource.resolve()
                val desiredLocalText = if (!desiredProductId.isNullOrEmpty()) {
                    val product = products[desiredProductId]
                    val paymentModeStr = when(product?.firstDiscountOfferOrNull()?.paymentMode) {
                        PaymentMode.FREE_TRIAL -> "free_trial"
                        PaymentMode.PAY_AS_YOU_GO -> "pay_as_you_go"
                        PaymentMode.PAY_UPFRONT -> "pay_up_front"
                        else -> "default"
                    }
                    texts[listOfNotNull("PRODUCT", desiredProductId, paymentModeStr, stringId.suffix).joinToString(separator = "_")]
                        ?: texts[listOfNotNull("PRODUCT", desiredProductId, "default", stringId.suffix).joinToString(separator = "_")]
                        ?: texts[listOfNotNull("PRODUCT", paymentModeStr, stringId.suffix).joinToString(separator = "_")]
                        ?: texts[listOfNotNull("PRODUCT", "default", stringId.suffix).joinToString(separator = "_")]
                } else {
                    texts[listOfNotNull("PRODUCT_not_selected", stringId.suffix).joinToString(separator = "_")]
                }
                return desiredLocalText?.toComposeString(textElementAttrs, assets, products, locale, desiredProductId)
                    ?: StringWrapper.EMPTY
            }
        }
    }

    fun setCustomTagResolver(customTagResolver: AdaptyUiTagResolver) {
        tagResolver.customTagResolver = customTagResolver
    }

    @Composable
    private fun TextItem.toComposeString(textElementAttrs: Attributes?, assets: Assets, products: Products, locale: Locale, productId: String? = null, tagValues: Map<String, TagValueSource>? = null): StringWrapper? {
        return value.toComposeString(textElementAttrs, assets, products, locale, fallback == null, productId, tagValues).let { actualStr ->
            if (actualStr === StringWrapper.CUSTOM_TAG_NOT_FOUND)
                fallback?.toComposeString(textElementAttrs, assets, products, locale, true, productId, tagValues)
            else
                actualStr
        }
    }

    @Composable
    private fun RichText.toComposeString(
        textElementAttrs: Attributes?,
        assets: Assets,
        products: Products,
        locale: Locale,
        ignoreMissingCustomTag: Boolean,
        productId: String?,
        tagValues: Map<String, TagValueSource>? = null,
    ): StringWrapper {
        if (items.isEmpty())
            return StringWrapper.EMPTY
        if (items.size == 1) {
            val item = items.first()
            if (item is RichText.Item.Text)
                return processRichTextItemText(item, textElementAttrs, assets)
            if (item is RichText.Item.Tag)
                return processRichTextItemTag(
                    item,
                    productId,
                    textElementAttrs,
                    ignoreMissingCustomTag,
                    assets,
                    products,
                    locale,
                    tagValues,
                )
        }
        val inlineContent = mutableMapOf<String, InlineTextContent>()
        val parts = mutableListOf<StringWrapper.ComplexStr.ComplexStrPart>()
        items.forEach { item ->
            when (item) {
                is RichText.Item.Text -> {
                    val processedItem = processRichTextItemText(item, textElementAttrs, assets)
                    parts.add(StringWrapper.ComplexStr.ComplexStrPart.Text(processedItem))
                }
                is RichText.Item.Tag -> {
                    val processedItem = processRichTextItemTag(
                        item,
                        productId,
                        textElementAttrs,
                        ignoreMissingCustomTag,
                        assets,
                        products,
                        locale,
                        tagValues,
                    )
                    if (processedItem === StringWrapper.PRODUCT_NOT_FOUND)
                        return StringWrapper.EMPTY
                    if (processedItem === StringWrapper.CUSTOM_TAG_NOT_FOUND)
                        return processedItem
                    parts.add(StringWrapper.ComplexStr.ComplexStrPart.Text(processedItem))
                }
                is RichText.Item.Image -> {
                    val imageAsset = assets.getAsset<Asset.Image>(item.imageAssetId)
                        ?: return@forEach
                    val isSystemInDarkTheme = isSystemInDarkTheme()
                    val context = LocalContext.current
                    val imageBitmap = remember(imageAsset.main.source.javaClass, isSystemInDarkTheme) {
                        getBitmap(context, imageAsset)
                            ?.asImageBitmap()
                    } ?: return@forEach

                    val id = "image_${inlineContent.size}"
                    val colorFilter: ColorFilter? = item.attrs?.imageTint?.resolveColorFilter()
                    val elementResolvedAttrs = textElementAttrs?.let { ComposeTextAttrs.from(it, assets) }
                    val runResolvedAttrs = item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) }
                        ?: elementResolvedAttrs
                    val elementFontSize = elementResolvedAttrs?.fontSize ?: BaseTextElement.DEFAULT_FONT_SIZE
                    val runFontSize = runResolvedAttrs?.fontSize ?: elementFontSize
                    val density = LocalDensity.current
                    val runTypeface = runResolvedAttrs?.typeface
                    val (widthEm, heightEm) = remember(density, runFontSize, elementFontSize, runTypeface, imageBitmap) {
                        val paint = TextPaint()
                        paint.textSize = with(density) { runFontSize.sp.toPx() }
                        runTypeface?.let { paint.typeface = it }
                        val bounds = Rect()
                        paint.getTextBounds("H", 0, 1, bounds)
                        val capHeightPx = bounds.height().toFloat()
                        val elementFontPx = with(density) { elementFontSize.sp.toPx() }
                        val aspect = imageBitmap.width.toFloat() / imageBitmap.height.toFloat()
                        val heightEm = if (elementFontPx > 0f) capHeightPx / elementFontPx else 1f
                        heightEm * aspect to heightEm
                    }
                    val inlineImage = InlineTextContent(
                        Placeholder(widthEm.em, heightEm.em, PlaceholderVerticalAlign.AboveBaseline)
                    ) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = colorFilter,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    parts.add(StringWrapper.ComplexStr.ComplexStrPart.Image(id, inlineImage, item.actions))
                }
            }
        }
        return StringWrapper.ComplexStr(parts)
    }

    @Composable
    private fun processRichTextItemText(
        item: RichText.Item.Text,
        textElementAttrs: Attributes?,
        assets: Assets,
    ): StringWrapper.Str {
        return StringWrapper.Str(item.text, item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) }, item.actions)
    }

    @Composable
    private fun processRichTextItemTag(
        item: RichText.Item.Tag,
        productId: String?,
        textElementAttrs: Attributes?,
        ignoreMissingCustomTag: Boolean,
        assets: Assets,
        products: Products,
        locale: Locale,
        tagValues: Map<String, TagValueSource>? = null,
    ): StringWrapper.Single {
        return tagResolver.tryResolveInlineTag(item, tagValues, textElementAttrs, assets, locale, literalOnly = true)
            ?: tagResolver.tryResolveCustomTagOrNull(item, textElementAttrs, assets)
            ?: tagResolver.tryResolveInlineTag(item, tagValues, textElementAttrs, assets, locale, literalOnly = false)
            ?: tagResolver.tryResolveProductTag(item, productId, textElementAttrs, assets, products)
            ?: tagResolver.tryResolveMissingTag(item, textElementAttrs, assets, ignoreMissingCustomTag)
    }
}