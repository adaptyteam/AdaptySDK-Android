package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.utils.EventCallback
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.getProductGroupKey

@InternalAdaptyApi
public class ToggleElement internal constructor(
    internal val onActions: List<Action>,
    internal val offActions: List<Action>,
    internal val onCondition: Condition,
    internal val color: Shape.Fill?,
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
        val fill = color?.assetId?.let { assetId -> resolveAssets().getAsset<Asset.Color>(assetId) }
        val colors = if (fill != null)
            SwitchDefaults.colors(checkedTrackColor = fill.toComposeFill().color)
        else
            SwitchDefaults.colors()

        Box(
            Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            val onActionsResolved = onActions.mapNotNull { action -> action.resolve(resolveText) }
            val offActionsResolved = offActions.mapNotNull { action -> action.resolve(resolveText) }

            Switch(
                checked = when (onCondition) {
                    is Condition.SelectedSection -> {
                        val sectionKey = SectionElement.getKey(onCondition.sectionId)
                        state[sectionKey] as? Int == onCondition.index
                    }
                    is Condition.SelectedProduct -> {
                        val productGroupKey = getProductGroupKey(onCondition.groupId)
                        state[productGroupKey] as? String == onCondition.productId
                    }
                    else -> false
                },
                onCheckedChange = { checked ->
                    eventCallback.onActions(if (checked) onActionsResolved else offActionsResolved)
                },
                colors = colors,
                modifier = modifier,
            )
        }
    }
}