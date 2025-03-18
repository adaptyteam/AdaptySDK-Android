package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.HorizontalAlign
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.utils.EventCallback

@InternalAdaptyApi
public class VStackElement internal constructor(
    override var content: List<UIElement>,
    internal val align: HorizontalAlign,
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
        Column(
            verticalArrangement = when {
                spacing != null -> Arrangement.spacedBy(spacing)
                else -> Arrangement.Top
            },
            horizontalAlignment = align.toComposeAlignment(),
            modifier = modifier,
        ) {
            content.forEach { item ->
                item.run {
                    render(
                        this@Column.toComposableInColumn(
                            resolveAssets,
                            resolveText,
                            resolveState,
                            eventCallback,
                            fillModifierWithScopedParams(
                                item,
                                Modifier.fillWithBaseParams(item, resolveAssets),
                            ),
                        )
                    )
                }
            }
        }
    }
}