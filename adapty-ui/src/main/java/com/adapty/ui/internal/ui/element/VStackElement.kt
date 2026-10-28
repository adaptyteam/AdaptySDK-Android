@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.VStackLayout
import com.adapty.ui.internal.ui.VStackCellKind
import com.adapty.ui.internal.ui.STACK_SPACER_MIN_LENGTH
import com.adapty.ui.internal.ui.VStackCellSpec
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.HorizontalAlign
import com.adapty.ui.internal.ui.attributes.LocalParentImposesHeight
import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
import com.adapty.ui.internal.ui.attributes.PageSize
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.attributes.toExactDp
import com.adapty.ui.internal.ui.attributes.LocalOverflowAnchorVertical
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.LocalSectionPresenceSlot
import com.adapty.ui.internal.ui.rememberSectionPresenceSlots
import com.adapty.ui.internal.ui.resolveInPresenceSlot

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
        val horizontalAlignment = align.toComposeAlignment()
        val presenceSlots = rememberSectionPresenceSlots(content, vertical = true)
        val activeVariantHugs = baseProps.heightSpec == null && content.withIndex().none { (i, it) ->
            resolveInPresenceSlot(presenceSlots[i]) {
                it.layoutRelevantPropsResolved().weight != null || it.fillsColumnMainAxisResolved()
            }
        }
        val overflowAnchor = if (activeVariantHugs) LocalOverflowAnchorVertical.current else Alignment.Top
        val emitting = content.withIndex().filterNot { (_, it) -> it === UnknownElement || it === SkippedElement }
        val slots = emitting.map { (i, _) -> presenceSlots[i] }
        val cells = emitting.map { (i, item) ->
            resolveInPresenceSlot(presenceSlots[i]) { item.vStackCellSpec() }
        }
        emitting.forEachIndexed { k, (i, _) ->
            presenceSlots[i]?.let { slot ->
                val fills = cells[k].kind == VStackCellKind.FILL_HEIGHT
                SideEffect { slot.fillsMainAxis = fills }
            }
        }
        Box(modifier = modifier) {
            VStackLayout(
                spacing = spacing ?: 0.dp,
                horizontalAlignment = horizontalAlignment,
                overflowAnchor = overflowAnchor,
                cells = cells,
                modifier = Modifier,
                slots = slots.takeIf { list -> list.any { it != null } },
            ) {
                CompositionLocalProvider(LocalParentImposesHeight provides false) {
                    emitting.forEach { (i, item) ->
                        CompositionLocalProvider(LocalSectionPresenceSlot provides presenceSlots[i]) {
                            item.withActiveAnimations(dispatch) {
                                item.render(dispatch, Modifier.fillWithBaseParams(item))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(InternalAdaptyApi::class)
private fun UIElement.isTextLikeForHeight(): Boolean = when (this) {
    is BaseTextElement -> true
    is SingleContainer -> layoutRelevantProps.heightSpec == null && content.isTextLikeForHeight()
    else -> false
}

@OptIn(InternalAdaptyApi::class)
@Composable
private fun UIElement.isGreedyOnColumnMainAxis(): Boolean {
    if (layoutRelevantPropsResolved().heightSpec != null) return false
    return when (this) {
        is PagerElement -> true
        is ColumnElement -> fillsMainAxis()
        is FlexElement -> columnElement.fillsMainAxis()
        is SwitchElement -> default.growsOnColumnMainAxis() || cases.any { it.content.growsOnColumnMainAxis() }
        is FlexStackElement -> vStackElement.content.any { it.growsOnColumnMainAxis() }
        is SectionElement -> layoutSlotOrNull()?.isGreedyOnColumnMainAxis() ?: false
        is SingleContainer -> content.isGreedyOnColumnMainAxis()
        else -> false
    }
}

@OptIn(InternalAdaptyApi::class)
@Composable
private fun UIElement.growsOnColumnMainAxis(): Boolean =
    stretchesToColumnProposal() || isGreedyOnColumnMainAxis()

@OptIn(InternalAdaptyApi::class)
private fun ColumnElement.fillsMainAxis(): Boolean = (height ?: MainAxisBehaviour.FILL) == MainAxisBehaviour.FILL

@OptIn(InternalAdaptyApi::class)
@Composable
private fun UIElement.stretchesToColumnProposal(): Boolean = when {
    fillsColumnMainAxisResolved() -> true
    layoutRelevantPropsResolved().heightSpec != null -> false
    this is VStackElement || this is ZStackElement -> (this as MultiContainer).content.any { it.stretchesToColumnProposal() }
    this is SectionElement -> layoutSlotOrNull()?.stretchesToColumnProposal() ?: false
    this is SingleContainer -> content.stretchesToColumnProposal()
    else -> false
}

@OptIn(InternalAdaptyApi::class)
@Composable
private fun UIElement.vStackCellSpec(): VStackCellSpec = when {
    this is SpaceElement || layoutRelevantPropsResolved().weight != null -> {
        val weight = layoutRelevantPropsResolved().weight ?: 1f
        VStackCellSpec(
            VStackCellKind.SPACER,
            minHeight = if (this is SpaceElement) STACK_SPACER_MIN_LENGTH * weight else 0.dp,
            weight = weight,
        )
    }
    this is PagerElement && layoutRelevantPropsResolved().heightSpec == null && pageHeight is PageSize.Unit ->
        VStackCellSpec(VStackCellKind.SIZE_DRIVEN_SIZED, minHeight = (pageHeight as PageSize.Unit).value.toExactDp(DimSpec.Axis.Y))
    stretchesToColumnProposal() -> VStackCellSpec(
        VStackCellKind.FILL_HEIGHT,
        minFromContent = !fillsColumnMainAxisResolved() || fillContentIsRigid(),
    )
    isTextLikeForHeight() -> VStackCellSpec(VStackCellKind.TEXT_LIKE)
    isGreedyOnColumnMainAxis() -> VStackCellSpec(VStackCellKind.GREEDY_ROOT)
    this is SizeDrivenElement -> {
        val props = layoutRelevantProps
        if (props.widthSpec == null && props.heightSpec == null) {
            VStackCellSpec(VStackCellKind.SIZE_DRIVEN_UNSIZED)
        } else {
            VStackCellSpec(VStackCellKind.SIZE_DRIVEN_SIZED, minHeight = props.heightSpec.declaredMinHeight())
        }
    }
    else -> VStackCellSpec(VStackCellKind.DEFAULT)
}

@OptIn(InternalAdaptyApi::class)
@Composable
private fun UIElement.fillContentIsRigid(): Boolean = when {
    this is BoxWithoutContentElement || this is SizeDrivenElement || this is BaseTextElement -> false
    this is PagerElement || this is FlexElement || this is ScreenHolderElement -> false
    this is ColumnElement && fillsMainAxis() -> false
    this is SectionElement -> layoutSlotOrNull()?.fillContentHolds() ?: false
    this is SingleContainer -> content.fillContentHolds()
    else -> true
}

@OptIn(InternalAdaptyApi::class)
@Composable
private fun UIElement.fillContentHolds(): Boolean = when (layoutRelevantPropsResolved().heightSpec) {
    null, is DimSpec.FillMax -> fillContentIsRigid()
    else -> true
}

@OptIn(InternalAdaptyApi::class)
@Composable
private fun DimSpec?.declaredMinHeight(): Dp = when (this) {
    is DimSpec.Specified -> value.toExactDp(DimSpec.Axis.Y)
    is DimSpec.Min -> value.toExactDp(DimSpec.Axis.Y)
    is DimSpec.Shrink -> min.toExactDp(DimSpec.Axis.Y)
    else -> 0.dp
}
