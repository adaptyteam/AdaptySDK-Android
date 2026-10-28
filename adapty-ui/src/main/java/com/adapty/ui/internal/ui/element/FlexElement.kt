@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.AdaptiveRootBox
import com.adapty.ui.internal.ui.attributes.Condition
import com.adapty.ui.internal.ui.attributes.Transition
import com.adapty.ui.internal.ui.attributes.evaluateAll

@InternalAdaptyApi
public class FlexElement internal constructor(
    internal val condition: List<Condition>,
    internal val direction: Direction,
    internal val rowElement: RowElement,
    internal val columnElement: ColumnElement,
    internal val transition: Transition?,
    override val baseProps: BaseProps,
) : UIElement {

    internal enum class Direction {
        HORIZONTAL, VERTICAL;

        val opposite: Direction get() = if (this == HORIZONTAL) VERTICAL else HORIZONTAL
    }

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        if (condition.isEmpty()) {
            RenderDirected(direction, dispatch, modifier)
        } else {
            AdaptiveRootBox(modifier = modifier) { scope ->
                val active = if (condition.evaluateAll(scope)) direction else direction.opposite
                AdaptiveVariant(active, transition) { dir ->
                    RenderDirected(dir, dispatch, Modifier)
                }
            }
        }
    }

    @Composable
    private fun RenderDirected(
        direction: Direction,
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ) {
        when (direction) {
            Direction.HORIZONTAL -> rowElement.toComposable(dispatch, modifier).invoke()
            Direction.VERTICAL -> columnElement.toComposable(dispatch, modifier).invoke()
        }
    }
}
