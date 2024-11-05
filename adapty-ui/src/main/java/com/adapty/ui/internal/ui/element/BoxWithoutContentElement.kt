package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.ui.attributes.Align
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.utils.EventCallback

@InternalAdaptyApi
public class BoxWithoutContentElement internal constructor(
    internal val align: Align,
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier
    ): @Composable () -> Unit = {
        Box(
            contentAlignment = align.toComposeAlignment(),
            modifier = modifier.fillMaxWidth().fillMaxHeight(),
        ) { }
    }
}