@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.attributes.toComposeVerticalAlignment
import com.adapty.ui.internal.ui.allowHorizontalOverflow
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.store.Message

@InternalAdaptyApi
public class RowElement internal constructor(
    override var content: List<UIElement>,
    internal val spacing: Float?,
    internal val width: MainAxisBehaviour?,
    override val baseProps: BaseProps,
) : UIElement, MultiContainer {
    internal val items get() = content.filterIsInstance<GridItem>()

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val spacing = spacing?.dp
        val mode = width ?: MainAxisBehaviour.FILL
        val horizontalArrangement = spacing?.let { Arrangement.spacedBy(it) } ?: Arrangement.Start

        when (mode) {
            MainAxisBehaviour.FILL -> {
                val heightModifier =
                    if (items.any { it.content.explicitlyFillsRowHeight() }) Modifier.height(IntrinsicSize.Min) else Modifier.fillMaxHeight()
                Row(
                    horizontalArrangement = horizontalArrangement,
                    modifier = heightModifier.fillMaxWidth().then(modifier),
                ) {
                    items.forEach { item ->
                        item.withActiveAnimations(dispatch) {
                            item.run {
                                render(
                                    this@Row.toComposableInRow(
                                        dispatch,
                                        fillModifierWithScopedParams(
                                            item,
                                            Modifier.fillWithBaseParams(item),
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
            MainAxisBehaviour.HUG -> {
                val fixedItems = items.filter { it.baseProps.weight == null }
                val heightModifier =
                    if (fixedItems.any { it.content.explicitlyFillsRowHeight() }) Modifier.height(IntrinsicSize.Min) else Modifier.fillMaxHeight()
                val noWidthFill =
                    items.size == fixedItems.size &&
                        fixedItems.none {
                            it.baseProps.widthSpec is DimSpec.FillMax ||
                                it.baseProps.widthSpec == null ||
                                it.content is SizeDrivenElement
                        }
                val rowModifier =
                    if (noWidthFill) heightModifier.then(modifier).allowHorizontalOverflow()
                    else heightModifier.then(modifier)
                Row(
                    horizontalArrangement = horizontalArrangement,
                    modifier = rowModifier,
                ) {
                    fixedItems.forEach { item ->
                        item.withActiveAnimations(dispatch) {
                            Box(
                                contentAlignment = item.align.toComposeAlignment(),
                                modifier = Modifier.fillWithBaseParams(item).then(
                                    if (item.content.explicitlyFillsRowHeight()) Modifier.fillMaxHeight()
                                    else Modifier.align(item.align.toComposeVerticalAlignment())
                                ),
                            ) {
                                item.content.render(dispatch)
                            }
                        }
                    }
                }
            }
        }
    }
}
