package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.Align
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.utils.EventCallback

@InternalAdaptyApi
public class ZStackElement internal constructor(
    override var content: List<UIElement>,
    internal val align: Align,
    override val baseProps: BaseProps,
) : UIElement, MultiContainer {

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        Box(
            contentAlignment = align.toComposeAlignment(),
            modifier = modifier,
        ) {
            content.forEach { item ->
                item.render(
                    resolveAssets,
                    resolveText,
                    resolveState,
                    eventCallback,
                )
            }
        }
    }
}