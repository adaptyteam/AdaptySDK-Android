package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.text.ComposeTextAttrs
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.attributes.TextAlign
import com.adapty.ui.internal.ui.attributes.toComposeTextAlign
import com.adapty.ui.internal.ui.OverflowResult
import com.adapty.ui.internal.ui.ScaleDownToFit
import com.adapty.ui.internal.ui.ScaleDownToFitTextAutoSize
import com.adapty.ui.internal.ui.resolveAssets
import com.adapty.ui.internal.utils.VisualValue

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
        internal val fontId: VisualValue?,
        internal val fontSize: Float?,
        internal val strikethrough: Boolean,
        internal val underline: Boolean,
        internal val textColor: VisualValue?,
        internal val background: VisualValue?,
        internal val tint: VisualValue?,
        internal val letterSpacing: Float? = null,
        internal val lineHeight: Float? = null,
    )

    @Composable
    protected fun renderTextInternal(
        textAttrs: Attributes,
        textAlign: TextAlign,
        maxLines: Int?,
        onOverflow: OnOverflowMode?,
        modifier: Modifier,
        dispatch: ((Message) -> Unit)? = null,
        resolveText: @Composable () -> StringWrapper?,
    ) {
        when (val text = resolveText()) {
            null -> return
            is StringWrapper.Single -> {
                val elementTextAttrs = ComposeTextAttrs.from(textAttrs, resolveAssets())
                val originalFontSize = text.attrs?.fontSize ?: elementTextAttrs.fontSize ?: DEFAULT_FONT_SIZE
                val overflowAtMinState = remember { mutableStateOf<OverflowResult?>(null) }
                val autoSize = remember(onOverflow, originalFontSize, maxLines, text.value, LocalDensity.current.fontScale) {
                    overflowAtMinState.value = null
                    createAutoSize(originalFontSize, onOverflow) { overflow ->
                        overflowAtMinState.value = overflow
                    }
                }
                val overflowAtMin = overflowAtMinState.value
                val fontSize = if (overflowAtMin != null) autoSize?.minFontSize?.value ?: originalFontSize else originalFontSize
                val lineHeightStyle = if (overflowAtMin?.heightOverflow == true)
                    LineHeightStyle.Default.copy(alignment = LineHeightStyle.Alignment.Top)
                else
                    null
                val resolvedLineHeight = elementTextAttrs.lineHeight?.sp ?: defaultLineHeight(autoSize, originalFontSize, elementTextAttrs.typeface)
                val textStyle = LocalTextStyle.current.merge(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    color = text.attrs?.textColor ?: elementTextAttrs.textColor ?: DEFAULT_TEXT_COLOR,
                    fontFamily = text.attrs?.fontFamily ?: elementTextAttrs.fontFamily,
                    fontSize = fontSize.sp,
                    lineHeight = resolvedLineHeight,
                    lineHeightStyle = lineHeightStyle,
                    textDecoration = text.attrs?.textDecoration ?: elementTextAttrs.textDecoration,
                    textAlign = textAlign.toComposeTextAlign(),
                    letterSpacing = elementTextAttrs.letterSpacing?.sp ?: TextUnit.Unspecified,
                    background = text.attrs?.backgroundColor ?: elementTextAttrs.backgroundColor ?: Color.Unspecified,
                )
                val heightFit = rememberHeightFitController(text.value, maxLines, onOverflow == null)
                val bgModifier = modifier.then(heightFit?.modifier ?: Modifier)
                if (text.actions.isNotEmpty() && dispatch != null) {
                    val actions = text.actions
                    val screen = LocalScreenInstance.current
                    val annotatedText = buildAnnotatedString {
                        withLink(
                            LinkAnnotation.Clickable(
                                "action",
                                TextLinkStyles(style = textStyle.toSpanStyle()),
                            ) {
                                dispatch(Message.ActionsRequested(actions, screen))
                            }
                        ) {
                            append(text.value)
                        }
                    }
                    BasicText(
                        text = annotatedText,
                        maxLines = heightFit?.maxLines?.value ?: maxLines ?: Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = heightFit?.onTextLayout,
                        style = textStyle,
                        autoSize = if (overflowAtMin != null) null else autoSize,
                        modifier = bgModifier,
                    )
                } else {
                    BasicText(
                        text = text.value,
                        maxLines = heightFit?.maxLines?.value ?: maxLines ?: Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = heightFit?.onTextLayout,
                        style = textStyle,
                        autoSize = if (overflowAtMin != null) null else autoSize,
                        modifier = bgModifier,
                    )
                }
            }
            is StringWrapper.ComplexStr -> {
                val contentKey = text.signature()
                val screen = LocalScreenInstance.current
                val text = remember(contentKey) { text.resolve(dispatch, screen) }
                val elementTextAttrs = ComposeTextAttrs.from(textAttrs, resolveAssets())
                val originalFontSize = elementTextAttrs.fontSize ?: DEFAULT_FONT_SIZE
                val overflowAtMinState = remember { mutableStateOf<OverflowResult?>(null) }
                val autoSize = remember(onOverflow, originalFontSize, maxLines, contentKey, LocalDensity.current.fontScale) {
                    overflowAtMinState.value = null
                    createAutoSize(originalFontSize, onOverflow) { overflow ->
                        overflowAtMinState.value = overflow
                    }
                }
                val overflowAtMin = overflowAtMinState.value
                val fontSize = if (overflowAtMin != null) autoSize?.minFontSize?.value ?: originalFontSize else originalFontSize
                val lineHeightStyle = if (overflowAtMin?.heightOverflow == true)
                    LineHeightStyle.Default.copy(alignment = LineHeightStyle.Alignment.Top)
                else
                    null
                val resolvedLineHeight = elementTextAttrs.lineHeight?.sp ?: defaultLineHeight(autoSize, originalFontSize, elementTextAttrs.typeface)
                val heightFit = rememberHeightFitController(contentKey, maxLines, onOverflow == null)
                BasicText(
                    text = text.value,
                    inlineContent = text.inlineContent,
                    maxLines = heightFit?.maxLines?.value ?: maxLines ?: Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = heightFit?.onTextLayout,
                    style = LocalTextStyle.current.merge(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        color = elementTextAttrs.textColor ?: DEFAULT_TEXT_COLOR,
                        fontFamily = elementTextAttrs.fontFamily,
                        fontSize = fontSize.sp,
                        lineHeight = resolvedLineHeight,
                        lineHeightStyle = lineHeightStyle,
                        textDecoration = elementTextAttrs.textDecoration,
                        textAlign = textAlign.toComposeTextAlign(),
                        letterSpacing = elementTextAttrs.letterSpacing?.sp ?: TextUnit.Unspecified,
                        background = elementTextAttrs.backgroundColor ?: Color.Unspecified,
                    ),
                    autoSize = if (overflowAtMin != null) null else autoSize,
                    modifier = modifier.then(heightFit?.modifier ?: Modifier),
                )
            }
        }
    }

    private class HeightFitController(val cap: Int) {
        val maxLines: MutableState<Int> = mutableStateOf(cap)
        private var idealBottoms: FloatArray? = null
        private var capturedWidth: Int = -1

        val onTextLayout: (TextLayoutResult) -> Unit = { result ->
            if (maxLines.value == cap) {
                idealBottoms = FloatArray(result.lineCount) { result.getLineBottom(it) }
                capturedWidth = result.layoutInput.constraints.maxWidth
            } else if (result.layoutInput.constraints.maxWidth != capturedWidth) {
                idealBottoms = null
                maxLines.value = cap
            }
        }

        @Suppress("DEPRECATION")
        val modifier: Modifier = object : LayoutModifier {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints,
            ): MeasureResult {
                val placeable = measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
                idealBottoms?.takeIf { it.isNotEmpty() }?.let { bottoms ->
                    val target = if (constraints.maxHeight == Constraints.Infinity) {
                        cap
                    } else {
                        var fit = 0
                        for (i in bottoms.indices) {
                            if (bottoms[i] <= constraints.maxHeight + 0.5f) fit = i + 1 else break
                        }
                        fit.coerceAtLeast(1).coerceAtMost(cap)
                    }
                    if (target != maxLines.value) maxLines.value = target
                }
                return layout(
                    placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth),
                    placeable.height.coerceIn(constraints.minHeight, constraints.maxHeight),
                ) { placeable.place(0, 0) }
            }

            override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
                measurable.minIntrinsicWidth(height)

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
                measurable.maxIntrinsicWidth(height)

            override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
                measurable.minIntrinsicHeight(width)

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
                measurable.maxIntrinsicHeight(width)
        }
    }

    @Composable
    private fun rememberHeightFitController(contentKey: Any?, maxLines: Int?, enabled: Boolean): HeightFitController? =
        remember(contentKey, maxLines, LocalDensity.current.fontScale, enabled) {
            if (enabled) HeightFitController(maxLines ?: Int.MAX_VALUE) else null
        }

    @Composable
    private fun defaultLineHeight(autoSize: TextAutoSize?, fontSize: Float, typeface: android.graphics.Typeface?): TextUnit {
        if (autoSize != null) return fontSize.sp
        return remember(fontSize, typeface) {
            val density = android.content.res.Resources.getSystem().displayMetrics.density
            val paint = android.text.TextPaint()
            paint.textSize = fontSize * density
            if (typeface != null) paint.typeface = typeface
            val metrics = paint.fontMetrics
            val naturalPx = metrics.descent - metrics.ascent + metrics.leading
            val naturalSp = naturalPx / density
            naturalSp.sp
        }
    }

    private fun createAutoSize(
        fontSize: Float,
        onOverflowMode: OnOverflowMode?,
        onOverflowAtMin: ((OverflowResult) -> Unit)? = null,
    ): ScaleDownToFitTextAutoSize? =
        if (onOverflowMode == OnOverflowMode.SCALE)
            TextAutoSize.ScaleDownToFit(
                minFontSize = (fontSize * MIN_FONT_SIZE_RATIO).sp,
                maxFontSize = fontSize.sp,
                onOverflowAtMin = onOverflowAtMin,
            )
        else
            null

    internal companion object {
        const val MIN_FONT_SIZE_RATIO = 0.1f
        const val DEFAULT_FONT_SIZE = 15f
        val DEFAULT_TEXT_COLOR = Color.Black
    }

    private fun StringWrapper.ComplexStr.signature(): Int {
        var h = 1
        for (p in parts) {
            h = 31 * h + when (p) {
                is StringWrapper.ComplexStr.ComplexStrPart.Text -> p.str.value.hashCode()
                is StringWrapper.ComplexStr.ComplexStrPart.Image -> p.id.hashCode()
            }
        }
        return h
    }
}
