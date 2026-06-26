@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.Align
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.LocalContentAlignment
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.attributes.toHorizontalAlignmentOrCenter
import com.adapty.ui.internal.ui.attributes.toVerticalAlignmentOrCenter
import com.adapty.ui.internal.store.Message

@InternalAdaptyApi
public class BoxWithoutContentElement internal constructor(
    internal val align: Align,
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier
    ): @Composable () -> Unit = {
        val parentAlignment = LocalContentAlignment.current
        var localModifier: Modifier = Modifier
        if (baseProps.widthSpec is DimSpec.Specified)
            localModifier = localModifier.wrapContentWidth(parentAlignment.toHorizontalAlignmentOrCenter(), unbounded = true)
        if (baseProps.heightSpec is DimSpec.Specified)
            localModifier = localModifier.wrapContentHeight(parentAlignment.toVerticalAlignmentOrCenter(), unbounded = true)
        Box(
            contentAlignment = align.toComposeAlignment(),
            modifier = localModifier.then(modifier).fillMaxWidth().fillMaxHeight(),
        ) { }
    }
}
