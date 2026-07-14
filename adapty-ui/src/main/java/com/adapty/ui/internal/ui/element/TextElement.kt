@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.ui.attributes.TextAlign
import com.adapty.ui.internal.ui.resolveText
import com.adapty.ui.internal.store.Message

@InternalAdaptyApi
public class TextElement internal constructor(
    internal val stringId: StringId,
    textAlign: TextAlign,
    internal val maxLines: Int?,
    internal val onOverflow: OnOverflowMode?,
    attributes: Attributes,
    baseProps: BaseProps,
) : BaseTextElement(textAlign, attributes, baseProps) {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        renderTextInternal(
            attributes,
            textAlign,
            maxLines,
            onOverflow,
            modifier,
            dispatch,
        ) {
            resolveText(stringId, attributes)
        }
    }
}
