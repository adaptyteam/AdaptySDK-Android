@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.buttonPressDim
import com.adapty.ui.internal.ui.rememberOpacityProvider
import androidx.compose.runtime.CompositionLocalProvider
import com.adapty.ui.internal.ui.LocalOpacityProvider
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.LocalUiEnabled
import com.adapty.ui.internal.utils.DISABLED_ALPHA

@InternalAdaptyApi
public class ButtonElement internal constructor(
    internal val actions: List<Action>,
    internal val content: UIElement,
    override val baseProps: BaseProps,
) : UIElement {

    override val layoutRelevantProps: BaseProps get() = when (val c = content) {
        is SectionElement -> c.content.firstOrNull()?.layoutRelevantProps ?: BaseProps.EMPTY
        else -> c.layoutRelevantProps
    }

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val opacityProvider = rememberOpacityProvider(baseProps)
        val screen = LocalScreenInstance.current
        val interactionSource = remember { MutableInteractionSource() }
        val enabled = LocalUiEnabled.current
        CompositionLocalProvider(LocalOpacityProvider provides opacityProvider) {
            val baseModifier = if (enabled) modifier else modifier.alpha(DISABLED_ALPHA)
            Box(
                Modifier
                    .buttonPressDim(interactionSource)
                    .then(baseModifier)
                    .clickable(
                        indication = null,
                        interactionSource = interactionSource,
                        enabled = opacityProvider.alpha.value > 0f && enabled,
                    ) {
                        dispatch(Message.ActionsRequested(actions, screen))
                    }
            ) {
                CompositionLocalProvider(LocalOpacityProvider provides null) {
                    content.render(dispatch)
                }
            }
        }
    }
}
