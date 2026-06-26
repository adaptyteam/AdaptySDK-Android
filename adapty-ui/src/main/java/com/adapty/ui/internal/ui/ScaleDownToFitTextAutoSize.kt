package com.adapty.ui.internal.ui

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal data class OverflowResult(
    val hasVisualOverflow: Boolean,
    val widthOverflow: Boolean,
    val heightOverflow: Boolean,
) {
    val didOverflow: Boolean get() = hasVisualOverflow || widthOverflow || heightOverflow
}

internal fun TextAutoSize.Companion.ScaleDownToFit(
    minFontSize: TextUnit,
    maxFontSize: TextUnit,
    stepSize: TextUnit = 0.5.sp,
    onOverflowAtMin: ((OverflowResult) -> Unit)? = null,
): ScaleDownToFitTextAutoSize =
    ScaleDownToFitTextAutoSize(minFontSize, maxFontSize, stepSize, onOverflowAtMin)

internal class ScaleDownToFitTextAutoSize(
    val minFontSize: TextUnit,
    val maxFontSize: TextUnit,
    private val stepSize: TextUnit,
    private val onOverflowAtMin: ((OverflowResult) -> Unit)?,
) : TextAutoSize {

    private var lastChosenK: Int? = null

    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString
    ): TextUnit {
        val stepPx = stepSize.toPx()
        val minPxRaw = minFontSize.toPx()
        val maxPxRaw = maxFontSize.toPx()

        if (!stepPx.isFinite() || stepPx <= 0f) return minPxRaw.toSp()

        val minPx = max(0f, minPxRaw)
        val maxPx = max(0f, maxPxRaw)
        if (maxPx <= minPx) {
            lastChosenK = 0
            return minPx.toSp()
        }

        val kMax = floor((maxPx - minPx) / stepPx).toInt().coerceAtLeast(0)
        fun pxForK(k: Int): Float = minPx + k * stepPx

        val unboundedHeightConstraints = constraints.copy(maxHeight = Constraints.Infinity)

        fun checkOverflowAt(sizePx: Float): OverflowResult {
            val lr = performLayout(unboundedHeightConstraints, text, sizePx.toSp())

            val widthOverflow = constraints.hasBoundedWidth && lr.size.width > constraints.maxWidth
            val heightOverflow = constraints.hasBoundedHeight && lr.size.height > constraints.maxHeight

            val hasVisualOverflow = when (lr.layoutInput.overflow) {
                TextOverflow.Clip, TextOverflow.Visible ->
                    lr.hasVisualOverflow
                TextOverflow.StartEllipsis,
                TextOverflow.MiddleEllipsis,
                TextOverflow.Ellipsis -> {
                    var ellipsized = false
                    for (i in 0 until lr.lineCount) {
                        if (lr.isLineEllipsized(i)) {
                            ellipsized = true
                            break
                        }
                    }
                    ellipsized || lr.hasVisualOverflow
                }
                else -> error("TextOverflow ${lr.layoutInput.overflow} is not supported.")
            }

            return OverflowResult(hasVisualOverflow, widthOverflow, heightOverflow)
        }

        val kUpper = lastChosenK?.let { min(it, kMax) } ?: kMax

        val minOverflow = checkOverflowAt(pxForK(0))
        if (minOverflow.didOverflow) {
            lastChosenK = 0
            onOverflowAtMin?.invoke(minOverflow)
            return minFontSize
        }

        val topPx = pxForK(kUpper)
        if (!checkOverflowAt(topPx).didOverflow) {
            lastChosenK = kUpper
            return topPx.toSp()
        }

        var lo = 0
        var hi = (kUpper - 1).coerceAtLeast(0)
        var best = 0

        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (checkOverflowAt(pxForK(mid)).didOverflow) {
                hi = mid - 1
            } else {
                best = mid
                lo = mid + 1
            }
        }

        lastChosenK = best
        return pxForK(best).toSp()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ScaleDownToFitTextAutoSize) return false
        return other.minFontSize == minFontSize &&
                other.maxFontSize == maxFontSize &&
                other.stepSize == stepSize
    }

    override fun hashCode(): Int {
        var result = minFontSize.hashCode()
        result = 31 * result + maxFontSize.hashCode()
        result = 31 * result + stepSize.hashCode()
        return result
    }
}
