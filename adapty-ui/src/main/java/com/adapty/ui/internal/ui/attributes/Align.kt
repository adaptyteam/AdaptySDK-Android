@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError

internal enum class VerticalAlign(internal val intValue: Int) {
    CENTER(0b0),
    TOP(0b1),
    BOTTOM(0b10),
}

internal enum class HorizontalAlign(internal val intValue: Int) {
    CENTER(0b100),
    START(0b1000),
    LEFT(0b1100),
    END(0b10000),
    RIGHT(0b10100),
}

internal enum class Align(internal val intValue: Int) {
    TOP_START(VerticalAlign.TOP.intValue or HorizontalAlign.START.intValue),
    TOP_CENTER(VerticalAlign.TOP.intValue or HorizontalAlign.CENTER.intValue),
    TOP_END(VerticalAlign.TOP.intValue or HorizontalAlign.END.intValue),
    CENTER_START(VerticalAlign.CENTER.intValue or HorizontalAlign.START.intValue),
    CENTER(VerticalAlign.CENTER.intValue or HorizontalAlign.CENTER.intValue),
    CENTER_END(VerticalAlign.CENTER.intValue or HorizontalAlign.END.intValue),
    BOTTOM_START(VerticalAlign.BOTTOM.intValue or HorizontalAlign.START.intValue),
    BOTTOM_CENTER(VerticalAlign.BOTTOM.intValue or HorizontalAlign.CENTER.intValue),
    BOTTOM_END(VerticalAlign.BOTTOM.intValue or HorizontalAlign.END.intValue),
    TOP_LEFT(VerticalAlign.TOP.intValue or HorizontalAlign.LEFT.intValue),
    TOP_RIGHT(VerticalAlign.TOP.intValue or HorizontalAlign.RIGHT.intValue),
    CENTER_LEFT(VerticalAlign.CENTER.intValue or HorizontalAlign.LEFT.intValue),
    CENTER_RIGHT(VerticalAlign.CENTER.intValue or HorizontalAlign.RIGHT.intValue),
    BOTTOM_LEFT(VerticalAlign.BOTTOM.intValue or HorizontalAlign.LEFT.intValue),
    BOTTOM_RIGHT(VerticalAlign.BOTTOM.intValue or HorizontalAlign.RIGHT.intValue);

    companion object {
        fun getOrNull(intValue: Int) = values().firstOrNull { it.intValue == intValue }
    }
}

internal operator fun HorizontalAlign.plus(other: VerticalAlign): Align {
    return Align.getOrNull(this.intValue or other.intValue)
        ?: throw adaptyError(
            message = "Can't find composite alignment from ${this.name} (${this.intValue}) and ${other.name} (${other.intValue})",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
}

internal operator fun VerticalAlign.plus(other: HorizontalAlign): Align {
    return Align.getOrNull(this.intValue or other.intValue)
        ?: throw adaptyError(
            message = "Can't find composite alignment from ${this.name} (${this.intValue}) and ${other.name} (${other.intValue})",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
}

internal fun VerticalAlign.toComposeAlignment(): Alignment.Vertical {
    return when(this) {
        VerticalAlign.CENTER -> Alignment.CenterVertically
        VerticalAlign.TOP -> Alignment.Top
        VerticalAlign.BOTTOM -> Alignment.Bottom
    }
}

internal fun HorizontalAlign.toComposeAlignment(): Alignment.Horizontal {
    return when(this) {
        HorizontalAlign.CENTER -> Alignment.CenterHorizontally
        HorizontalAlign.START -> Alignment.Start
        HorizontalAlign.END -> Alignment.End
        HorizontalAlign.LEFT -> AbsoluteAlignment.Left
        HorizontalAlign.RIGHT -> AbsoluteAlignment.Right
    }
}

internal fun Align.toComposeAlignment(): Alignment {
    return when(this) {
        Align.CENTER -> Alignment.Center
        Align.CENTER_START -> Alignment.CenterStart
        Align.CENTER_END -> Alignment.CenterEnd
        Align.TOP_START -> Alignment.TopStart
        Align.TOP_CENTER -> Alignment.TopCenter
        Align.TOP_END -> Alignment.TopEnd
        Align.BOTTOM_START -> Alignment.BottomStart
        Align.BOTTOM_CENTER -> Alignment.BottomCenter
        Align.BOTTOM_END -> Alignment.BottomEnd
        Align.CENTER_LEFT -> AbsoluteAlignment.CenterLeft
        Align.CENTER_RIGHT -> AbsoluteAlignment.CenterRight
        Align.TOP_LEFT -> AbsoluteAlignment.TopLeft
        Align.TOP_RIGHT -> AbsoluteAlignment.TopRight
        Align.BOTTOM_LEFT -> AbsoluteAlignment.BottomLeft
        Align.BOTTOM_RIGHT -> AbsoluteAlignment.BottomRight
    }
}

internal fun Align.toComposeVerticalAlignment(): Alignment.Vertical {
    return when (this) {
        Align.TOP_START, Align.TOP_CENTER, Align.TOP_END, Align.TOP_LEFT, Align.TOP_RIGHT -> Alignment.Top
        Align.CENTER_START, Align.CENTER, Align.CENTER_END, Align.CENTER_LEFT, Align.CENTER_RIGHT -> Alignment.CenterVertically
        Align.BOTTOM_START, Align.BOTTOM_CENTER, Align.BOTTOM_END, Align.BOTTOM_LEFT, Align.BOTTOM_RIGHT -> Alignment.Bottom
    }
}

internal fun Align.toComposeHorizontalAlignment(): Alignment.Horizontal {
    return when (this) {
        Align.TOP_START, Align.CENTER_START, Align.BOTTOM_START -> Alignment.Start
        Align.TOP_CENTER, Align.CENTER, Align.BOTTOM_CENTER -> Alignment.CenterHorizontally
        Align.TOP_END, Align.CENTER_END, Align.BOTTOM_END -> Alignment.End
        Align.TOP_LEFT, Align.CENTER_LEFT, Align.BOTTOM_LEFT -> AbsoluteAlignment.Left
        Align.TOP_RIGHT, Align.CENTER_RIGHT, Align.BOTTOM_RIGHT -> AbsoluteAlignment.Right
    }
}

internal fun Alignment.toVerticalAlignmentOrCenter(): Alignment.Vertical = when (this) {
    Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd,
    AbsoluteAlignment.TopLeft, AbsoluteAlignment.TopRight -> Alignment.Top
    Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd,
    AbsoluteAlignment.BottomLeft, AbsoluteAlignment.BottomRight -> Alignment.Bottom
    else -> Alignment.CenterVertically
}

internal fun Alignment.toHorizontalAlignmentOrCenter(): Alignment.Horizontal = when (this) {
    Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> Alignment.Start
    Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd -> Alignment.End
    AbsoluteAlignment.TopLeft, AbsoluteAlignment.CenterLeft, AbsoluteAlignment.BottomLeft -> AbsoluteAlignment.Left
    AbsoluteAlignment.TopRight, AbsoluteAlignment.CenterRight, AbsoluteAlignment.BottomRight -> AbsoluteAlignment.Right
    else -> Alignment.CenterHorizontally
}

internal val LocalContentAlignment = compositionLocalOf { Alignment.Center }
