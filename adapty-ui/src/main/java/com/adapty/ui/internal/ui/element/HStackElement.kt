@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.LocalParentImposesHeight
import com.adapty.ui.internal.ui.attributes.VerticalAlign
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.allowHorizontalOverflow
import com.adapty.ui.internal.ui.fillBoundedHeightOrIntrinsic
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.ui.intrinsicHeightOrHug
import com.adapty.ui.internal.store.Message

@InternalAdaptyApi
public class HStackElement internal constructor(
    override var content: List<UIElement>,
    internal val align: VerticalAlign,
    internal val spacing: Float?,
    override val baseProps: BaseProps,
) : UIElement, MultiContainer {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val spacing = spacing?.dp
        val rowModifier =
            if (content.none {
                    val props = it.layoutRelevantPropsResolved()
                    props.widthSpec is DimSpec.FillMax ||
                        props.widthSpec == null ||
                        props.weight != null ||
                        it is SizeDrivenElement
                })
                modifier.allowHorizontalOverflow()
            else modifier
        val hasFlexibleChild = content.any { it.isVerticallyFlexible() }
        val crossAxisModifier = when {
            !hasFlexibleChild -> Modifier
            LocalParentImposesHeight.current -> Modifier.fillBoundedHeightOrIntrinsic()
            else -> Modifier.intrinsicHeightOrHug()
        }
        Row(
            horizontalArrangement = when {
                spacing != null -> Arrangement.spacedBy(spacing)
                else -> Arrangement.Start
            },
            verticalAlignment = align.toComposeAlignment(),
            modifier = crossAxisModifier.then(rowModifier),
        ) {
            content.forEach { item ->
                item.withActiveAnimations(dispatch) {
                    item.run {
                        val cellModifier = Modifier.fillWithBaseParams(item).let {
                            if (hasFlexibleChild && item.isVerticallyFlexible()) it.fillMaxHeight() else it
                        }
                        render(
                            this@Row.toComposableInRow(
                                dispatch,
                                fillModifierWithScopedParams(item, cellModifier),
                            )
                        )
                    }
                }
            }
        }
    }
}
