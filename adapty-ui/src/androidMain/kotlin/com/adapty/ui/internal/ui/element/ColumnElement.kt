@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.allowVerticalOverflow
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.ui.ZeroIntrinsicsModifier
import com.adapty.ui.internal.store.Message

@InternalAdaptyApi
public class ColumnElement internal constructor(
    override var content: List<UIElement>,
    internal val spacing: Float?,
    internal val height: MainAxisBehaviour?,
    override val baseProps: BaseProps,
) : UIElement, MultiContainer {
    internal val items get() = content.filterIsInstance<GridItem>()

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val spacing = spacing?.dp
        val mode = height ?: MainAxisBehaviour.FILL
        val verticalArrangement = spacing?.let { Arrangement.spacedBy(it) } ?: Arrangement.Top

        when (mode) {
            MainAxisBehaviour.FILL -> {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight().then(modifier)
                        .then(ZeroIntrinsicsModifier),
                ) {
                    val heightBounded = constraints.maxHeight != Constraints.Infinity
                    Column(
                        verticalArrangement = verticalArrangement,
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    ) {
                        items.forEach { item ->
                            item.withActiveAnimations(dispatch) {
                                val itemBaseModifier = Modifier
                                    .fillWithBaseParams(item)
                                    .fillMaxWidth()
                                val itemModifier = when {
                                    heightBounded ->
                                        fillModifierWithScopedParams(item, itemBaseModifier)
                                    item.baseProps.weight != null ->
                                        itemBaseModifier.height(0.dp)
                                    else -> itemBaseModifier
                                }
                                Box(
                                    contentAlignment = item.align.toComposeAlignment(),
                                    modifier = itemModifier,
                                ) {
                                    item.content.render(dispatch)
                                }
                            }
                        }
                    }
                }
            }
            MainAxisBehaviour.HUG -> {
                val fixedItems = items.filter { it.baseProps.weight == null }
                val columnModifier =
                    if (fixedItems.none { it.baseProps.heightSpec is DimSpec.FillMax }) modifier.allowVerticalOverflow()
                    else modifier
                Column(
                    verticalArrangement = verticalArrangement,
                    modifier = columnModifier,
                ) {
                    fixedItems.forEach { item ->
                        item.withActiveAnimations(dispatch) {
                            Box(
                                contentAlignment = item.align.toComposeAlignment(),
                                modifier = Modifier.fillWithBaseParams(item).fillMaxWidth(),
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
