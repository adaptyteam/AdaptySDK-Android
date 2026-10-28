@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.toExactDp
import com.adapty.ui.internal.ui.attributes.LocalParentImposesHeight
import com.adapty.ui.internal.ui.attributes.VerticalAlign
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.LocalSectionPresenceSlot
import com.adapty.ui.internal.ui.HStackCellKind
import com.adapty.ui.internal.ui.HStackCellSpec
import com.adapty.ui.internal.ui.STACK_SPACER_MIN_LENGTH
import com.adapty.ui.internal.ui.HStackLayout
import com.adapty.ui.internal.ui.allowHorizontalOverflow
import com.adapty.ui.internal.ui.rememberSectionPresenceSlots
import com.adapty.ui.internal.ui.resolveInPresenceSlot
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
        val presenceSlots = rememberSectionPresenceSlots(content, vertical = false)
        val rowModifier =
            if (content.withIndex().none { (i, it) ->
                    val props = resolveInPresenceSlot(presenceSlots[i]) { it.layoutRelevantPropsResolved() }
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
        val emitting = content.withIndex().filterNot { (_, it) -> it === UnknownElement || it === SkippedElement }
        val slots = emitting.map { (i, _) -> presenceSlots[i] }
        val cells = emitting.map { (i, item) ->
            resolveInPresenceSlot(presenceSlots[i]) { item.hStackCellSpec() }
        }
        emitting.forEachIndexed { k, (i, _) ->
            presenceSlots[i]?.let { slot ->
                val fills = cells[k].kind == HStackCellKind.FILL_WIDTH
                SideEffect { slot.fillsMainAxis = fills }
            }
        }
        HStackLayout(
            spacing = spacing ?: 0.dp,
            verticalAlignment = align.toComposeAlignment(),
            cells = cells,
            modifier = crossAxisModifier.then(rowModifier),
            slots = slots.takeIf { list -> list.any { it != null } },
        ) {
            emitting.forEach { (i, item) ->
                CompositionLocalProvider(LocalSectionPresenceSlot provides presenceSlots[i]) {
                    item.withActiveAnimations(dispatch) {
                        val cellModifier = Modifier
                            .fillWithBaseParams(item)
                            .let {
                                if (hasFlexibleChild && item.isVerticallyFlexible()) it.fillMaxHeight() else it
                            }
                        item.render(dispatch, cellModifier)
                    }
                }
            }
        }
    }
}

@OptIn(InternalAdaptyApi::class)
private fun UIElement.isTextLikeForWidth(): Boolean = when (this) {
    is BaseTextElement -> true
    is SingleContainer -> layoutRelevantProps.widthSpec == null && content.isTextLikeForWidth()
    else -> false
}

@OptIn(InternalAdaptyApi::class)
@Composable
private fun UIElement.hStackCellSpec(): HStackCellSpec = when {
    this is SpaceElement || layoutRelevantPropsResolved().weight != null -> {
        val weight = layoutRelevantPropsResolved().weight ?: 1f
        HStackCellSpec(
            HStackCellKind.SPACER,
            minWidth = if (this is SpaceElement) STACK_SPACER_MIN_LENGTH * weight else 0.dp,
            weight = weight,
        )
    }
    layoutRelevantPropsResolved().widthSpec is DimSpec.FillMax -> HStackCellSpec(HStackCellKind.FILL_WIDTH)
    isTextLikeForWidth() -> HStackCellSpec(HStackCellKind.TEXT_LIKE)
    (this is PagerElement || this is WheelPickerElement || this is DateTimePickerElement || this is TextFieldElement) &&
        layoutRelevantPropsResolved().widthSpec == null -> HStackCellSpec(HStackCellKind.GREEDY_ROOT)
    this is SizeDrivenElement -> {
        val props = layoutRelevantProps
        if (props.widthSpec == null && props.heightSpec == null) {
            HStackCellSpec(HStackCellKind.SIZE_DRIVEN_UNSIZED)
        } else {
            HStackCellSpec(HStackCellKind.SIZE_DRIVEN_SIZED, minWidth = props.widthSpec.declaredMinWidth())
        }
    }
    else -> HStackCellSpec(HStackCellKind.DEFAULT)
}

@OptIn(InternalAdaptyApi::class)
@Composable
private fun DimSpec?.declaredMinWidth(): Dp = when (this) {
    is DimSpec.Specified -> value.toExactDp(DimSpec.Axis.X)
    is DimSpec.Min -> value.toExactDp(DimSpec.Axis.X)
    is DimSpec.Shrink -> min.toExactDp(DimSpec.Axis.X)
    else -> 0.dp
}
