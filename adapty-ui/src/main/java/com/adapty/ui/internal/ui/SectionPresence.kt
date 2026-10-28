@file:OptIn(com.adapty.internal.utils.InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.adapty.ui.internal.ui.element.SectionElement
import com.adapty.ui.internal.ui.element.UIElement
import kotlin.math.roundToInt

internal class SectionPresenceSlot(val vertical: Boolean) {
    var presence: State<Float> by mutableStateOf(PRESENT)

    var lastActive: Int = 0

    private val leavingState = derivedStateOf { presence.value > 0f }
    val leaving: Boolean get() = leavingState.value

    var fillsMainAxis: Boolean by mutableStateOf(false)
}

@Composable
internal fun <T> resolveInPresenceSlot(slot: SectionPresenceSlot?, block: @Composable () -> T): T {
    if (slot == null) return block()
    var result: Any? = null
    CompositionLocalProvider(LocalSectionPresenceSlot provides slot) { result = block() }
    @Suppress("UNCHECKED_CAST")
    return result as T
}

internal val LocalSectionPresenceSlot = compositionLocalOf<SectionPresenceSlot?> { null }

internal val PRESENT: State<Float> = mutableFloatStateOf(1f)
internal val ABSENT: State<Float> = mutableFloatStateOf(0f)

@Composable
internal fun rememberSectionPresenceSlots(
    content: List<UIElement>,
    vertical: Boolean,
): List<SectionPresenceSlot?> =
    remember(content, vertical) { content.map { if (it is SectionElement) SectionPresenceSlot(vertical) else null } }

internal fun gapFraction(slots: List<SectionPresenceSlot?>, index: Int): Float {
    val own = slots[index]?.presence?.value ?: 1f
    var prev = index - 1
    while (prev >= 0 && (slots[prev]?.presence?.value ?: 1f) <= 0f) prev--
    if (prev < 0) return 0f
    var prevIsFirst = true
    for (k in 0 until prev) if ((slots[k]?.presence?.value ?: 1f) > 0f) { prevIsFirst = false; break }
    return if (prevIsFirst) own * (slots[prev]?.presence?.value ?: 1f) else own
}

internal fun stackGapsPx(spacingPx: Int, count: Int, slots: List<SectionPresenceSlot?>?): IntArray =
    IntArray(count) { i ->
        when {
            i == 0 -> 0
            slots == null || slots.size != count -> spacingPx
            else -> (spacingPx * gapFraction(slots, i)).roundToInt()
        }
    }

