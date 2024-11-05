@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.text

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.adapty.internal.utils.InternalAdaptyApi

@InternalAdaptyApi
public sealed class StringWrapper {
    internal sealed class Single(val value: String, val attrs: ComposeTextAttrs?): StringWrapper()
    internal class Str internal constructor(value: String, attrs: ComposeTextAttrs? = null): Single(value, attrs)
    internal class TimerSegmentStr internal constructor(
        value: String,
        val timerSegment: TimerSegment,
        attrs: ComposeTextAttrs? = null,
    ) : Single(value, attrs)

    internal class ComplexStr internal constructor(val parts: List<ComplexStrPart>): StringWrapper() {
        sealed class ComplexStrPart {
            class Text(val str: Single): ComplexStrPart()
            class Image(val id: String, val inlineContent: InlineTextContent): ComplexStrPart()
        }

        fun resolve(): AnnotatedStr {
            val inlineContent = mutableMapOf<String, InlineTextContent>()
            val annotatedString = buildAnnotatedString {
                parts.forEach { part ->
                    when (part) {
                        is ComplexStrPart.Text -> {
                            append(part.str)
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

internal class AnnotatedStr(val value: AnnotatedString, val inlineContent: Map<String, InlineTextContent>)

private fun AnnotatedString.Builder.append(processedItem: StringWrapper.Single) {
    if (processedItem.attrs == null) {
        append(processedItem.value)
    } else {
        withStyle(createSpanStyle(processedItem.attrs)) {
            append(processedItem.value)
        }
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