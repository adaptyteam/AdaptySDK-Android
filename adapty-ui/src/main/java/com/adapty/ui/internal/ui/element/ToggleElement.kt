@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.LocalUiEnabled
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.utils.DISABLED_ALPHA
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.resolveAsset

@InternalAdaptyApi
public class ToggleElement internal constructor(
    internal val value: TwoWayBinding,
    internal val color: VisualValue?,
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val state = resolveState()
        val fill = color?.resolveAsset<Asset.Color>()
        val colors = if (fill != null) {
            val accent = fill.toComposeFill().color
            SwitchDefaults.colors(
                checkedTrackColor = accent,
                disabledCheckedTrackColor = accent.copy(alpha = DISABLED_ALPHA),
            )
        } else {
            SwitchDefaults.colors()
        }

        Box(
            Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            val screen = LocalScreenInstance.current
            Switch(
                checked = state[value] as? Boolean ?: false,
                onCheckedChange = { checked ->
                    dispatch(Message.ToggleChanged(value, checked, screen))
                },
                enabled = LocalUiEnabled.current,
                colors = colors,
                modifier = modifier,
            )
        }
    }
}
