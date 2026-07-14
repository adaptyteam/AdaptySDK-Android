@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.script.toJsFloat
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.text.CONVERTER_PARAM_FORMAT
import com.adapty.ui.internal.text.ConverterSpec
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.TagValueSource
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.attributes.Interpolator
import com.adapty.ui.internal.ui.attributes.TextAlign
import com.adapty.ui.internal.ui.attributes.toEasing
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.ui.resolveText
import com.adapty.ui.internal.utils.OneWayBinding
import kotlinx.coroutines.delay

internal const val PERCENT_TAG = "PERCENT"

private val DEFAULT_PERCENT_CONVERTER = ConverterSpec(
    name = "percent",
    params = mapOf(CONVERTER_PARAM_FORMAT to "%d"),
)

@InternalAdaptyApi
public class TextProgressElement internal constructor(
    internal val value: OneWayBinding,
    internal val durationMillis: Int,
    internal val min: Float,
    internal val max: Float,
    internal val skipAnimationOnOverflow: Boolean,
    internal val interpolator: Interpolator,
    internal val actions: List<Action>,
    internal val format: Format,
    textAlign: TextAlign,
    attributes: Attributes,
    baseProps: BaseProps,
) : BaseTextElement(textAlign, attributes, baseProps) {

    internal class Format(val formatItemsDesc: List<FormatItem>)

    internal class FormatItem(val fromValue: Double, val stringId: StringId)

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = lambda@{
        val state = resolveState()
        val screen = LocalScreenInstance.current
        val rawValue = state[value].toJsFloat() ?: 0f
        val span = max - min
        val targetValue = if (span > 0f) ((rawValue - min) / span).coerceIn(0f, 1f) else 0f
        val skipAnimation = skipAnimationOnOverflow && (rawValue < min || rawValue > max)

        var prevRaw by remember { mutableFloatStateOf(rawValue) }
        val animatedValue by animateFloatAsState(
            targetValue = targetValue,
            animationSpec = if (skipAnimation) snap() else tween(
                durationMillis = durationMillis,
                easing = interpolator.toEasing(),
            ),
            label = "text_progress",
        )

        LaunchedEffect(rawValue) {
            if (rawValue != prevRaw) {
                prevRaw = rawValue
                if (actions.isNotEmpty()) {
                    delay(if (skipAnimation) 0L else durationMillis.toLong())
                    dispatch(Message.ActionsRequested(actions, screen))
                }
            }
        }

        val resolvedFormats = format.formatItemsDesc
            .map { item ->
                val sid = item.stringId
                val withProgressTag = if (sid is StringId.Str) {
                    val tagValues = (sid.tagValues?.toMutableMap() ?: mutableMapOf()).apply {
                        put(PERCENT_TAG, TagValueSource.Literal(animatedValue, DEFAULT_PERCENT_CONVERTER))
                    }
                    StringId.Str(sid.source, tagValues)
                } else sid
                item.fromValue to resolveText(withProgressTag, attributes)
            }
            .takeIf { it.isNotEmpty() }
            ?: return@lambda

        val currentIndex = run {
            var idx = 0
            for (i in resolvedFormats.indices) {
                if (resolvedFormats[i].first <= animatedValue.toDouble()) {
                    idx = i
                    break
                }
            }
            idx
        }

        val displayText = resolvedFormats.getOrNull(currentIndex)?.second ?: return@lambda

        renderTextInternal(
            attributes,
            textAlign,
            1,
            OnOverflowMode.SCALE,
            modifier,
            dispatch,
        ) {
            displayText
        }
    }
}
