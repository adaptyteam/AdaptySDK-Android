@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.viewconfig

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.RichText
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.TextItem
import com.adapty.ui.internal.utils.getAs

internal class ViewConfigurationTextMapper {

    private companion object {
        const val LOCALIZATIONS = "localizations"
        const val ID = "id"
        const val VALUE = "value"
        const val TEXT = "text"
        const val TAG = "tag"
        const val IMAGE = "image"
        const val ATTRS = "attributes"
        const val FONT = "font"
        const val SIZE = "size"
        const val STRIKE = "strike"
        const val UNDERLINE = "underline"
        const val COLOR = "color"
        const val BACKGROUND = "background"
        const val TINT = "tint"
        const val STRINGS = "strings"
        const val FALLBACK = "fallback"
    }

    fun map(config: JsonObject, localesOrderedDesc: Set<String>): Map<String, TextItem> {
        val rawTextItems = mutableMapOf<String, JsonObject>()
        localesOrderedDesc.forEach { locale ->
            config.getAs<JsonArray>(LOCALIZATIONS)
                ?.firstOrNull { it.getAs<String>(ID) == locale }
                ?.getAs<JsonArray>(STRINGS)
                ?.mapNotNull { rawTextItem ->
                    rawTextItem.getAs<String>(ID)
                        ?.let { id -> id to rawTextItem }
                }
                ?.toMap()
                ?.let { localizedRawTextItems ->
                    rawTextItems.putAll(localizedRawTextItems)
                }
        }

        return rawTextItems.mapNotNull { (id, rawTextItem) ->
            id to mapTextItem(rawTextItem)
        }.toMap()
    }

    private fun mapTextItem(rawTextItem: JsonObject): TextItem {
        val textItemId = rawTextItem.getAs<String>(ID)
        val textItemValue = rawTextItem.getAsRichText(VALUE)
        if (textItemId == null || textItemValue == null) {
            throw adaptyError(
                message = "id and value in strings in Localization should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }
        return TextItem(
            textItemValue,
            rawTextItem.getAsRichText(FALLBACK),
        )
    }

    private fun JsonObject.getAsRichText(key: String) =
        getAs<Iterable<*>>(key)?.let(::mapRichText)
            ?: getAs<JsonObject>(key)?.let(::mapRichText)
            ?: getAs<String>(key)?.let { item -> RichText(mapRichTextItem(item)) }

    private fun mapRichText(rawRichText: Iterable<*>): RichText? {
        val richTextItems = rawRichText.mapNotNull { item ->
            when(item) {
                is String -> mapRichTextItem(item)
                is Map<*, *> -> mapRichTextItem(item)
                else -> null
            }
        }
            .takeIf { it.isNotEmpty() }
            ?: return null
        return RichText(richTextItems)
    }

    private fun mapRichText(rawRichText: JsonObject): RichText? {
        val richTextItem = mapRichTextItem(rawRichText) ?: return null
        return RichText(richTextItem)
    }

    private fun mapRichTextItem(rawRichTextItem: String): RichText.Item {
        return RichText.Item.Text(rawRichTextItem, null)
    }

    private fun mapRichTextItem(rawRichTextItem: Map<*, *>): RichText.Item? {
        val attrs = rawRichTextItem.getAs<JsonObject>(ATTRS)?.let(::mapRichTextAttrs)

        val image = rawRichTextItem.getAs<String?>(IMAGE)
        if (image != null)
            return RichText.Item.Image(image, attrs)

        val tag = rawRichTextItem.getAs<String?>(TAG)
        if (tag != null)
            return RichText.Item.Tag(tag, attrs)

        val text = rawRichTextItem.getAs<String?>(TEXT)
        if (text != null)
            return RichText.Item.Text(text, attrs)

        return null
    }

    private fun mapRichTextAttrs(rawRichTextAttrs: JsonObject): RichText.Attributes {
        return RichText.Attributes(
            rawRichTextAttrs.getAs<String?>(FONT),
            rawRichTextAttrs.getAs<Number?>(SIZE)?.toFloat(),
            rawRichTextAttrs.getAs<Boolean?>(STRIKE) ?: false,
            rawRichTextAttrs.getAs<Boolean?>(UNDERLINE) ?: false,
            rawRichTextAttrs.getAs<String?>(COLOR),
            rawRichTextAttrs.getAs<String?>(BACKGROUND),
            rawRichTextAttrs.getAs<String?>(TINT),
        )
    }
}