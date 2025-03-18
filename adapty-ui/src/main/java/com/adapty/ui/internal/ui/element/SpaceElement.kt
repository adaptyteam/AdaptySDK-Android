package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.EventCallback

@InternalAdaptyApi
public class SpaceElement internal constructor(
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        Spacer(modifier = modifier)
    }
}