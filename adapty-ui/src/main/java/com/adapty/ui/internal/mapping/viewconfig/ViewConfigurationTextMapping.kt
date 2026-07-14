@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.mapping.viewconfig

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.internal.utils.getAs
import com.adapty.ui.AdaptyUI.FlowConfiguration.RichText
import com.adapty.ui.AdaptyUI.FlowConfiguration.TextItem
import com.adapty.ui.internal.mapping.attributes.toAction
import com.adapty.ui.internal.mapping.attributes.toAssetVisualValue
import com.adapty.ui.internal.mapping.attributes.toVisualValue
import com.adapty.ui.internal.text.CONVERTER_PARAM_DATE_STYLE
import com.adapty.ui.internal.text.CONVERTER_PARAM_FORMAT
import com.adapty.ui.internal.text.CONVERTER_PARAM_TIME_STYLE
import com.adapty.ui.internal.ui.element.Action

private const val LOCALIZATIONS = "localizations"
private const val ID = "id"
private const val VALUE = "value"
private const val TEXT = "text"
private const val TAG = "tag"
private const val IMAGE = "image"
private const val ATTRS = "attributes"
private const val FONT = "font"
private const val SIZE = "size"
private const val STRIKE = "strike"
private const val UNDERLINE = "underline"
private const val COLOR = "color"
private const val BACKGROUND = "background"
private const val TINT = "tint"
private const val LETTER_SPACING = "letter_spacing"
private const val ACTION = "action"
private const val STRINGS = "strings"
private const val FALLBACK = "fallback"
private const val CONVERTER = "converter"

internal fun mapTexts(config: JsonObject, localesOrderedDesc: Set<String>): Map<String, TextItem> {
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

private fun mapRichText(rawRichText: Iterable<*>): RichText {
    val richTextItems = rawRichText.mapNotNull { item ->
        when(item) {
            is String -> mapRichTextItem(item)
            is Map<*, *> -> mapRichTextItem(item)
            else -> null
        }
    }
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
    val actions = rawRichTextItem.toItemActions()

    val image = rawRichTextItem.getAs<String?>(IMAGE)
    if (image != null)
        return RichText.Item.Image(image, attrs, actions)

    val tag = rawRichTextItem.getAs<String?>(TAG)
    if (tag != null) {
        val converterName = rawRichTextItem.getAs<String?>(CONVERTER)
        val converterParams = extractConverterParams(rawRichTextItem)
        return RichText.Item.Tag(tag, attrs, actions, converterName, converterParams)
    }

    val text = rawRichTextItem.getAs<String?>(TEXT)
    if (text != null)
        return RichText.Item.Text(text, attrs, actions)

    return null
}

private fun extractConverterParams(raw: Map<*, *>): Map<String, Any?>? {
    val params = buildMap {
        raw.getAs<String?>(FORMAT)?.let { put(CONVERTER_PARAM_FORMAT, it) }
        raw.getAs<String?>(CONVERTER_PARAM_DATE_STYLE)?.let { put(CONVERTER_PARAM_DATE_STYLE, it) }
        raw.getAs<String?>(CONVERTER_PARAM_TIME_STYLE)?.let { put(CONVERTER_PARAM_TIME_STYLE, it) }
    }
    return params.takeIf { it.isNotEmpty() }
}

@Suppress("UNCHECKED_CAST")
private fun Map<*, *>.toItemActions(): List<Action> {
    val raw = this[ACTION] ?: return emptyList()
    return (raw as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.toAction() }
        ?: (raw as? Map<*, *>)?.toAction()?.let { listOf(it) }
        ?: emptyList()
}

private fun mapRichTextAttrs(rawRichTextAttrs: JsonObject): RichText.Attributes {
    return RichText.Attributes(
        rawRichTextAttrs[FONT]?.toAssetVisualValue(),
        rawRichTextAttrs.getAs<Number?>(SIZE)?.toFloat(),
        rawRichTextAttrs.getAs<Boolean?>(STRIKE) ?: false,
        rawRichTextAttrs.getAs<Boolean?>(UNDERLINE) ?: false,
        rawRichTextAttrs[COLOR]?.toVisualValue(),
        rawRichTextAttrs[BACKGROUND]?.toVisualValue(),
        rawRichTextAttrs[TINT]?.toVisualValue(),
        rawRichTextAttrs.getAs<Number?>(LETTER_SPACING)?.toFloat(),
    )
}
