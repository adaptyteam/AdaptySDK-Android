@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.buttonPressDim
import com.adapty.ui.internal.ui.rememberOpacityProvider
import androidx.compose.runtime.CompositionLocalProvider
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.ui.LocalOpacityProvider
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.utils.OneWayBinding

@InternalAdaptyApi
public class LegacyButtonElement internal constructor(
    internal val actions: List<Action>,
    internal val normal: UIElement,
    internal val selected: UIElement?,
    internal val isSelected: OneWayBinding?,
    override val baseProps: BaseProps,
) : UIElement {

    override val layoutRelevantProps: BaseProps get() = normal.baseProps

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val state = resolveState()
        val item = when {
            selected == null || isSelected == null -> normal
            else -> {
                val isSelected = state[isSelected] as? Boolean ?: false
                if (isSelected) selected else normal
            }
        }
        val opacityProvider = rememberOpacityProvider(baseProps)
        val screen = LocalScreenInstance.current
        val interactionSource = remember { MutableInteractionSource() }
        CompositionLocalProvider(LocalOpacityProvider provides opacityProvider) {
            Box(
                Modifier
                    .buttonPressDim(interactionSource)
                    .then(modifier)
            ) {
                CompositionLocalProvider(LocalOpacityProvider provides null) {
                    item.render(dispatch)
                }
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(
                            indication = null,
                            interactionSource = interactionSource,
                            enabled = opacityProvider.alpha.value > 0f,
                        ) {
                            dispatch(Message.ActionsRequested(actions, screen))
                        }
                )
            }
        }
    }
}
