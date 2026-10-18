@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.attributes.Condition
import com.adapty.ui.internal.ui.attributes.ConditionScope
import com.adapty.ui.internal.ui.attributes.Transition
import com.adapty.ui.internal.ui.attributes.evaluateAll
import com.adapty.ui.internal.utils.LocalScreenDimensions

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
            BoxWithConstraints(modifier = modifier) {
                val configuration = LocalConfiguration.current
                val screenDimensions = LocalScreenDimensions.current
                val screenWidthDp = screenDimensions.widthDp
                val screenHeightDp = screenDimensions.heightDp
                val scope = ConditionScope(
                    availableWidthDp = maxWidth.value.takeIf { it.isFinite() } ?: screenWidthDp,
                    availableHeightDp = maxHeight.value.takeIf { it.isFinite() } ?: screenHeightDp,
                    screenWidthDp = screenWidthDp,
                    screenHeightDp = screenHeightDp,
                    orientation = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                        Condition.Orientation.Landscape
                    else
                        Condition.Orientation.Portrait,
                )
                val selection = cases.indexOfFirst { it.condition.evaluateAll(scope) }
                if (transition == null) {
                    (cases.getOrNull(selection)?.content ?: default).render(dispatch)
                } else {
                    AdaptiveContentTransition(selection, transition) { index ->
                        (cases.getOrNull(index)?.content ?: default).render(dispatch)
                    }
                }
            }
        }
    }
}
