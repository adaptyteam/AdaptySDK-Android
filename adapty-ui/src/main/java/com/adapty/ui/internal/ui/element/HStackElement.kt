package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.VerticalAlign
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.utils.EventCallback

@InternalAdaptyApi
public class HStackElement internal constructor(
    override var content: List<UIElement>,
    internal val align: VerticalAlign,
    internal val spacing: Float?,
    override val baseProps: BaseProps,
) : UIElement, MultiContainer {

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val spacing = spacing?.dp
        Row(
            horizontalArrangement = when {
                spacing != null -> Arrangement.spacedBy(spacing)
                else -> Arrangement.Start
            },
            verticalAlignment = align.toComposeAlignment(),
            modifier = modifier,
        ) {
            content.forEach { item ->
                item.run {
                    render(
                        this@Row.toComposableInRow(
                            resolveAssets,
                            resolveText,
                            resolveState,
                            eventCallback,
                            fillModifierWithScopedParams(
                                item,
                                Modifier.fillWithBaseParams(item, resolveAssets),
                            )
                        )
                    )
                }
            }
        }
    }
}