@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration
import com.adapty.ui.AdaptyUI.FlowConfiguration.RichText
import com.adapty.ui.AdaptyUI.FlowConfiguration.TextItem
import com.adapty.ui.internal.text.ConverterSpec
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.TagConverter
import com.adapty.ui.internal.text.TagValueSource
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.LocalTexts
import com.adapty.ui.internal.ui.LocalTimerCommands
import com.adapty.ui.internal.ui.attributes.TextAlign
import com.adapty.ui.internal.ui.resolveText
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.utils.StringSource
import kotlinx.coroutines.delay

internal const val TIMER_TAG = "TIMER"

@InternalAdaptyApi
public class TimerElement internal constructor(
    internal val id: String,
    internal val actions: List<Action>,
    internal val format: Format,
    textAlign: TextAlign,
    attributes: Attributes,
    internal val maxRows: Int?,
    internal val onOverflowMode: OnOverflowMode?,
    baseProps: BaseProps,
) : BaseTextElement(textAlign, attributes, baseProps) {

    internal class Format(val formatItemsDesc: List<FormatItem>)

    internal class FormatItem(val fromSeconds: Long, val stringId: StringId)

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = lambda@{
        val texts = LocalTexts.current
        val timerCommands = LocalTimerCommands.current
        val timerCommand = timerCommands[id]
        val screen = LocalScreenInstance.current

        val updatesPerSecond by remember(format, texts) {
            derivedStateOf { computeUpdatesPerSecond(format, texts) }
        }
        val tickIntervalMs by remember(updatesPerSecond) {
            derivedStateOf { (1000L / updatesPerSecond.coerceAtLeast(1)).coerceAtLeast(1L) }
        }

        var timerMillis by rememberSaveable(timerCommand?.endAtSeconds) {
            mutableLongStateOf(
                if (timerCommand != null)
                    (timerCommand.endAtSeconds * 1000L - System.currentTimeMillis()).coerceAtLeast(0L)
                else 0L
            )
        }

        LaunchedEffect(timerCommand?.endAtSeconds, tickIntervalMs) {
            if (timerCommand == null) return@LaunchedEffect
            val endAtMillis = timerCommand.endAtSeconds * 1000L
            while (true) {
                val remainingMillis = (endAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                timerMillis = remainingMillis
                if (remainingMillis == 0L) break
                delay(remainingMillis % tickIntervalMs + 1)
            }
            dispatch(Message.TimerCompleted(id, actions, screen))
        }

        val wholeSecondsLeft = timerMillis / 1000L
        val currentIndex = format.formatItemsDesc
            .indexOfFirst { it.fromSeconds <= wholeSecondsLeft }
            .takeIf { it != -1 }
            ?: format.formatItemsDesc.lastIndex

        val currentItem = format.formatItemsDesc.getOrNull(currentIndex) ?: return@lambda
        val secondsLeft = timerMillis / 1000.0

        val withTimerTag = injectTimerTag(currentItem.stringId, secondsLeft)
        val displayText = resolveText(withTimerTag, attributes) ?: return@lambda

        renderTextInternal(
            attributes,
            textAlign,
            maxRows,
            onOverflowMode,
            modifier,
            dispatch,
        ) {
            displayText
        }
    }
}

private val DEFAULT_TIMER_TICK_RATE = 1
private val FALLBACK_BINDING_TICK_RATE = 1

private fun injectTimerTag(stringId: StringId, secondsLeft: Double): StringId {
    if (stringId !is StringId.Str) return stringId
    val tagValues = (stringId.tagValues?.toMutableMap() ?: mutableMapOf()).apply {
        put(TIMER_TAG, TagValueSource.Literal(secondsLeft))
    }
    return StringId.Str(stringId.source, tagValues)
}

private fun computeUpdatesPerSecond(
    format: TimerElement.Format,
    texts: Map<String, TextItem>,
): Int {
    var maxRate = DEFAULT_TIMER_TICK_RATE
    for (item in format.formatItemsDesc) {
        val rate = inspectFormatItemUpdateRate(item.stringId, texts)
        if (rate > maxRate) maxRate = rate
    }
    return maxRate
}

private fun inspectFormatItemUpdateRate(stringId: StringId, texts: Map<String, TextItem>): Int {
    if (stringId !is StringId.Str) return DEFAULT_TIMER_TICK_RATE
    val source = stringId.source
    if (source !is StringSource.Value) return FALLBACK_BINDING_TICK_RATE
    val textItem = texts[source.value] ?: return DEFAULT_TIMER_TICK_RATE
    return maxOf(
        inspectRichTextUpdateRate(textItem.value),
        textItem.fallback?.let(::inspectRichTextUpdateRate) ?: DEFAULT_TIMER_TICK_RATE,
    )
}

private fun inspectRichTextUpdateRate(richText: RichText): Int {
    var rate = DEFAULT_TIMER_TICK_RATE
    for (richTextItem in richText.items) {
        if (richTextItem !is RichText.Item.Tag) continue
        if (richTextItem.tag != TIMER_TAG) continue
        val name = richTextItem.converterName ?: continue
        val converter = TagConverter.fromJson(ConverterSpec(name, richTextItem.converterParams))
            as? TagConverter.Timer ?: continue
        if (converter.updatesPerSecond > rate) rate = converter.updatesPerSecond
    }
    return rate
}
