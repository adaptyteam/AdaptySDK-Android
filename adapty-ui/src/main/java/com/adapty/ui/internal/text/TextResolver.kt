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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyProductDiscountPhase.PaymentMode
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.RichText
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.TextItem
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.mapping.element.Products
import com.adapty.ui.internal.mapping.element.StateMap
import com.adapty.ui.internal.mapping.element.Texts
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.utils.firstDiscountOfferOrNull
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.getBitmap
import com.adapty.ui.internal.utils.getProductGroupKey
import com.adapty.ui.listeners.AdaptyUiTagResolver

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
        state: StateMap,
    ): StringWrapper? {
        when (stringId) {
            is StringId.Str -> return texts[stringId.value]?.toComposeString(textElementAttrs, assets, products)
            is StringId.Product -> {
                val desiredProductId = stringId.productId?.takeIf { it.isNotEmpty() }
                    ?: state[getProductGroupKey(stringId.productGroupId)] as? String
                val desiredLocalText = if (!desiredProductId.isNullOrEmpty()) {
                    val product = products[desiredProductId] ?: return StringWrapper.EMPTY
                    val paymentModeStr = when(product.firstDiscountOfferOrNull()?.paymentMode) {
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
                return desiredLocalText?.toComposeString(textElementAttrs, assets, products, desiredProductId)
            }
        }
    }

    fun setCustomTagResolver(customTagResolver: AdaptyUiTagResolver) {
        tagResolver.customTagResolver = customTagResolver
    }

    @Composable
    private fun TextItem.toComposeString(textElementAttrs: Attributes?, assets: Assets, products: Products, productId: String? = null): StringWrapper? {
        return value.toComposeString(textElementAttrs, assets, products, fallback == null, productId).let { actualStr ->
            if (actualStr === StringWrapper.CUSTOM_TAG_NOT_FOUND)
                fallback?.toComposeString(textElementAttrs, assets, products, true, productId)
            else
                actualStr
        }
    }

    @Composable
    private fun RichText.toComposeString(
        textElementAttrs: Attributes?,
        assets: Assets,
        products: Products,
        ignoreMissingCustomTag: Boolean,
        productId: String?,
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
                    val tint = item.attrs?.imageTintAssetId?.let { assetId ->
                        assets.getAsset<Asset.Color>(assetId)
                    }
                    val colorFilter = remember(isSystemInDarkTheme) {
                        tint?.toComposeFill()?.color?.let { color ->
                            ColorFilter.tint(color)
                        }
                    }
                    val inlineImage = InlineTextContent(
                        Placeholder(1.em, 1.em, PlaceholderVerticalAlign.TextCenter)
                    ) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = colorFilter,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    parts.add(StringWrapper.ComplexStr.ComplexStrPart.Image(id, inlineImage))
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
        return StringWrapper.Str(item.text, item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) })
    }

    @Composable
    private fun processRichTextItemTag(
        item: RichText.Item.Tag,
        productId: String?,
        textElementAttrs: Attributes?,
        ignoreMissingCustomTag: Boolean,
        assets: Assets,
        products: Products,
    ): StringWrapper.Single {
        return tagResolver.tryResolveProductTag(item, productId, textElementAttrs, assets, products)
            ?: tagResolver.tryResolveTimerTag(item, textElementAttrs, assets)
            ?: tagResolver.tryResolveCustomTag(item, textElementAttrs, assets, ignoreMissingCustomTag)
    }
}