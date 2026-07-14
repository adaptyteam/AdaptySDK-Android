@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(ExperimentalMaterial3Api::class, InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.script.toJsLong
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.LocalUiEnabled
import com.adapty.ui.internal.ui.attributes.DateTime
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.resolveAsset
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import androidx.compose.material3.ColorScheme
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@InternalAdaptyApi
public class DateTimePickerElement internal constructor(
    internal val kind: Kind,
    internal val value: TwoWayBinding,
    internal val components: Components,
    internal val minDate: DateTime?,
    internal val maxDate: DateTime?,
    internal val color: VisualValue?,
    override val baseProps: BaseProps,
) : UIElement {

    public enum class Kind { Compact, Wheel, Graphical }

    public class Components internal constructor(
        public val date: Boolean,
        public val hourAndMinute: Boolean,
    ) {
        internal companion object {
            val Default = Components(date = true, hourAndMinute = true)
        }
    }

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        DateTimePickerContent(this@DateTimePickerElement, dispatch, modifier)
    }
}

@Composable
private fun DateTimePickerContent(
    element: DateTimePickerElement,
    dispatch: (Message) -> Unit,
    modifier: Modifier,
) {
    val state = resolveState()
    val screen = LocalScreenInstance.current

    val nowMs = remember { System.currentTimeMillis() }
    val currentMs = state[element.value].toJsLong() ?: nowMs

    val tint = element.color?.resolveAsset<Asset.Color>()?.main?.value?.let { Color(it) }
        ?: MaterialTheme.colorScheme.primary

    val resolvedMin = element.minDate?.resolveTimestampMs(nowMs)
    val resolvedMax = element.maxDate?.resolveTimestampMs(nowMs)
    val minMs: Long?
    val maxMs: Long?
    if (resolvedMin != null && resolvedMax != null && resolvedMin > resolvedMax) {
        LaunchedEffect(Unit) {
            log(WARN) { "$LOG_PREFIX DateTimePicker: min ($resolvedMin) is later than max ($resolvedMax); clamping min to max" }
        }
        minMs = resolvedMax
        maxMs = resolvedMax
    } else {
        minMs = resolvedMin
        maxMs = resolvedMax
    }

    val showDate = element.components.date
    val showTime = element.components.hourAndMinute
    val effectiveShow = if (!showDate && !showTime) {
        DateTimePickerElement.Components.Default
    } else {
        element.components
    }

    val onDateChanged: (Long) -> Unit = { newMs ->
        val clampedMs = clampMs(newMs, minMs, maxMs)
        dispatch(Message.ValueChanged(element.value, clampedMs, screen))
    }

    val gateModifier = if (LocalUiEnabled.current) modifier else modifier.consumePointerInput()

    Box(modifier = gateModifier) {
        when (element.kind) {
            DateTimePickerElement.Kind.Compact -> CompactPicker(
                currentMs = currentMs,
                showDate = effectiveShow.date,
                showTime = effectiveShow.hourAndMinute,
                minMs = minMs,
                maxMs = maxMs,
                tint = tint,
                onChange = onDateChanged,
            )
            DateTimePickerElement.Kind.Wheel -> WheelPicker(
                currentMs = currentMs,
                showDate = effectiveShow.date,
                showTime = effectiveShow.hourAndMinute,
                minMs = minMs,
                maxMs = maxMs,
                tint = tint,
                onChange = onDateChanged,
            )
            DateTimePickerElement.Kind.Graphical -> GraphicalPicker(
                currentMs = currentMs,
                showDate = effectiveShow.date,
                showTime = effectiveShow.hourAndMinute,
                minMs = minMs,
                maxMs = maxMs,
                tint = tint,
                onChange = onDateChanged,
            )
        }
    }
}

@Composable
private fun GraphicalPicker(
    currentMs: Long,
    showDate: Boolean,
    showTime: Boolean,
    minMs: Long?,
    maxMs: Long?,
    tint: Color,
    onChange: (Long) -> Unit,
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.withTint(tint),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showDate) {
                InlineDatePicker(
                    currentMs = currentMs,
                    minMs = minMs,
                    maxMs = maxMs,
                    onChange = { newDateMs -> onChange(combineDateAndTime(newDateMs, currentMs)) },
                )
            }
            if (showTime) {
                InlineTimePicker(
                    currentMs = currentMs,
                    onChange = { hour, minute -> onChange(replaceTime(currentMs, hour, minute)) },
                )
            }
        }
    }
}

@Composable
private fun WheelPicker(
    currentMs: Long,
    showDate: Boolean,
    showTime: Boolean,
    minMs: Long?,
    maxMs: Long?,
    tint: Color,
    onChange: (Long) -> Unit,
) {
    val cal = remember(currentMs) { Calendar.getInstance().apply { timeInMillis = currentMs } }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)

    val minYear = minMs?.let { yearOf(it) } ?: (year - 100)
    val maxYear = maxMs?.let { yearOf(it) } ?: (year + 100)
    val years = remember(minYear, maxYear) {
        val lo = minOf(minYear, maxYear)
        val hi = maxOf(minYear, maxYear)
        (lo..hi).toList()
    }
    val yearLabels = remember(years) { years.map { it.toString() } }
    val monthLabels = remember {
        val symbols = DateFormatSymbols.getInstance(Locale.getDefault())
        symbols.shortMonths.take(12)
    }
    val daysInMonth = remember(year, month) { daysInMonth(year, month) }
    val dayLabels = remember(daysInMonth) { (1..daysInMonth).map { it.toString() } }
    val hourLabels = remember { (0..23).map { it.toString().padStart(2, '0') } }
    val minuteLabels = remember { (0..59).map { it.toString().padStart(2, '0') } }

    val indicatorColor = tint.copy(alpha = 0.12f)
    val selectedTextColor = MaterialTheme.colorScheme.onSurface
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    val yearIndex = (years.indexOf(year)).coerceAtLeast(0)
    val monthIndex = month
    val dayIndex = (day - 1).coerceIn(0, daysInMonth - 1)
    val hourIndex = hour
    val minuteIndex = minute

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (showDate) {
            WheelColumn(
                labels = monthLabels,
                selectedIndex = monthIndex,
                onSelectedIndexChange = { idx ->
                    if (idx != monthIndex) onChange(buildMs(year, idx, day, hour, minute))
                },
                modifier = Modifier.weight(1.2f),
                indicatorColor = indicatorColor,
                selectedTextColor = selectedTextColor,
                textColor = textColor,
            )
            WheelColumn(
                labels = dayLabels,
                selectedIndex = dayIndex,
                onSelectedIndexChange = { idx ->
                    val newDay = idx + 1
                    if (newDay != day) onChange(buildMs(year, month, newDay, hour, minute))
                },
                modifier = Modifier.weight(0.8f),
                indicatorColor = indicatorColor,
                selectedTextColor = selectedTextColor,
                textColor = textColor,
            )
            WheelColumn(
                labels = yearLabels,
                selectedIndex = yearIndex,
                onSelectedIndexChange = { idx ->
                    val newYear = years.getOrNull(idx) ?: return@WheelColumn
                    if (newYear != year) onChange(buildMs(newYear, month, day, hour, minute))
                },
                modifier = Modifier.weight(1.1f),
                indicatorColor = indicatorColor,
                selectedTextColor = selectedTextColor,
                textColor = textColor,
            )
        }
        if (showTime) {
            WheelColumn(
                labels = hourLabels,
                selectedIndex = hourIndex,
                onSelectedIndexChange = { idx ->
                    if (idx != hourIndex) onChange(buildMs(year, month, day, idx, minute))
                },
                modifier = Modifier.weight(0.8f),
                indicatorColor = indicatorColor,
                selectedTextColor = selectedTextColor,
                textColor = textColor,
            )
            WheelColumn(
                labels = minuteLabels,
                selectedIndex = minuteIndex,
                onSelectedIndexChange = { idx ->
                    if (idx != minuteIndex) onChange(buildMs(year, month, day, hour, idx))
                },
                modifier = Modifier.weight(0.8f),
                indicatorColor = indicatorColor,
                selectedTextColor = selectedTextColor,
                textColor = textColor,
            )
        }
    }
}

@Composable
private fun CompactPicker(
    currentMs: Long,
    showDate: Boolean,
    showTime: Boolean,
    minMs: Long?,
    maxMs: Long?,
    tint: Color,
    onChange: (Long) -> Unit,
) {
    var dateDialogOpen by remember { mutableStateOf(false) }
    var timeDialogOpen by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showDate) {
            CompactChip(
                label = formatDate(currentMs),
                tint = tint,
                onClick = { dateDialogOpen = true },
            )
        }
        if (showTime) {
            CompactChip(
                label = formatTime(currentMs),
                tint = tint,
                onClick = { timeDialogOpen = true },
            )
        }
    }

    if (dateDialogOpen) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = currentMs,
            selectableDates = remember(minMs, maxMs) { rangeSelectableDates(minMs, maxMs) },
        )
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.withTint(tint),
        ) {
            DatePickerDialog(
                onDismissRequest = { dateDialogOpen = false },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { ms -> onChange(combineDateAndTime(ms, currentMs)) }
                        dateDialogOpen = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { dateDialogOpen = false }) { Text("Cancel") }
                },
            ) {
                DatePicker(state = state)
            }
        }
    }

    if (timeDialogOpen) {
        val cal = remember(currentMs) { Calendar.getInstance().apply { timeInMillis = currentMs } }
        val state = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false,
        )
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.withTint(tint),
        ) {
            Dialog(onDismissRequest = { timeDialogOpen = false }) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimePicker(state = state)
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { timeDialogOpen = false }) { Text("Cancel") }
                            TextButton(onClick = {
                                onChange(replaceTime(currentMs, state.hour, state.minute))
                                timeDialogOpen = false
                            }) { Text("OK") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactChip(label: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text = label, color = tint)
    }
}

@Composable
private fun InlineDatePicker(
    currentMs: Long,
    minMs: Long?,
    maxMs: Long?,
    onChange: (Long) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = currentMs,
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = remember(minMs, maxMs) { rangeSelectableDates(minMs, maxMs) },
    )
    DatePicker(state = state, title = null, headline = null, showModeToggle = false)
    LaunchedEffect(state) {
        snapshotFlow { state.selectedDateMillis }
            .distinctUntilChanged()
            .collect { ms -> if (ms != null && ms != currentMs) onChange(ms) }
    }
}

@Composable
private fun InlineTimePicker(
    currentMs: Long,
    onChange: (Int, Int) -> Unit,
) {
    val cal = remember(currentMs) { Calendar.getInstance().apply { timeInMillis = currentMs } }
    val initialHour = cal.get(Calendar.HOUR_OF_DAY)
    val initialMinute = cal.get(Calendar.MINUTE)
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false,
    )
    TimePicker(state = state)
    LaunchedEffect(state) {
        snapshotFlow { state.hour to state.minute }
            .distinctUntilChanged()
            .collect { (h, m) ->
                if (h != initialHour || m != initialMinute) onChange(h, m)
            }
    }
}

private fun Modifier.consumePointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

private fun ColorScheme.withTint(tint: Color): ColorScheme = copy(
    primary = tint,
    primaryContainer = tint.copy(alpha = 0.12f),
    onPrimaryContainer = tint,
    tertiary = tint,
    tertiaryContainer = tint.copy(alpha = 0.12f),
    onTertiaryContainer = tint,
)

private fun rangeSelectableDates(minMs: Long?, maxMs: Long?): SelectableDates {
    if (minMs == null && maxMs == null) return DatePickerDefaults.AllDates
    return object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            if (minMs != null && utcTimeMillis < startOfDayUtc(minMs)) return false
            if (maxMs != null && utcTimeMillis > startOfDayUtc(maxMs)) return false
            return true
        }
        override fun isSelectableYear(year: Int): Boolean {
            val minYear = minMs?.let { yearOf(it) } ?: Int.MIN_VALUE
            val maxYear = maxMs?.let { yearOf(it) } ?: Int.MAX_VALUE
            return year in minYear..maxYear
        }
    }
}

private fun startOfDayUtc(ms: Long): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = ms
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun yearOf(ms: Long): Int =
    Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.YEAR)

private fun combineDateAndTime(dateMs: Long, currentMs: Long): Long {
    val datePart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateMs }
    val cal = Calendar.getInstance().apply { timeInMillis = currentMs }
    cal.set(Calendar.YEAR, datePart.get(Calendar.YEAR))
    cal.set(Calendar.MONTH, datePart.get(Calendar.MONTH))
    cal.set(Calendar.DAY_OF_MONTH, datePart.get(Calendar.DAY_OF_MONTH))
    return cal.timeInMillis
}

private fun replaceTime(currentMs: Long, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = currentMs }
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun buildMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
    val safeDay = day.coerceIn(1, daysInMonth(year, month))
    val cal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, safeDay)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun daysInMonth(year: Int, month: Int): Int {
    val cal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun clampMs(ms: Long, minMs: Long?, maxMs: Long?): Long {
    var result = ms
    if (minMs != null && result < minMs) result = minMs
    if (maxMs != null && result > maxMs) result = maxMs
    return result
}

private fun formatDate(ms: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ms))

private fun formatTime(ms: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
