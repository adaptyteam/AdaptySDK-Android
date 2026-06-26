@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.attributes

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.EdgeEntities
import com.adapty.ui.internal.ui.attributes.InteractionBehavior
import com.adapty.ui.internal.ui.attributes.Interpolator
import com.adapty.ui.internal.ui.attributes.PageSize
import com.adapty.ui.internal.ui.attributes.PagerAnimation
import com.adapty.ui.internal.ui.attributes.PagerIndicator
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.Transition
import com.adapty.ui.internal.ui.attributes.VerticalAlign
import com.adapty.ui.internal.utils.StringSource
import com.adapty.ui.internal.utils.VisualValue

internal fun Map<*, *>.toPagerAnimation(): PagerAnimation {
    val startDelay = (this["start_delay"] as? Number)?.toLong() ?: 0L
    val afterInteractionDelay = (this["after_interaction_delay"] as? Number)?.toLong() ?: 3000L
    val pageTransition =
        (this["page_transition"] as? Map<*, *>)?.toTransition()
            ?: Transition(300f, 0f, Interpolator.Named("ease_in_out"))
    val repeatTransition =
        (this["repeat_transition"] as? Map<*, *>)?.toTransition()

    return PagerAnimation(startDelay, afterInteractionDelay, pageTransition, repeatTransition)
}

internal fun Map<*, *>.toPagerIndicator(): PagerIndicator {
    val layout = (this["layout"] as? String).let { layout ->
        when (layout) {
            "overlaid" -> PagerIndicator.Layout.OVERLAID
            else -> PagerIndicator.Layout.STACKED
        }
    }
    val vAlign = this["v_align"].toVerticalAlign(default = VerticalAlign.BOTTOM)
    val dotSize = (this["dot_size"] as? Number)?.toFloat() ?: 6f
    val spacing = (this["spacing"] as? Number)?.toFloat() ?: 6f
    val padding = this["padding"]?.toEdgeEntities() ?: EdgeEntities(6f)
    val color = this["color"]?.toVisualValue()
        ?: VisualValue.any(StringSource.Value("#FFFFFFFF"))
    val selectedColor = this["selected_color"]?.toVisualValue()
        ?: VisualValue.any(StringSource.Value("#D3D3D3FF"))

    return PagerIndicator(layout, vAlign, padding, dotSize, spacing, color, selectedColor)
}

internal fun Any?.toInteractionBehavior(): InteractionBehavior {
    return when(this) {
        "none" -> InteractionBehavior.NONE
        "cancel_animation" -> InteractionBehavior.CANCEL_ANIMATION
        else -> InteractionBehavior.PAUSE_ANIMATION
    }
}

internal fun Any?.toPageSize(): PageSize {
    when (this) {
        is Map<*, *> -> {
            val parent = this["parent"] as? Number
            if (parent != null)
                return PageSize.PageFraction(parent.toFloat())
            return PageSize.Unit(this.toDimUnit())
        }
        else -> return this?.toDimUnit()?.let { PageSize.Unit(it) }
            ?: PageSize.PageFraction(1f)
    }
}
