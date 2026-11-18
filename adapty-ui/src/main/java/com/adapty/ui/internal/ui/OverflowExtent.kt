package com.adapty.ui.internal.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor

internal class OverflowExtent {

    private val reports = LinkedHashMap<OverflowExtentNode, Rect?>()
    private val listeners = ArrayList<() -> Unit>()

    private var coordinates: LayoutCoordinates? = null

    internal var onChange: (() -> Unit)? = null

    var active = false
        private set

    var union: Rect? = null
        private set

    fun around(bounds: Rect): Rect = (union?.let { bounds.union(it) } ?: bounds).inflate(slack)

    private var slack = 0f

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    internal fun setActive(active: Boolean) {
        if (this.active == active) return
        this.active = active
        for (member in reports.keys.toList()) member.parentActive(active)
        if (!active) {
            reports.replaceAll { _, _ -> null }
            recompute()
        }
    }

    internal fun join(member: OverflowExtentNode) {
        if (member in reports) return
        reports[member] = null
        if (active) member.parentActive(true)
    }

    internal fun leave(member: OverflowExtentNode) {
        if (member !in reports) return
        reports.remove(member)
        recompute()
    }

    internal fun ownerPlaced(coordinates: LayoutCoordinates, density: Float) {
        this.coordinates = coordinates
        slack = GLYPH_SLACK.value * density
        if (!active) return
        for (member in reports.keys) reports[member] = member.extentIn(coordinates)
        recompute()
    }

    internal fun report(member: OverflowExtentNode) {
        if (!active || member !in reports) return
        reports[member] = coordinates?.let { member.extentIn(it) }
        recompute()
    }

    private fun recompute() {
        var u: Rect? = null
        for (rect in reports.values) if (rect != null) u = u?.union(rect) ?: rect
        if (u == union) return
        union = u
        listeners.forEach { it() }
        onChange?.invoke()
    }
}

private val ModifierLocalOverflowExtent = modifierLocalOf<OverflowExtent?> { null }

internal fun Modifier.overflowExtent(extent: OverflowExtent?, tracks: Boolean, reach: Dp): Modifier =
    this.then(OverflowExtentElement(extent, tracks && extent != null, reach))

private data class OverflowExtentElement(
    private val extent: OverflowExtent?,
    private val tracks: Boolean,
    private val reach: Dp,
) : ModifierNodeElement<OverflowExtentNode>() {
    override fun create() = OverflowExtentNode(extent, tracks, reach)
    override fun update(node: OverflowExtentNode) = node.update(extent, tracks, reach)
}

internal class OverflowExtentNode(
    private var extent: OverflowExtent?,
    private var tracks: Boolean,
    private var reach: Dp,
) : DelegatingNode(), ModifierLocalModifierNode {

    override val providedValues = modifierLocalMapOf(ModifierLocalOverflowExtent to extent)

    private var parent: OverflowExtent? = null
    private var parentActive = false
    private var coordinates: LayoutCoordinates? = null

    private val positions = object : Modifier.Node(), GlobalPositionAwareModifierNode {
        override fun onGloballyPositioned(coordinates: LayoutCoordinates) = placed(coordinates)
    }
    private var followsPositions = false

    override fun onAttach() {
        extent?.onChange = { parent?.report(this) }
        val parent = ModifierLocalOverflowExtent.current
        this.parent = parent
        parentActive = false
        parent?.join(this)
        refresh()
    }

    override fun onDetach() {
        parent?.leave(this)
        parent = null
        parentActive = false
        extent?.onChange = null
        extent?.setActive(false)
        coordinates = null
        followsPositions = false
    }

    fun update(extent: OverflowExtent?, tracks: Boolean, reach: Dp) {
        if (this.extent !== extent) {
            this.extent?.onChange = null
            this.extent?.setActive(false)
            this.extent = extent
            extent?.onChange = { parent?.report(this) }
            provide(ModifierLocalOverflowExtent, extent)
        }
        this.tracks = tracks
        this.reach = reach
        refresh()
        coordinates?.let { placed(it) }
    }

    internal fun parentActive(active: Boolean) {
        parentActive = active
        if (isAttached) refresh()
    }

    private fun refresh() {
        extent?.setActive(tracks || parentActive)
        val follows = parentActive || extent?.active == true
        if (follows != followsPositions) {
            followsPositions = follows
            if (follows) delegate(positions) else undelegate(positions)
        }
    }

    private fun placed(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        extent?.ownerPlaced(coordinates, requireDensity().density)
        if (parentActive) parent?.report(this)
    }

    fun extentIn(target: LayoutCoordinates): Rect? {
        val own = coordinates?.takeIf { it.isAttached && target.isAttached } ?: return null
        val r = reach.value * requireDensity().density
        val size = own.size
        val local = Rect(-r, -r, size.width + r, size.height + r).let { b -> extent?.around(b) ?: b }
        val a = target.localPositionOf(own, local.topLeft)
        val b = target.localPositionOf(own, local.topRight)
        val c = target.localPositionOf(own, local.bottomLeft)
        val d = target.localPositionOf(own, local.bottomRight)
        return Rect(
            floor(minOf(a.x, b.x, c.x, d.x)), floor(minOf(a.y, b.y, c.y, d.y)),
            ceil(maxOf(a.x, b.x, c.x, d.x)), ceil(maxOf(a.y, b.y, c.y, d.y)),
        )
    }
}

private val GLYPH_SLACK = 2.dp

private fun Rect.union(other: Rect) =
    Rect(minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom))
