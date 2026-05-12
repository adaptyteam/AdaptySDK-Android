@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.text

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.element.Action

@InternalAdaptyApi
public sealed class StringWrapper {
    internal sealed class Single(val value: String, val attrs: ComposeTextAttrs?, val actions: List<Action> = emptyList()): StringWrapper()
    internal class Str internal constructor(value: String, attrs: ComposeTextAttrs? = null, actions: List<Action> = emptyList()): Single(value, attrs, actions)
    internal class TimerSegmentStr internal constructor(
        value: String,
        val timerSegment: TimerSegment,
        attrs: ComposeTextAttrs? = null,
        actions: List<Action> = emptyList(),
    ) : Single(value, attrs, actions)

    internal class ComplexStr internal constructor(val parts: List<ComplexStrPart>): StringWrapper() {
        sealed class ComplexStrPart {
            class Text(val str: Single): ComplexStrPart()
            class Image(val id: String, val inlineContent: InlineTextContent): ComplexStrPart()
        }

        fun resolve(
            resolvedActionsByPart: Map<Int, List<Action>> = emptyMap(),
            onActions: ((List<Action>) -> Unit)? = null,
        ): AnnotatedStr {
            val inlineContent = mutableMapOf<String, InlineTextContent>()
            val annotatedString = buildAnnotatedString {
                parts.forEachIndexed { index, part ->
                    when (part) {
                        is ComplexStrPart.Text -> {
                            append(part.str, resolvedActionsByPart[index], onActions)
                        }
                        is ComplexStrPart.Image -> {
                            appendInlineContent(part.id, " ")
                            inlineContent[part.id] = part.inlineContent
                        }
                    }
                }
            }
            return AnnotatedStr(annotatedString, inlineContent)
        }
    }

    internal companion object {
        val EMPTY = Str("")
        val PRODUCT_NOT_FOUND = Str("")
        val CUSTOM_TAG_NOT_FOUND = Str("")
    }
}

internal fun StringWrapper.toPlainString() =
    when (this) {
        is StringWrapper.Single -> value
        is StringWrapper.ComplexStr -> resolve().value.text
    }

internal class AnnotatedStr(
    val value: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>,
)

private fun AnnotatedString.Builder.append(
    processedItem: StringWrapper.Single,
    resolvedActions: List<Action>? = null,
    onActions: ((List<Action>) -> Unit)? = null,
) {
    val spanStyle = processedItem.attrs?.let { createSpanStyle(it) }
    if (!resolvedActions.isNullOrEmpty() && onActions != null) {
        withLink(
            LinkAnnotation.Clickable(
                "action",
                TextLinkStyles(style = spanStyle),
            ) {
                onActions(resolvedActions)
            }
        ) {
            append(processedItem.value)
        }
    } else if (spanStyle != null) {
        withStyle(spanStyle) {
            append(processedItem.value)
        }
    } else {
        append(processedItem.value)
    }
}

private fun createSpanStyle(attrs: ComposeTextAttrs): SpanStyle {
    return SpanStyle(
        color = attrs.textColor ?: Color.Unspecified,
        fontSize = attrs.fontSize?.sp ?: TextUnit.Unspecified,
        fontFamily = attrs.fontFamily,
        background = attrs.backgroundColor ?: Color.Unspecified,
        textDecoration = attrs.textDecoration,
    )
}