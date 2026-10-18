@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.store.Message

@InternalAdaptyUiApi
public class SpaceElement internal constructor(
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        Spacer(modifier = modifier)
    }
}
