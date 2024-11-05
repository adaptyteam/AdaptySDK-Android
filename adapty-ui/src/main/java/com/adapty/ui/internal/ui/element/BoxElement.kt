package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.ui.attributes.Align
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.utils.EventCallback

@InternalAdaptyApi
public class BoxElement internal constructor(
    override var content: UIElement,
    internal val align: Align,
    override val baseProps: BaseProps,
) : UIElement, SingleContainer {

    override fun toComposable(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier
    ): @Composable () -> Unit = {
        var localModifier: Modifier = Modifier
        if (baseProps.widthSpec is DimSpec.Specified)
            localModifier = localModifier.wrapContentWidth(unbounded = true)
        if (baseProps.heightSpec is DimSpec.Specified)
            localModifier = localModifier.wrapContentHeight(unbounded = true)
        Box(
            contentAlignment = align.toComposeAlignment(),
            modifier = localModifier.then(modifier),
        ) {
            content.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier.fillWithBaseParams(content, resolveAssets),
            ).invoke()
        }
    }
}