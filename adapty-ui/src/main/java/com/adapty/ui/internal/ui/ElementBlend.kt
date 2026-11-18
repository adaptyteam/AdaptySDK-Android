@file:OptIn(com.adapty.internal.utils.InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adapty.ui.internal.ui.attributes.BlendMode
import com.adapty.ui.internal.ui.attributes.Decorator

@Composable
internal fun Modifier.elementBlendOrSkip(
    blend: BlendMode,
    decorator: ResolvedDecorator?,
    blurRadius: Dp,
    overflow: OverflowExtent?,
): Modifier {
    if (blend == BlendMode.NORMAL || blend == BlendMode.DESTINATION_IN) return this
    val shadows = decorator?.shadowCaptureExpand ?: 0.dp
    val blurred = blurRadius > 0.dp
    val reach = if (blurred) blurRadius + shadows else shadows
    val overflows = !blurred && decorator?.decorator?.clip != Decorator.Clip.CONTENT
    return this.then(ElementBlendElement(blend, reach, overflow.takeIf { overflows }))
}

private data class ElementBlendElement(
    private val blend: BlendMode,
    private val reach: Dp,
    private val overflow: OverflowExtent?,
) : ModifierNodeElement<ElementBlendNode>() {
    override fun create() = ElementBlendNode(blend, reach, overflow)
    override fun update(node: ElementBlendNode) = node.update(blend, reach, overflow)
}

private class ElementBlendNode(
    private var blend: BlendMode,
    private var reach: Dp,
    private var overflow: OverflowExtent?,
) : Modifier.Node(), DrawModifierNode {

    private var paint: android.graphics.Paint? = blend.platformPaint()

    private val redraw: () -> Unit = { invalidateDraw() }

    override fun onAttach() {
        overflow?.addListener(redraw)
    }

    override fun onDetach() {
        overflow?.removeListener(redraw)
    }

    fun update(blend: BlendMode, reach: Dp, overflow: OverflowExtent?) {
        if (this.blend != blend) {
            this.blend = blend
            paint = blend.platformPaint()
        }
        this.reach = reach
        if (this.overflow !== overflow) {
            this.overflow?.removeListener(redraw)
            overflow?.addListener(redraw)
            this.overflow = overflow
        }
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val paint = paint ?: return drawContent()
        val canvas = drawContext.canvas.nativeCanvas
        val r = reach.toPx()
        val own = Rect(-r, -r, size.width + r, size.height + r)
        val bounds = overflow?.around(own) ?: own
        val count = canvas.saveLayer(bounds.left, bounds.top, bounds.right, bounds.bottom, paint)
        drawContent()
        canvas.restoreToCount(count)
    }
}
