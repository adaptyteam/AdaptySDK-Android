@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.HorizontalAlign
import com.adapty.ui.internal.ui.attributes.LocalParentImposesHeight
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.allowVerticalOverflow
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.ui.ZeroIntrinsicsModifier
import com.adapty.ui.internal.store.Message

@InternalAdaptyApi
public class VStackElement internal constructor(
    override var content: List<UIElement>,
    internal val align: HorizontalAlign,
    internal val spacing: Float?,
    override val baseProps: BaseProps,
) : UIElement, MultiContainer {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val spacing = spacing?.dp
        val verticalArrangement = when {
            spacing != null -> Arrangement.spacedBy(spacing)
            else -> Arrangement.Top
        }
        val horizontalAlignment = align.toComposeAlignment()
        val needsConstraintCheck = content.any { el -> el.anyLayoutVariant { it.fillsColumnMainAxis() } }
        if (needsConstraintCheck) {
            val activeVariantHugs = baseProps.heightSpec == null && content.none {
                it.layoutRelevantPropsResolved().weight != null || it.fillsColumnMainAxisResolved()
            }
            val stackModifier = if (activeVariantHugs) modifier.allowVerticalOverflow() else modifier
            BoxWithConstraints(modifier = stackModifier.then(ZeroIntrinsicsModifier)) {
                val heightBounded = constraints.maxHeight != Constraints.Infinity
                Column(
                    verticalArrangement = verticalArrangement,
                    horizontalAlignment = horizontalAlignment,
                ) {
                    renderContent(dispatch, heightBounded)
                }
            }
        } else {
            val hasFlexibleHeightChild = content.any {
                it.layoutRelevantProps.weight != null || it.layoutRelevantProps.heightSpec is DimSpec.FillMax
            }
            val boxModifier = if (baseProps.heightSpec == null && !hasFlexibleHeightChild)
                modifier.allowVerticalOverflow() else modifier
            Box(modifier = boxModifier) {
                Column(
                    verticalArrangement = verticalArrangement,
                    horizontalAlignment = horizontalAlignment,
                ) {
                    renderContent(dispatch, heightBounded = false)
                }
            }
        }
    }

    @Composable
    private fun ColumnScope.renderContent(
        dispatch: (Message) -> Unit,
        heightBounded: Boolean,
    ) {
        CompositionLocalProvider(LocalParentImposesHeight provides false) {
            content.forEach { item ->
                item.withActiveAnimations(dispatch) {
                    item.run {
                        render(
                            this@renderContent.toComposableInColumn(
                                dispatch,
                                this@renderContent.fillModifierForFlexibleColumn(
                                    item,
                                    Modifier.fillWithBaseParams(item),
                                    heightBounded,
                                ),
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ColumnScope.fillModifierForFlexibleColumn(
        element: UIElement,
        modifier: Modifier,
        heightBounded: Boolean,
    ): Modifier {
        val weight = element.layoutRelevantPropsResolved().weight
        return when {
            weight != null -> modifier.weight(weight)
            element.fillsColumnMainAxisResolved() && heightBounded -> modifier.weight(1f)
            else -> modifier
        }
    }
}
