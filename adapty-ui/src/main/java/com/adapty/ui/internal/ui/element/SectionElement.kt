@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.script.toJsInt
import com.adapty.ui.internal.ui.attributes.Interpolator
import com.adapty.ui.internal.ui.attributes.toEasing
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.utils.OneWayBinding

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
        activeSlotOrNull()?.layoutRelevantPropsResolved() ?: BaseProps.EMPTY

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
            Box(modifier = modifier) {
                renderSection { currentIndex ->
                    content[currentIndex].run { render(dispatch) }
                }
            }
        }
    }

    @Composable
    private fun renderSection(
        renderChild: @Composable (currentIndex: Int) -> Unit,
    ) {
        val state = resolveState()
        val currentIndex = state[index].toJsInt() ?: 0
        if (currentIndex !in content.indices) return

        val duration = animationDurationMillis
        if (duration == null || duration <= 0) {
            val slot = content[currentIndex]
            val slotKey: Any = if (slot.baseProps.eventHandlers.isNullOrEmpty()) slot::class else currentIndex
            key(slotKey) { renderChild(currentIndex) }
        } else {
            val easing = animationInterpolator.toEasing()
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = duration, easing = easing)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = duration, easing = easing))
                },
                label = "SectionAnimatedContent",
            ) { animatedIndex ->
                renderChild(animatedIndex)
            }
        }
    }
}
