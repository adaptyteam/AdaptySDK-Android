package com.adapty.internal.crossplatform.ui

import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.AdaptyFlowView
import com.adapty.ui.AdaptyUI
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import java.util.Calendar
import java.util.Date
import java.util.SimpleTimeZone
import java.util.TimeZone

internal fun AdaptyFlowView.show(
    viewConfig: AdaptyUI.FlowConfiguration,
    products: List<AdaptyPaywallProduct>?,
    eventListener: AdaptyUiEventListener,
    customAssets: AdaptyCustomAssets?,
    customTags: Map<String, String>?,
    customTimers: Map<String, String>?,
    enableSafeAreaPaddings: Boolean,
    observerModeHandler: AdaptyUiObserverModeHandler?,
) {
    showFlow(
        viewConfig,
        products = products,
        eventListener = eventListener,
        insets = if (enableSafeAreaPaddings) AdaptyFlowInsets.Unspecified else AdaptyFlowInsets.None,
        customAssets = customAssets ?: AdaptyCustomAssets.Empty,
        tagResolver = if (customTags == null) {
            AdaptyUiTagResolver.Default
        } else {
            AdaptyUiTagResolver { tag -> customTags[tag] }
        },
        timerResolver = if (customTimers == null) {
            AdaptyUiTimerResolver.Default
        } else {
            AdaptyUiTimerResolver { timerId ->
                customTimers[timerId]?.let(::endTimeStrToDate)
                    ?: AdaptyUiTimerResolver.Default.timerEndAtDate(timerId)
            }
        },
        observerModeHandler = observerModeHandler,
    )
}

private fun endTimeStrToDate(dateToParse: String): Date {
    val (dateTime, timezone) = if (dateToParse.endsWith("Z")) {
        dateToParse.dropLast(1) to "Z"
    } else {
        val tzIndex = dateToParse.lastIndexOfAny(charArrayOf('+', '-'))
        dateToParse.substring(0, tzIndex) to dateToParse.substring(tzIndex)
    }

    val (date, time) = dateTime.split("T")

    val (year, month, day) = date.split("-").map { it.toInt() }

    val (timeWithoutMillis, millis) = time.split(".").let { parts ->
        if (parts.size > 1) {
            parts[0] to parts[1].toInt()
        } else {
            parts[0] to 0
        }
    }
    val (hour, minute, second) = timeWithoutMillis.split(":").map { it.toInt() }

    val timeZone = when {
        timezone == "Z" -> TimeZone.getTimeZone("UTC")
        timezone.contains("+") -> {
            val (tzHour, tzMinute) = timezone.substring(1).split(":").map { it.toInt() }
            SimpleTimeZone(tzHour * 3600000 + tzMinute * 60000, "Custom")
        }
        else -> {
            val (tzHour, tzMinute) = timezone.substring(1).split(":").map { it.toInt() }
            SimpleTimeZone(-(tzHour * 3600000 + tzMinute * 60000), "Custom")
        }
    }

    return Calendar.getInstance(timeZone).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, millis)
    }.time
}