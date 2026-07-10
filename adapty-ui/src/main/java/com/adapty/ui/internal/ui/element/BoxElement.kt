@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.Align
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.LocalContentAlignment
import com.adapty.ui.internal.ui.attributes.LocalOverflowAnchorHorizontal
import com.adapty.ui.internal.ui.attributes.LocalOverflowAnchorVertical
import com.adapty.ui.internal.ui.attributes.LocalParentImposesHeight
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.attributes.toHorizontalAlignmentOrCenter
import com.adapty.ui.internal.ui.attributes.toVerticalAlignmentOrCenter
import com.adapty.ui.internal.store.Message

@InternalAdaptyApi
public class BoxElement internal constructor(
    override var content: UIElement,
    internal val align: Align,
    override val baseProps: BaseProps,
) : UIElement, SingleContainer {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier
    ): @Composable () -> Unit = {
        val overflowAnchorH = LocalOverflowAnchorHorizontal.current
        val overflowAnchorV = LocalOverflowAnchorVertical.current
        var localModifier: Modifier = Modifier
        if (baseProps.widthSpec is DimSpec.Specified)
            localModifier = localModifier.wrapContentWidth(overflowAnchorH, unbounded = true)
        if (baseProps.heightSpec is DimSpec.Specified)
            localModifier = localModifier.wrapContentHeight(overflowAnchorV, unbounded = true)
        val contentAlignment = align.toComposeAlignment()
        val escapeContentHeight = remember(content) {
            baseProps.heightSpec is DimSpec.Specified &&
                content is BoxElement &&
                content.hugBoxChainEndsInText()
        }
        Box(
            contentAlignment = contentAlignment,
            modifier = localModifier.then(modifier),
        ) {
            val provided = buildList {
                add(LocalContentAlignment provides contentAlignment)
                if (baseProps.widthSpec != null)
                    add(LocalOverflowAnchorHorizontal provides contentAlignment.toHorizontalAlignmentOrCenter())
                if (baseProps.heightSpec != null)
                    add(LocalOverflowAnchorVertical provides contentAlignment.toVerticalAlignmentOrCenter())
                if (baseProps.heightSpec is DimSpec.Specified || baseProps.heightSpec is DimSpec.FillMax)
                    add(LocalParentImposesHeight provides true)
            }
            CompositionLocalProvider(
                *provided.toTypedArray(),
            ) {
                if (escapeContentHeight) {
                    Box(
                        contentAlignment = contentAlignment,
                        modifier = Modifier.wrapContentHeight(
                            contentAlignment.toVerticalAlignmentOrCenter(), unbounded = true,
                        ),
                    ) {
                        content.render(dispatch)
                    }
                } else {
                    content.render(dispatch)
                }
            }
        }
    }
}
