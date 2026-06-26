@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(ExperimentalMaterial3Api::class)

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.script.toJsFloat
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.LocalUiEnabled
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.utils.DISABLED_ALPHA
import com.adapty.ui.internal.utils.TwoWayBinding

private val TrackHeight = 4.dp
private val ThumbSize = 28.dp
private val ThumbElevation = 4.dp
private val InactiveTrackColor = Color(0xFFE5E5EA)

@InternalAdaptyApi
public class SliderElement internal constructor(
    internal val value: TwoWayBinding,
    internal val min: Float,
    internal val max: Float,
    internal val steps: Int,
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val state = resolveState()
        val screen = LocalScreenInstance.current
        val enabled = LocalUiEnabled.current
        val currentValue = (state[value].toJsFloat() ?: min).coerceIn(min, max)
        val activeTrackColor = MaterialTheme.colorScheme.primary
            .let { if (enabled) it else it.copy(alpha = DISABLED_ALPHA) }
        val thumbColor = if (enabled) Color.White else Color.White.copy(alpha = DISABLED_ALPHA)
        val fraction = if (max > min) ((currentValue - min) / (max - min)).coerceIn(0f, 1f) else 0f

        Slider(
            value = currentValue,
            onValueChange = { newValue ->
                dispatch(Message.ValueChanged(value, newValue, screen))
            },
            enabled = enabled,
            valueRange = min..max,
            steps = steps,
            modifier = modifier,
            thumb = {
                Box(
                    modifier = Modifier
                        .size(ThumbSize)
                        .shadow(ThumbElevation, CircleShape)
                        .background(thumbColor, CircleShape)
                )
            },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TrackHeight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TrackHeight)
                            .background(InactiveTrackColor, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(TrackHeight)
                            .background(activeTrackColor, CircleShape)
                    )
                }
            },
        )
    }
}
