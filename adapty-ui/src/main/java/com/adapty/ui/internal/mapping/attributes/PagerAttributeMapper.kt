@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.attributes

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.ui.attributes.EdgeEntities
import com.adapty.ui.internal.ui.attributes.InteractionBehavior
import com.adapty.ui.internal.ui.attributes.PageSize
import com.adapty.ui.internal.ui.attributes.PagerAnimation
import com.adapty.ui.internal.ui.attributes.PagerIndicator
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.Transition
import com.adapty.ui.internal.ui.attributes.VerticalAlign

internal class PagerAttributeMapper(
    private val commonAttributeMapper: CommonAttributeMapper,
) {

    fun mapPagerAnimation(item: Map<*, *>): PagerAnimation {
        val startDelay = (item["start_delay"] as? Number)?.toLong() ?: 0L
        val afterInteractionDelay = (item["after_interaction_delay"] as? Number)?.toLong() ?: 3000L
        val pageTransition =
            ((item["page_transition"] as? Map<*, *>)?.let(commonAttributeMapper::mapTransition) as? Transition.Slide)
                ?: throw adaptyError(
                    message = "page_transition is invalid (${item["page_transition"]})",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
        val repeatTransition =
            (item["repeat_transition"] as? Map<*, *>)?.let(commonAttributeMapper::mapTransition) as? Transition.Slide

        return PagerAnimation(startDelay, afterInteractionDelay, pageTransition, repeatTransition)
    }

    fun mapPagerIndicator(item: Map<*, *>): PagerIndicator {
        val layout = (item["layout"] as? String).let { layout ->
            when (layout) {
                "overlaid" -> PagerIndicator.Layout.OVERLAID
                else -> PagerIndicator.Layout.STACKED
            }
        }
        val vAlign = commonAttributeMapper.mapVerticalAlign(item["v_align"], default = VerticalAlign.BOTTOM)
        val dotSize = (item["dot_size"] as? Number)?.toFloat() ?: 6f
        val spacing = (item["spacing"] as? Number)?.toFloat() ?: 6f
        val padding = item["padding"]?.let(commonAttributeMapper::mapEdgeEntities) ?: EdgeEntities(6f)
        val color = (item["color"] as? String)?.let { assetId -> Shape.Fill(assetId) }
        val selectedColor = (item["selected_color"] as? String)?.let { assetId -> Shape.Fill(assetId) }

        return PagerIndicator(layout, vAlign, padding, dotSize, spacing, color, selectedColor)
    }

    fun mapInteractionBehavior(item: Any?): InteractionBehavior {
        return when(item) {
            "none" -> InteractionBehavior.NONE
            "cancel_animation" -> InteractionBehavior.CANCEL_ANIMATION
            else -> InteractionBehavior.PAUSE_ANIMATION
        }
    }

    fun mapPageSize(item: Any?): PageSize {
        when (item) {
            is Map<*, *> -> {
                val parent = item["parent"] as? Number
                if (parent != null)
                    return PageSize.PageFraction(parent.toFloat())
                return PageSize.Unit(commonAttributeMapper.mapDimUnit(item))
            }
            else -> return item?.let(commonAttributeMapper::mapDimUnit)?.let { PageSize.Unit(it) }
                ?: PageSize.PageFraction(1f)
        }
    }
}