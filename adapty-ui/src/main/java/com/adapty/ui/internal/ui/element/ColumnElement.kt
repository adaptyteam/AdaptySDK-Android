package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.utils.EventCallback

@InternalAdaptyApi
public class ColumnElement internal constructor(
    override var content: List<UIElement>,
    internal val spacing: Float?,
    override val baseProps: BaseProps,
) : UIElement, MultiContainer {
    internal val items get() = content.filterIsInstance<GridItem>()

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val spacing = spacing?.dp
        Column(
            verticalArrangement = when {
                spacing != null -> Arrangement.spacedBy(spacing)
                else -> Arrangement.Top
            },
            modifier = modifier
                .width(IntrinsicSize.Min),
        ) {
            items.forEach { item ->
                item.run {
                    render(
                        toComposableInColumn(
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