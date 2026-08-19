@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import com.adapty.internal.utils.InternalAdaptyApi

internal sealed class Condition {

    data class SizeThreshold(
        val metric: Metric,
        val min: Float?,
        val max: Float?,
    ) : Condition()

    data class OrientationIs(
        val orientation: Orientation,
    ) : Condition()

    internal enum class Metric { AvailableWidth, AvailableHeight, ScreenWidth, ScreenHeight }

    internal enum class Orientation { Landscape, Portrait }
}

internal data class ConditionScope(
    val availableWidthDp: Float,
    val availableHeightDp: Float,
    val screenWidthDp: Float,
    val screenHeightDp: Float,
    val orientation: Condition.Orientation,
)

internal fun Condition.evaluate(scope: ConditionScope): Boolean =
    when (this) {
        is Condition.SizeThreshold -> {
            val value = when (metric) {
                Condition.Metric.AvailableWidth -> scope.availableWidthDp
                Condition.Metric.AvailableHeight -> scope.availableHeightDp
                Condition.Metric.ScreenWidth -> scope.screenWidthDp
                Condition.Metric.ScreenHeight -> scope.screenHeightDp
            }
            (min == null || value >= min) && (max == null || value <= max)
        }
        is Condition.OrientationIs -> scope.orientation == orientation
    }

internal fun List<Condition>.evaluateAll(scope: ConditionScope): Boolean =
    all { it.evaluate(scope) }
