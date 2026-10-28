@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.AdaptiveRootBox
import com.adapty.ui.internal.ui.attributes.Condition
import com.adapty.ui.internal.ui.attributes.Transition
import com.adapty.ui.internal.ui.attributes.evaluateAll

@InternalAdaptyApi
public class SwitchElement internal constructor(
    internal val cases: List<Case>,
    internal val default: UIElement,
    internal val transition: Transition?,
    override val baseProps: BaseProps,
) : UIElement {

    internal class Case(
        val condition: List<Condition>,
        val content: UIElement,
    )

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        if (cases.isEmpty()) {
            Box(modifier = modifier) { default.render(dispatch) }
        } else {
            AdaptiveRootBox(modifier = modifier) { scope ->
                val selection = cases.indexOfFirst { it.condition.evaluateAll(scope) }
                AdaptiveVariant(selection, transition) { index ->
                    (cases.getOrNull(index)?.content ?: default).render(dispatch)
                }
            }
        }
    }
}
