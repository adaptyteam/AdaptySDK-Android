package com.adapty.ui.internal.ui.element

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.text.TimerSegment
import com.adapty.ui.internal.ui.attributes.TextAlign
import com.adapty.ui.internal.ui.element.TimerElement.LaunchType.Duration.StartBehavior
import com.adapty.ui.internal.utils.EventCallback
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@InternalAdaptyApi
public class TimerElement internal constructor(
    internal val id: String,
    internal val actions: List<Action>,
    internal val launchType: LaunchType,
    internal val format: Format,
    textAlign: TextAlign,
    attributes: Attributes,
    baseProps: BaseProps,
) : BaseTextElement(textAlign, attributes, baseProps) {

    public sealed class LaunchType {
        public class EndAtTime internal constructor(internal val endTimestamp: Long): LaunchType()
        public class Duration internal constructor(internal val seconds: Long, internal val startBehavior: StartBehavior): LaunchType() {
            internal enum class StartBehavior {
                START_AT_EVERY_APPEAR,
                START_AT_FIRST_APPEAR,
                START_AT_FIRST_APPEAR_PERSISTED,
            }
        }
        public object Custom: LaunchType()
    }

    internal class Format(val formatItemsDesc: List<FormatItem>)

    internal class FormatItem(val fromSeconds: Long, val stringId: StringId)

    override fun toComposable(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = lambda@{
        val timerFormatStrs = format.formatItemsDesc
            .map { it.fromSeconds to resolveText(it.stringId) }
            .takeIf { it.isNotEmpty() }
            ?: return@lambda

        var currentIndex by remember {
            mutableIntStateOf(0)
        }

        val nextSecondsThreshold by remember {
            derivedStateOf {
                timerFormatStrs.getOrNull(currentIndex+1)?.first ?: Long.MIN_VALUE
            }
        }

        val timerFormatStr = timerFormatStrs.getOrNull(currentIndex)?.second ?: return@lambda

        renderTimerInternal(
            timerFormatStr,
            eventCallback,
            resolveAssets,
            resolveText,
            modifier,
            onInitialSecondsLeft = { secondsLeft ->
                if (nextSecondsThreshold >= secondsLeft) {
                    var i = currentIndex
                    while (i < timerFormatStrs.lastIndex && timerFormatStrs[i + 1].first >= secondsLeft)
                        i++
                    currentIndex = i
                }
            },
            onTick = { secondsLeft ->
                if (nextSecondsThreshold == secondsLeft)
                    currentIndex++
            },
        )
    }

    private fun getCurrentTimestamp() = System.currentTimeMillis() / 1000L

    @Composable
    internal fun renderTimerInternal(
        timerFormat: StringWrapper,
        callback: EventCallback,
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        modifier: Modifier,
        onInitialSecondsLeft: (secondsLeft: Long) -> Unit,
        onTick: (secondsLeft: Long) -> Unit,
    ) {
        var timerValue by rememberSaveable {
            mutableLongStateOf(
                (when(launchType) {
                    is LaunchType.Duration -> {
                        val duration = launchType.seconds
                        when (val startBehavior = launchType.startBehavior) {
                            StartBehavior.START_AT_EVERY_APPEAR ->  duration.also(onInitialSecondsLeft)
                            StartBehavior.START_AT_FIRST_APPEAR, StartBehavior.START_AT_FIRST_APPEAR_PERSISTED -> {
                                val isPersisted = startBehavior == StartBehavior.START_AT_FIRST_APPEAR_PERSISTED
                                val previousStartTimestamp = callback.getTimerStartTimestamp(id, isPersisted)
                                if (previousStartTimestamp == null) {
                                    duration.also {
                                        callback.setTimerStartTimestamp(id, getCurrentTimestamp(), isPersisted)
                                    }
                                } else {
                                    (duration - (getCurrentTimestamp() - previousStartTimestamp))
                                        .coerceAtLeast(0L)
                                        .also(onInitialSecondsLeft)
                                }
                            }
                        }
                    }
                    is LaunchType.EndAtTime -> {
                        (launchType.endTimestamp - getCurrentTimestamp())
                            .coerceAtLeast(0L)
                            .also(onInitialSecondsLeft)
                    }
                    is LaunchType.Custom -> {
                        ((callback.timerEndAtDate(id).time - System.currentTimeMillis()) / 1000L)
                            .coerceAtLeast(0L)
                            .also(onInitialSecondsLeft)
                    }
                }) * 1000L
            )
        }

        val actionsResolved = actions.mapNotNull { action -> action.resolve(resolveText) }

        val lastTimeSegment by remember(timerFormat) {
            mutableStateOf(
                when (timerFormat) {
                    is StringWrapper.TimerSegmentStr -> timerFormat.timerSegment
                    is StringWrapper.ComplexStr -> timerFormat.parts
                        .filterIsInstance<StringWrapper.ComplexStr.ComplexStrPart.Text>().map { it.str }
                        .filterIsInstance<StringWrapper.TimerSegmentStr>().lastOrNull()?.timerSegment
                    else -> null
                }
            )
        }

        val countdownSegments by remember {
            mutableStateOf(listOf(TimerSegment.MILLISECONDS, TimerSegment.CENTISECONDS, TimerSegment.DECISECONDS))
        }

        val isCountdown by remember(lastTimeSegment) {
            derivedStateOf { lastTimeSegment in countdownSegments }
        }

        LaunchedEffect(isCountdown) {
            while(timerValue > 0L) {
                when (lastTimeSegment) {
                    TimerSegment.DECISECONDS, TimerSegment.CENTISECONDS, TimerSegment.MILLISECONDS -> {
                        val subtrahend = 125L
                        delay(subtrahend.milliseconds)
                        timerValue -= subtrahend
                        val divisor = 1000L
                        if (timerValue % divisor == 0L) {
                            onTick(timerValue / divisor)
                        }
                    }
                    else -> {
                        delay(1.seconds)
                        timerValue -= 1000L
                        onTick(timerValue / 1000L)
                    }
                }
            }
            if (timerValue == 0L)
                callback.onActions(actionsResolved)
        }

        val timerValueStr by remember(timerFormat) {
            derivedStateOf {
                var timerMillisLeft = timerValue.milliseconds
                when (timerFormat) {
                    is StringWrapper.Str -> timerFormat
                    is StringWrapper.TimerSegmentStr -> {
                        val currentFormattedString: String
                        when (timerFormat.timerSegment) {
                            TimerSegment.UNKNOWN -> {
                                currentFormattedString = timerFormat.value
                            }
                            TimerSegment.DAYS -> {
                                val days = timerMillisLeft.inWholeDays
                                currentFormattedString = timerFormat.value.format(days)
                                timerMillisLeft -= days.days
                            }
                            TimerSegment.HOURS -> {
                                val hours = timerMillisLeft.inWholeHours
                                currentFormattedString = timerFormat.value.format(hours)
                                timerMillisLeft -= hours.hours
                            }
                            TimerSegment.MINUTES -> {
                                val minutes = timerMillisLeft.inWholeMinutes
                                currentFormattedString = timerFormat.value.format(minutes)
                                timerMillisLeft -= minutes.minutes
                            }
                            TimerSegment.SECONDS -> {
                                val seconds = timerMillisLeft.inWholeSeconds
                                currentFormattedString = timerFormat.value.format(seconds)
                                timerMillisLeft -= seconds.seconds
                            }
                            TimerSegment.DECISECONDS -> {
                                val deciseconds = timerMillisLeft.inWholeMilliseconds / 100
                                currentFormattedString = timerFormat.value.format(deciseconds)
                            }
                            TimerSegment.CENTISECONDS -> {
                                val centiseconds = timerMillisLeft.inWholeMilliseconds / 10 + Random.nextLong(10L)
                                currentFormattedString = timerFormat.value.format(centiseconds)
                            }
                            TimerSegment.MILLISECONDS -> {
                                val milliseconds = timerMillisLeft.inWholeMilliseconds + Random.nextLong(100L)
                                currentFormattedString = timerFormat.value.format(milliseconds)
                            }
                        }
                        StringWrapper.Str(currentFormattedString, timerFormat.attrs)
                    }

                    is StringWrapper.ComplexStr -> {
                        val mappedParts = timerFormat.parts.map { part ->
                            when (part) {
                                is StringWrapper.ComplexStr.ComplexStrPart.Text -> {
                                    when (val str = part.str) {
                                        is StringWrapper.Str -> StringWrapper.ComplexStr.ComplexStrPart.Text(str)
                                        is StringWrapper.TimerSegmentStr -> {
                                            val currentFormattedString: String
                                            when (str.timerSegment) {
                                                TimerSegment.UNKNOWN -> return@map StringWrapper.ComplexStr.ComplexStrPart.Text(str)
                                                TimerSegment.DAYS -> {
                                                    val days = timerMillisLeft.inWholeDays
                                                    currentFormattedString = str.value.format(days)
                                                    timerMillisLeft -= days.days
                                                }
                                                TimerSegment.HOURS -> {
                                                    val hours = timerMillisLeft.inWholeHours
                                                    currentFormattedString = str.value.format(hours)
                                                    timerMillisLeft -= hours.hours
                                                }
                                                TimerSegment.MINUTES -> {
                                                    val minutes = timerMillisLeft.inWholeMinutes
                                                    currentFormattedString = str.value.format(minutes)
                                                    timerMillisLeft -= minutes.minutes
                                                }
                                                TimerSegment.SECONDS -> {
                                                    val seconds = timerMillisLeft.inWholeSeconds
                                                    currentFormattedString = str.value.format(seconds)
                                                    timerMillisLeft -= seconds.seconds
                                                }
                                                TimerSegment.DECISECONDS -> {
                                                    val deciseconds = timerMillisLeft.inWholeMilliseconds / 100
                                                    currentFormattedString = str.value.format(deciseconds)
                                                }
                                                TimerSegment.CENTISECONDS -> {
                                                    val centiseconds = timerMillisLeft.inWholeMilliseconds / 10 + Random.nextLong(10L)
                                                    currentFormattedString = str.value.format(centiseconds)
                                                }
                                                TimerSegment.MILLISECONDS -> {
                                                    val milliseconds = timerMillisLeft.inWholeMilliseconds + Random.nextLong(100L)
                                                    currentFormattedString = str.value.format(milliseconds)
                                                }
                                            }
                                            StringWrapper.ComplexStr.ComplexStrPart.Text(
                                                StringWrapper.Str(currentFormattedString, str.attrs)
                                            )
                                        }
                                    }
                                }
                                is StringWrapper.ComplexStr.ComplexStrPart.Image -> part
                            }
                        }
                        StringWrapper.ComplexStr(mappedParts)
                    }
                }
            }
        }

        renderTextInternal(
            attributes,
            textAlign,
            1,
            OnOverflowMode.SCALE,
            modifier,
            resolveAssets,
        ) {
            timerValueStr
        }
    }
}