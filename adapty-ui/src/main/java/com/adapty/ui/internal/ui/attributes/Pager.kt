@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.VisualValue

internal class PagerAnimation(
    internal val startDelayMillis: Long,
    internal val afterInteractionDelayMillis: Long,
    internal val pageTransition: Transition,
    internal val repeatTransition: Transition?,
)

internal enum class InteractionBehavior {
    NONE, CANCEL_ANIMATION, PAUSE_ANIMATION
}

internal class PagerIndicator(
    val layout: Layout,
    val vAlign: VerticalAlign,
    val padding: EdgeEntities,
    val dotSize: Float,
    val spacing: Float,
    val color: VisualValue?,
    val selectedColor: VisualValue?,
) {
    enum class Layout {
        STACKED, OVERLAID
    }
}

@InternalAdaptyApi
public sealed class PageSize {
    public class Unit internal constructor(internal val value: DimUnit): PageSize()
    public class PageFraction internal constructor(internal val fraction: Float): PageSize()
}