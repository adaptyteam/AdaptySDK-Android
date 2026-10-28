@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.script.toJsInt
import com.adapty.ui.internal.ui.attributes.Interpolator
import com.adapty.ui.internal.ui.attributes.toEasing
import com.adapty.ui.internal.ui.ABSENT
import com.adapty.ui.internal.ui.LocalSectionPresenceSlot
import com.adapty.ui.internal.ui.PRESENT
import com.adapty.ui.internal.ui.SectionPresenceSlot
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.utils.OneWayBinding
import kotlin.math.roundToInt

@InternalAdaptyApi
public class SectionElement internal constructor(
    internal val index: OneWayBinding,
    override var content: List<UIElement>,
    internal val animationDurationMillis: Int? = null,
    internal val animationInterpolator: Interpolator = Interpolator.Named("ease_in_out"),
    override val baseProps: BaseProps = BaseProps.EMPTY,
): UIElement, MultiContainer {

    override val layoutRelevantProps: BaseProps get() = content.firstOrNull()?.layoutRelevantProps ?: BaseProps.EMPTY

    @Composable
    override fun layoutRelevantPropsResolved(): BaseProps =
        layoutSlotOrNull()?.layoutRelevantPropsResolved() ?: BaseProps.EMPTY

    @Composable
    internal fun layoutSlotOrNull(): UIElement? {
        activeSlotOrNull()?.let { return it }
        val slot = LocalSectionPresenceSlot.current ?: return null
        if (!isAnimated || !slot.leaving) return null
        return content.getOrNull(slot.lastActive)
    }

    internal val isAnimated: Boolean get() = (animationDurationMillis ?: 0) > 0

    override fun anyLayoutVariant(predicate: (UIElement) -> Boolean): Boolean =
        content.any { it.anyLayoutVariant(predicate) }

    @Composable
    internal fun activeSlotOrNull(): UIElement? {
        val state = resolveState()
        val currentIndex = state[index].toJsInt() ?: 0
        return content.getOrNull(currentIndex)
    }

    internal companion object {
        fun getKey(sectionId: String) = "section_$sectionId"
    }

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit {
        return {
            val state = resolveState()
            val currentIndex = state[index].toJsInt() ?: 0
            val duration = animationDurationMillis
            val presenceSlot = LocalSectionPresenceSlot.current
            if (duration == null || duration <= 0) {
                val active = currentIndex in content.indices
                presenceSlot?.let { slot -> SideEffect { slot.presence = if (active) PRESENT else ABSENT } }
                if (active) {
                    CompositionLocalProvider(LocalSectionPresenceSlot provides null) {
                        Box(modifier = modifier) { renderStaticSlot(currentIndex, dispatch) }
                    }
                } else if (presenceSlot != null) {
                    AbsentStackChild()
                }
            } else {
                val transition = updateTransition(targetState = currentIndex, label = "SectionTransition")
                val settledHidden = transition.currentState == transition.targetState &&
                    currentIndex !in content.indices
                val presence = transition.animateFloat(
                    transitionSpec = { tween(durationMillis = duration, easing = animationInterpolator.toEasing()) },
                    label = "SectionPresence",
                ) { index -> if (index in content.indices) 1f else 0f }
                presenceSlot?.let { slot -> SideEffect { if (slot.presence !== presence) slot.presence = presence } }
                if (currentIndex in content.indices) presenceSlot?.lastActive = currentIndex
                if (!settledHidden) {
                    CompositionLocalProvider(LocalSectionPresenceSlot provides null) {
                        Box(modifier = modifier) {
                            renderAnimatedSlot(transition, presence, duration, presenceSlot, dispatch)
                        }
                    }
                } else if (presenceSlot != null) {
                    AbsentStackChild()
                }
            }
        }
    }

    @Composable
    private fun renderStaticSlot(currentIndex: Int, dispatch: (Message) -> Unit) {
        val slot = content[currentIndex]
        val slotKey: Any = if (slot.baseProps.eventHandlers.isNullOrEmpty()) slot::class else currentIndex
        key(slotKey) { slot.run { render(dispatch) } }
    }

    private class LastActiveSlot(var index: Int)

    @Composable
    private fun renderAnimatedSlot(
        transition: Transition<Int>,
        presence: State<Float>,
        duration: Int,
        presenceSlot: SectionPresenceSlot?,
        dispatch: (Message) -> Unit,
    ) {
        val mainAxisVertical = presenceSlot?.vertical
        val easing = animationInterpolator.toEasing()
        val currentIndex = transition.targetState
        val lastActive = remember { LastActiveSlot(if (currentIndex in content.indices) currentIndex else 0) }
        if (currentIndex in content.indices) lastActive.index = currentIndex
        transition.AnimatedContent(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val fraction = presence.value
                    val fill = presenceSlot?.fillsMainAxis == true
                    if (fill && mainAxisVertical == true && constraints.hasBoundedHeight) {
                        val h = (constraints.maxHeight * fraction).roundToInt()
                        val placeable = measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
                        return@layout layout(placeable.width, h) { placeable.place(0, 0) }
                    }
                    if (fill && mainAxisVertical == false && constraints.hasBoundedWidth) {
                        val w = (constraints.maxWidth * fraction).roundToInt()
                        val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
                        return@layout layout(w, placeable.height) { placeable.place(0, 0) }
                    }
                    val placeable = measurable.measure(constraints)
                    layout(
                        if (mainAxisVertical == true) placeable.width else (placeable.width * fraction).roundToInt(),
                        if (mainAxisVertical == false) placeable.height else (placeable.height * fraction).roundToInt(),
                    ) { placeable.place(0, 0) }
                }
                .graphicsLayer { alpha = presence.value },
            transitionSpec = {
                val spec = tween<Float>(durationMillis = duration, easing = easing)
                (fadeIn(animationSpec = spec) togetherWith fadeOut(animationSpec = spec))
                    .using(SizeTransform(clip = false) { _, _ ->
                        tween(durationMillis = duration, easing = easing)
                    })
            },
            contentKey = { index -> if (index in content.indices) index else lastActive.index },
        ) { animatedIndex ->
            (content.getOrNull(animatedIndex) ?: content[lastActive.index]).run { render(dispatch) }
        }
    }
}

@Composable
private fun AbsentStackChild() {
    Layout(content = {}, measurePolicy = { _, constraints -> layout(constraints.minWidth, constraints.minHeight) {} })
}
