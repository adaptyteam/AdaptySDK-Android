@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.mapping.attributes.InteractiveAttributeMapper
import com.adapty.ui.internal.mapping.attributes.TextAttributeMapper
import com.adapty.ui.internal.text.toStringId
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.ui.element.TimerElement
import com.adapty.ui.internal.ui.element.TimerElement.Format
import com.adapty.ui.internal.ui.element.TimerElement.FormatItem
import com.adapty.ui.internal.ui.element.TimerElement.LaunchType
import com.adapty.ui.internal.ui.element.UIElement
import java.util.Calendar
import java.util.TimeZone

internal class TimerElementMapper(
    private val textAttributeMapper: TextAttributeMapper,
    private val interactiveAttributeMapper: InteractiveAttributeMapper,
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIElementMapper("timer", commonAttributeMapper), UIPlainElementMapper {
    override fun map(config: Map<*, *>, assets: Assets, refBundles: ReferenceBundles): UIElement {
        val id = (config["id"] as? String)?.takeIf { it.isNotEmpty() }
            ?: throw adaptyError(
                message = "id in Timer must not be empty",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )

        val actions = (config["action"] as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.let(interactiveAttributeMapper::mapAction) }
            ?: (config["action"] as? Map<*, *>)?.let(interactiveAttributeMapper::mapAction)?.let { action -> listOf(action) }
                .orEmpty()

        val launchType = when (val behavior = config["behaviour"]) {
            "custom" -> LaunchType.Custom
            "end_at_local_time", "end_at_utc_time" -> {
                val endTimestamp = (config["end_time"] as? String)?.let { endTime ->
                    val timeZone = if (behavior == "end_at_utc_time") TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
                    kotlin.runCatching { endTimeToTimestamp(endTime, timeZone) }.getOrNull()
                        ?: throw adaptyError(
                            message = "invalid time format: $endTime",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                } ?: throw adaptyError(
                    message = "end_time in Timer must not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
                LaunchType.EndAtTime(endTimestamp)
            }
            else -> {
                val duration = (config["duration"] as? Number)?.toLong()
                    ?: throw adaptyError(
                        message = "duration in Timer must not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                val behavior = when (config["behaviour"]) {
                    "start_at_every_appear" -> LaunchType.Duration.StartBehavior.START_AT_EVERY_APPEAR
                    "start_at_first_appear_persisted" -> LaunchType.Duration.StartBehavior.START_AT_FIRST_APPEAR_PERSISTED
                    else -> LaunchType.Duration.StartBehavior.START_AT_FIRST_APPEAR
                }
                LaunchType.Duration(duration, behavior)
            }
        }

        val format = config["format"]?.toStringId()?.let { stringId ->
            Format(listOf(FormatItem(Long.MAX_VALUE, stringId)))
        }
            ?: (config["format"] as? Iterable<*>)?.mapNotNull { item ->
                if (item !is Map<*, *>) return@mapNotNull null
                val stringId = item["string_id"]?.toStringId() ?: return@mapNotNull null
                FormatItem(
                    (item["from"] as? Number)?.toLong() ?: 60,
                    stringId,
                )
            }
                ?.sortedByDescending { it.fromSeconds }
                ?.takeIf { it.isNotEmpty() }
                ?.let { formatItems -> Format(formatItems) }
            ?: throw adaptyError(
                message = "format in Timer must not be empty",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )

        val textAlign = textAttributeMapper.mapTextAlign(config["align"])

        return TimerElement(
            id,
            actions,
            launchType,
            format,
            textAlign,
            config.toTextAttributes(),
            config.extractBaseProps(),
        )
            .also { element ->
                addToReferenceTargetsIfNeeded(config, element, refBundles)
            }
    }

    private fun Map<*, *>.toTextAttributes(): Attributes {
        return Attributes(
            this["font"] as? String,
            this["size"]?.toFloatOrNull(),
            (this["strike"] as? Boolean) ?: false,
            (this["underline"] as? Boolean) ?: false,
            (this["color"] as? String)?.let { assetId -> Shape.Fill(assetId) },
            (this["background"] as? String)?.let { assetId -> Shape.Fill(assetId) },
            (this["tint"] as? String)?.let { assetId -> Shape.Fill(assetId) },
        )
    }

    private fun endTimeToTimestamp(dateToParse: String, timeZone: TimeZone): Long {
        val (date, time) = dateToParse.split(" ")
        val (year, month, day) = date.split("-").map { it.toInt() }
        val (hour, minute, second) = time.split(":").map { it.toInt() }

        return Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
        }.timeInMillis / 1000L
    }
}