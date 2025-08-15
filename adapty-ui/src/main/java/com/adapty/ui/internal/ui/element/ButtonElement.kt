package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.toComposeShape
import com.adapty.ui.internal.ui.clickIndication
import com.adapty.ui.internal.utils.EventCallback
import com.adapty.ui.internal.utils.getProductGroupKey

@InternalAdaptyApi
public class ButtonElement internal constructor(
    internal val actions: List<Action>,
    internal val normal: UIElement,
    internal val selected: UIElement?,
    internal val selectedCondition: Condition?,
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val state = resolveState()
        val item = when {
            selected == null -> normal
            selectedCondition is Condition.SelectedSection -> {
                val sectionKey = SectionElement.getKey(selectedCondition.sectionId)
                if (state[sectionKey] as? Int == selectedCondition.index)
                    selected
                else
                    normal
            }
            selectedCondition is Condition.SelectedProduct -> {
                val productGroupKey = getProductGroupKey(selectedCondition.groupId)
                if (state[productGroupKey] as? String == selectedCondition.productId)
                    selected
                else
                    normal
            }
            else -> normal
        }
        val shape = item.baseProps.shape?.type?.toComposeShape()
        val actionsResolved = actions.mapNotNull { action -> action.resolve(resolveText) }
        Box(
            modifier
                .clickable(
                    indication = shape?.let { clickIndication() },
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    eventCallback.onActions(actionsResolved)
                }
        ) {
            item.render(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
            )
        }
    }
}