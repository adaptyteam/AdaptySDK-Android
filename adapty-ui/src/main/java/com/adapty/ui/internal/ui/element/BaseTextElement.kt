package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.text.ComposeTextAttrs
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.TextAlign
import com.adapty.ui.internal.ui.attributes.toComposeTextAlign

@InternalAdaptyApi
public abstract class BaseTextElement(
    protected val textAlign: TextAlign,
    protected val attributes: Attributes,
    override val baseProps: BaseProps,
) : UIElement {
    public enum class OnOverflowMode {
        SCALE
    }

    public class Attributes internal constructor(
        internal val fontId: String?,
        internal val fontSize: Float?,
        internal val strikethrough: Boolean,
        internal val underline: Boolean,
        internal val textColor: Shape.Fill?,
        internal val background: Shape.Fill?,
        internal val tint: Shape.Fill?,
    )

    @Composable
    protected fun renderTextInternal(
        textAttrs: Attributes,
        textAlign: TextAlign,
        maxLines: Int?,
        onOverflow: OnOverflowMode?,
        modifier: Modifier,
        resolveAssets: ResolveAssets,
        resolveText: @Composable () -> StringWrapper?,
    ) {
        val readyToDraw = remember {
            mutableStateOf(onOverflow != OnOverflowMode.SCALE)
        }
        val initialHeightPxState = remember { mutableIntStateOf(0) }

        when (val text = resolveText()) {
            null -> return
            is StringWrapper.Single -> {
                val elementTextAttrs = ComposeTextAttrs.from(textAttrs, resolveAssets())
                val fontSize = remember {
                    mutableFloatStateOf(text.attrs?.fontSize ?: elementTextAttrs.fontSize ?: 14f)
                }
                val fontSizeSp = fontSize.floatValue.sp
                Text(
                    text = text.value,
                    color = text.attrs?.textColor ?: elementTextAttrs.textColor ?: Color.Unspecified,
                    maxLines = maxLines ?: Int.MAX_VALUE,
                    fontFamily = text.attrs?.fontFamily ?: elementTextAttrs.fontFamily,
                    fontSize = fontSizeSp,
                    lineHeight = fontSizeSp,
                    textDecoration = text.attrs?.textDecoration ?: elementTextAttrs.textDecoration,
                    textAlign = textAlign.toComposeTextAlign(),
                    onTextLayout = createOnTextLayoutCallback(onOverflow, fontSize, readyToDraw),
                    overflow = TextOverflow.Ellipsis,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    modifier = modifier
                        .retainInitialHeight(initialHeightPxState)
                        .textBackgroundOrSkip(
                            text.attrs?.backgroundColor ?: elementTextAttrs.backgroundColor
                        )
                        .drawWithContent {
                            if (readyToDraw.value) drawContent()
                        },
                )
            }
            is StringWrapper.ComplexStr -> {
                val text = text.resolve()
                val elementTextAttrs = ComposeTextAttrs.from(textAttrs, resolveAssets())
                val fontSize = remember {
                    mutableFloatStateOf(elementTextAttrs.fontSize ?: 14f)
                }
                val fontSizeSp = fontSize.floatValue.sp
                Text(
                    text = text.value,
                    inlineContent = text.inlineContent,
                    color = elementTextAttrs.textColor ?: Color.Unspecified,
                    maxLines = maxLines ?: Int.MAX_VALUE,
                    fontFamily = elementTextAttrs.fontFamily,
                    fontSize = fontSizeSp,
                    lineHeight = fontSizeSp,
                    textDecoration = elementTextAttrs.textDecoration,
                    textAlign = textAlign.toComposeTextAlign(),
                    onTextLayout = createOnTextLayoutCallback(onOverflow, fontSize, readyToDraw),
                    overflow = TextOverflow.Ellipsis,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    modifier = modifier
                        .retainInitialHeight(initialHeightPxState)
                        .textBackgroundOrSkip(elementTextAttrs.backgroundColor)
                        .drawWithContent {
                            if (readyToDraw.value) drawContent()
                        },
                )
            }
        }
    }

    @Composable
    private fun Modifier.retainInitialHeight(initialHeightPxState: MutableIntState): Modifier =
        onSizeChanged { size ->
            if (size.height <= 0 || initialHeightPxState.intValue > 0)
                return@onSizeChanged
            initialHeightPxState.intValue = size.height
        }
            .run {
                val initialHeight = initialHeightPxState.intValue.takeIf { it > 0 }
                    ?: return@run this
                height(with(LocalDensity.current) { initialHeight.toDp() })
                    .wrapContentHeight()
            }

    private fun Modifier.textBackgroundOrSkip(backgroundColor: Color?): Modifier {
        if (backgroundColor == null) return this
        return background(backgroundColor)
    }

    private fun createOnTextLayoutCallback(
        onOverflow: OnOverflowMode?,
        fontSize: MutableState<Float>,
        readyToDraw: MutableState<Boolean>,
    ) : (TextLayoutResult) -> Unit =
        when (onOverflow) {
            OnOverflowMode.SCALE -> { textLayoutResult ->
                if (textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight) {
                    runCatching { fontSize.value *= 0.9f }
                        .getOrElse { readyToDraw.value = true }
                } else {
                    readyToDraw.value = true
                }
            }
            else -> { _ -> }
        }
}