package com.adapty.ui.internal.ui.element

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

// Outlines of the Skia drawRadialProgress actual: 100 px circle at (10, 10), center (60, 60)
class RadialProgressDrawingTest {

    private val topLeft = Offset(10f, 10f)

    @Test
    fun strokedArcButtCapsCoversRightHalfRing() {
        val bounds = strokedArcOutline(topLeft, 100f, 20f, roundCaps = false, startAngle = -90f, sweepAngle = 180f).getBounds()
        assertRect(Rect(60f, 0f, 120f, 120f), bounds)
    }

    @Test
    fun strokedArcRoundCapsExtendByHalfStroke() {
        val bounds = strokedArcOutline(topLeft, 100f, 20f, roundCaps = true, startAngle = -90f, sweepAngle = 180f).getBounds()
        assertRect(Rect(50f, 0f, 120f, 120f), bounds)
    }

    @Test
    fun strokedArcCounterClockwiseCoversLeftHalfRing() {
        val bounds = strokedArcOutline(topLeft, 100f, 20f, roundCaps = false, startAngle = -90f, sweepAngle = -180f).getBounds()
        assertRect(Rect(0f, 0f, 60f, 120f), bounds)
    }

    @Test
    fun strokedFullArcIsRing() {
        val bounds = strokedArcOutline(topLeft, 100f, 20f, roundCaps = false, startAngle = 0f, sweepAngle = 360f).getBounds()
        assertRect(Rect(0f, 0f, 120f, 120f), bounds)
    }

    @Test
    fun pieQuarterStartsAtCenter() {
        val bounds = pieOutline(topLeft, 100f, startAngle = -90f, sweepAngle = 90f).getBounds()
        assertRect(Rect(60f, 10f, 110f, 60f), bounds)
    }

    private fun assertRect(expected: Rect, actual: Rect) {
        val ok = listOf(
            expected.left to actual.left,
            expected.top to actual.top,
            expected.right to actual.right,
            expected.bottom to actual.bottom,
        ).all { (e, a) -> abs(e - a) < 0.5f }
        assertTrue(ok, "expected $expected, got $actual")
    }
}
