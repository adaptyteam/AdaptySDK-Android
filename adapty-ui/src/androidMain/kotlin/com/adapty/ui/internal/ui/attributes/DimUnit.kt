@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LocalScreenDimensions
import com.adapty.ui.internal.utils.SafeAreaInsets
import com.adapty.ui.internal.utils.ScreenDimensions
import com.adapty.ui.internal.utils.getInsets

@Composable
internal fun DimUnit.toExactDp(axis: DimSpec.Axis): Dp {
    when (this) {
        is DimUnit.Exact -> return this.value.dp
        is DimUnit.SafeArea -> {
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val safeContent = getInsets()
            return with(density) {
                when (this@toExactDp.side) {
                    DimUnit.SafeArea.Side.START -> when (axis) {
                        DimSpec.Axis.X -> safeContent.getLeft(density, layoutDirection)
                        DimSpec.Axis.Y -> safeContent.getTop(density)
                    }
                    DimUnit.SafeArea.Side.END -> when (axis) {
                        DimSpec.Axis.X -> safeContent.getRight(density, layoutDirection)
                        DimSpec.Axis.Y -> safeContent.getBottom(density)
                    }
                }.toDp()
            }
        }
        is DimUnit.ScreenFraction -> {
            val screenDimensions = LocalScreenDimensions.current
            val screenSize =
                if (axis == DimSpec.Axis.Y) screenDimensions.heightDp else screenDimensions.widthDp

            return (this.fraction * screenSize).dp
        }
    }
}

@InternalAdaptyApi
public sealed class DimUnit {
    public data class Exact internal constructor(internal val value: Float): DimUnit()
    public data class ScreenFraction internal constructor(internal val fraction: Float): DimUnit()
    public data class SafeArea internal constructor(internal val side: Side): DimUnit() {
        internal enum class Side { START, END }
    }

    public companion object {
        public val Zero: DimUnit = Exact(0f)
    }
}

internal fun DimUnit.toExactDpInLayout(
    axis: DimSpec.Axis,
    safeAreaInsets: SafeAreaInsets,
    screenDimensions: ScreenDimensions,
    layoutDirection: LayoutDirection,
): Dp {
    when (this) {
        is DimUnit.Exact -> return this.value.dp
        is DimUnit.SafeArea -> {
            return when (this.side) {
                DimUnit.SafeArea.Side.START -> when (axis) {
                    DimSpec.Axis.X -> if (layoutDirection == LayoutDirection.Rtl) safeAreaInsets.end else safeAreaInsets.start
                    DimSpec.Axis.Y -> safeAreaInsets.top
                }
                DimUnit.SafeArea.Side.END -> when (axis) {
                    DimSpec.Axis.X -> if (layoutDirection == LayoutDirection.Rtl) safeAreaInsets.start else safeAreaInsets.end
                    DimSpec.Axis.Y -> safeAreaInsets.bottom
                }
            }
        }
        is DimUnit.ScreenFraction -> {
            val screenSize = if (axis == DimSpec.Axis.Y) screenDimensions.heightDp else screenDimensions.widthDp
            return (this.fraction * screenSize).dp
        }
    }
}