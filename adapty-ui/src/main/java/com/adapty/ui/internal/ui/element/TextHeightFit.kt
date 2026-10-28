package com.adapty.ui.internal.ui.element

import androidx.compose.ui.unit.Constraints

internal class HeightFitBudget(val cap: Int) {
    var maxLines: Int = cap
        private set

    private var idealBottoms: FloatArray? = null
    private var capturedWidth: Int = -1

    fun onLayout(proposedWidth: Int, lineBottoms: () -> FloatArray) {
        if (maxLines == cap) {
            idealBottoms = lineBottoms()
            capturedWidth = proposedWidth
        } else if (proposedWidth != capturedWidth) {
            idealBottoms = null
            maxLines = cap
        }
    }

    fun fit(maxHeight: Int) {
        val bottoms = idealBottoms?.takeIf { it.isNotEmpty() } ?: return
        maxLines = if (maxHeight == Constraints.Infinity) {
            cap
        } else {
            var fit = 0
            for (i in bottoms.indices) {
                if (bottoms[i] <= maxHeight + 0.5f) fit = i + 1 else break
            }
            fit.coerceAtLeast(1).coerceAtMost(cap)
        }
    }
}
