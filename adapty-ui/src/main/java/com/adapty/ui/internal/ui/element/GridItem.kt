@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.Align
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.LocalOverflowAnchorHorizontal
import com.adapty.ui.internal.ui.attributes.LocalOverflowAnchorVertical
import com.adapty.ui.internal.ui.attributes.LocalParentImposesHeight
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.attributes.toComposeHorizontalAlignment
import com.adapty.ui.internal.ui.attributes.toComposeVerticalAlignment
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.store.Message

@InternalAdaptyApi
public class GridItem internal constructor(
    dimAxis: DimSpec.Axis,
    sideSpec: DimSpec?,
    override var content: UIElement,
    internal val align: Align,
    baseProps: BaseProps,
) : UIElement, SingleContainer {

    override val baseProps: BaseProps =
        if (dimAxis == DimSpec.Axis.X)
            baseProps.copy(widthSpec = sideSpec)
        else
            baseProps.copy(heightSpec = sideSpec)

    @Composable
    private fun provideOverflowAnchors(
        horizontal: Boolean,
        vertical: Boolean,
        content: @Composable () -> Unit,
    ) {
        val provided = buildList {
            add(LocalParentImposesHeight provides true)
            if (horizontal) add(LocalOverflowAnchorHorizontal provides align.toComposeHorizontalAlignment())
            if (vertical) add(LocalOverflowAnchorVertical provides align.toComposeVerticalAlignment())
        }
        CompositionLocalProvider(*provided.toTypedArray(), content = content)
    }

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        Box(
            contentAlignment = align.toComposeAlignment(),
            modifier = modifier.fillMaxSize(),
        ) {
            provideOverflowAnchors(horizontal = true, vertical = true) {
                content.render(dispatch)
            }
        }
    }

    override fun ColumnScope.toComposableInColumn(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        Box(
            contentAlignment = align.toComposeAlignment(),
            modifier = modifier.fillMaxWidth(),
        ) {
            provideOverflowAnchors(
                horizontal = true,
                vertical = baseProps.heightSpec != null || baseProps.weight != null,
            ) {
                content.withActiveAnimations(dispatch) {
                    content.run {
                        render(
                            this@toComposableInColumn.toComposableInColumn(
                                dispatch,
                                this@toComposableInColumn.fillModifierWithScopedParams(
                                    content,
                                    Modifier.fillWithBaseParams(content)
                                ),
                            )
                        )
                    }
                }
            }
        }
    }

    override fun RowScope.toComposableInRow(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val verticalAlignment = align.toComposeVerticalAlignment()
        Box(
            contentAlignment = align.toComposeAlignment(),
            modifier = modifier.then(
                if (content.explicitlyFillsRowHeight()) Modifier.fillMaxHeight() else Modifier.align(verticalAlignment)
            ),
        ) {
            provideOverflowAnchors(
                horizontal = baseProps.widthSpec != null || baseProps.weight != null,
                vertical = content.explicitlyFillsRowHeight(),
            ) {
                content.withActiveAnimations(dispatch) {
                    content.run {
                        render(
                            this@toComposableInRow.toComposableInRow(
                                dispatch,
                                this@toComposableInRow.fillModifierWithScopedParams(
                                    content,
                                    Modifier.fillWithBaseParams(content)
                                ),
                            )
                        )
                    }
                }
            }
        }
    }
}
