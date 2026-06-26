@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Screen
import com.adapty.ui.AdaptyUI.FlowConfiguration.ScreenBundle
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.store.ScrollCommand
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.DimUnit
import com.adapty.ui.internal.ui.element.OverlayItem
import com.adapty.ui.internal.ui.element.SingleContainer
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.withActiveAnimations
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.attributes.toComposeHorizontalAlignment
import com.adapty.ui.internal.ui.attributes.toExactDp
import com.adapty.ui.internal.ui.element.render
import com.adapty.ui.internal.utils.getActivityOrNull
import kotlinx.coroutines.flow.first

@Composable
private fun ForceAdjustResizeOnLegacyApi() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.getActivityOrNull()?.window
        val savedSoftInputMode = window?.attributes?.softInputMode
        if (window != null && savedSoftInputMode != null) {
            val forced = (savedSoftInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv()) or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            window.setSoftInputMode(forced)
        }
        onDispose {
            if (window != null && savedSoftInputMode != null) {
                window.setSoftInputMode(savedSoftInputMode)
            }
        }
    }
}

private const val HERO_CONTENT_BOTTOM_RESERVE_FRACTION = 1f

@Composable
internal fun renderScreen(
    key: String,
    screenBundle: ScreenBundle,
    dispatch: (Message) -> Unit,
) {
    when (val screen = screenBundle.screens[key]) {
        is Screen.Plain -> renderPlainLayoutScreen(
            screen,
            dispatch,
        )
        is Screen.Hero -> renderHeroLayoutScreen(
            screen,
            dispatch,
        )
        is Screen.Flat -> renderFlatLayoutScreen(
            screen,
            dispatch,
        )
        is Screen.Transparent -> renderTransparentLayoutScreen(
            screen,
            dispatch,
        )
        else -> {}
    }
}

@Composable
internal fun renderPlainLayoutScreen(
    screen: Screen.Plain,
    dispatch: (Message) -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
    ) {
        renderAlignedElements(screen.backgrounds, dispatch, Modifier.matchParentSize())
        screen.content.render(dispatch)
        renderAlignedElements(screen.overlays, dispatch, Modifier.matchParentSize())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun renderHeroLayoutScreen(
    screen: Screen.Hero,
    dispatch: (Message) -> Unit,
) {
    ForceAdjustResizeOnLegacyApi()
    val measuredContentHeightPxState = remember { mutableIntStateOf(0) }
    val measuredFooterHeightPxState = remember { mutableIntStateOf(0) }

    val coverHeight = (screen.cover.baseProps.heightSpec as? DimSpec.Specified)
        ?.value
        ?.toExactDp(DimSpec.Axis.Y)
        ?: 0.dp

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
    ) {
        renderAlignedElements(screen.backgrounds, dispatch, Modifier.matchParentSize())
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(coverHeight),
        ) {
            screen.cover.content.render(dispatch)
        }
        val boxMaxHeightPx = constraints.maxHeight
        val contentWrapper = screen.contentWrapper
        val contentOffsetY = contentWrapper.offset?.y?.toExactDp(DimSpec.Axis.Y) ?: 0.dp
        val contentTopPadding = remember(coverHeight) {
            (coverHeight + contentOffsetY).coerceAtLeast(0.dp)
        }
        contentWrapper.offset?.consumed = true
        val density = LocalDensity.current

        val contentFillsViewport = contentWrapper.content.declaresScreenMinHeight()

        val scrollState = rememberScrollState()

        handleScrollCommand(scrollState, "content", dispatch)
        observeScrollValue(scrollState, screen.contentScrollValue, dispatch)

        val wrapperProps = contentWrapper.wrapperProps

        val renderScrollColumn = @Composable {
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null,
            ) {
                Column(
                    horizontalAlignment = contentWrapper.contentAlign.toComposeHorizontalAlignment(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(PaddingValues(top = contentTopPadding))
                        .offsetOrSkip(contentWrapper.offset),
                ) {
                    val availableViewportHeight = with(density) {
                        (boxMaxHeightPx - contentTopPadding.roundToPx()).coerceAtLeast(0).toDp()
                    }
                    val reservePx = if (wrapperProps != null)
                        (boxMaxHeightPx * HERO_CONTENT_BOTTOM_RESERVE_FRACTION).toInt().coerceAtLeast(0)
                    else 0
                    val reserveDp = with(density) { reservePx.toDp() }
                    Box(
                        contentAlignment = contentWrapper.contentAlign.toComposeAlignment(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (wrapperProps != null)
                                    Modifier
                                        .cancelReserveHeight(reservePx)
                                        .then(Modifier.fillWithBaseParams(wrapperProps))
                                        .padding(bottom = reserveDp)
                                else
                                    Modifier.heightIn(min = availableViewportHeight)
                            ),
                    ) {
                        if (contentFillsViewport) {
                            val measuredContentHeightPx = measuredContentHeightPxState.intValue
                            contentWrapper.content.render(
                                dispatch,
                                Modifier
                                    .onSizeChanged { size ->
                                        if (size.height <= 0) return@onSizeChanged
                                        if (measuredContentHeightPxState.intValue != size.height) {
                                            measuredContentHeightPxState.intValue = size.height
                                        }
                                    }
                                    .then(
                                        if (measuredContentHeightPx > 0)
                                            Modifier.height(with(density) { measuredContentHeightPx.toDp() })
                                        else
                                            Modifier.alpha(0f)
                                    )
                                    .fillWithBaseParams(contentWrapper.content),
                            )
                        } else {
                            contentWrapper.content.render(
                                dispatch,
                                Modifier.fillWithBaseParams(contentWrapper.content),
                            )
                        }
                    }
                    val measuredFooterHeightPx = measuredFooterHeightPxState.intValue
                    if (measuredFooterHeightPx > 0) {
                        Spacer(Modifier.height(with(density) { measuredFooterHeightPx.toDp() }))
                    }
                }
            }
        }

        if (wrapperProps != null) {
            withActiveAnimations(wrapperProps, dispatch) { renderScrollColumn() }
        } else {
            renderScrollColumn()
        }

        screen.footer?.let { footer ->
            val footerSizeProbe = Modifier.onSizeChanged { size ->
                if (size.height <= 0 || measuredFooterHeightPxState.intValue == size.height)
                    return@onSizeChanged
                measuredFooterHeightPxState.intValue = size.height
            }
            val measuredFooterHeightPx = measuredFooterHeightPxState.intValue
            val footerFillsViewport =
                measuredFooterHeightPx > 0 && measuredFooterHeightPx >= constraints.maxHeight

            if (footerFillsViewport) {
                val footerScrollState = rememberScrollState()
                handleScrollCommand(footerScrollState, "footer", dispatch)
                observeScrollValue(footerScrollState, screen.footerScrollValue, dispatch)
                CompositionLocalProvider(
                    LocalOverscrollConfiguration provides null,
                ) {
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxHeight()
                            .verticalScroll(footerScrollState),
                    ) {
                        footer.render(
                            dispatch,
                            footerSizeProbe.fillWithBaseParams(footer),
                        )
                    }
                }
            } else {
                footer.render(
                    dispatch,
                    Modifier
                        .align(Alignment.BottomCenter)
                        .then(footerSizeProbe)
                        .fillWithBaseParams(footer),
                )
            }
        }
        renderAlignedElements(screen.overlays, dispatch, Modifier.matchParentSize())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun renderFlatLayoutScreen(
    screen: Screen.Flat,
    dispatch: (Message) -> Unit,
) {
    ForceAdjustResizeOnLegacyApi()
    val measuredContentHeightPxState = remember { mutableIntStateOf(0) }
    val measuredFooterHeightPxState = remember { mutableIntStateOf(0) }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
    ) {
        renderAlignedElements(screen.backgrounds, dispatch, Modifier.matchParentSize())
        val density = LocalDensity.current
        val contentWrapper = screen.contentWrapper

        val contentFillsViewport = contentWrapper.content.declaresScreenMinHeight()

        val contentScrollState = rememberScrollState()

        handleScrollCommand(contentScrollState, "content", dispatch)
        observeScrollValue(contentScrollState, screen.contentScrollValue, dispatch)

        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
        ) {
            if (contentFillsViewport) {
                Column(
                    horizontalAlignment = contentWrapper.contentAlign.toComposeHorizontalAlignment(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(contentScrollState),
                ) {
                    val measuredContentHeightPx = measuredContentHeightPxState.intValue
                    contentWrapper.content.withActiveAnimations(dispatch) {
                        contentWrapper.content.render(
                            dispatch,
                            Modifier
                                .onSizeChanged { size ->
                                    if (size.height <= 0) return@onSizeChanged
                                    if (measuredContentHeightPxState.intValue != size.height) {
                                        measuredContentHeightPxState.intValue = size.height
                                    }
                                }
                                .then(
                                    if (measuredContentHeightPx > 0)
                                        Modifier.height(with(density) { measuredContentHeightPx.toDp() })
                                    else
                                        Modifier.alpha(0f)
                                )
                                .fillWithBaseParams(contentWrapper.content),
                        )
                    }
                    val measuredFooterHeightPx = measuredFooterHeightPxState.intValue
                    if (measuredFooterHeightPx > 0) {
                        Spacer(Modifier.height(with(density) { measuredFooterHeightPx.toDp() }))
                    }
                }
            } else {
                Column(
                    horizontalAlignment = contentWrapper.contentAlign.toComposeHorizontalAlignment(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(contentScrollState),
                ) {
                    contentWrapper.content.withActiveAnimations(dispatch) {
                        contentWrapper.content.render(
                            dispatch,
                            Modifier.fillWithBaseParams(contentWrapper.content),
                        )
                    }
                    val measuredFooterHeightPx = measuredFooterHeightPxState.intValue
                    if (measuredFooterHeightPx > 0) {
                        Spacer(Modifier.height(with(density) { measuredFooterHeightPx.toDp() }))
                    }
                }
            }
        }

        screen.footer?.let { footer ->
            val footerSizeProbe = Modifier.onSizeChanged { size ->
                if (size.height <= 0 || measuredFooterHeightPxState.intValue == size.height)
                    return@onSizeChanged
                measuredFooterHeightPxState.intValue = size.height
            }
            val measuredFooterHeightPx = measuredFooterHeightPxState.intValue
            val footerFillsViewport =
                measuredFooterHeightPx > 0 && measuredFooterHeightPx >= constraints.maxHeight

            if (footerFillsViewport) {
                val footerScrollState = rememberScrollState()
                handleScrollCommand(footerScrollState, "footer", dispatch)
                observeScrollValue(footerScrollState, screen.footerScrollValue, dispatch)
                CompositionLocalProvider(
                    LocalOverscrollConfiguration provides null,
                ) {
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxHeight()
                            .verticalScroll(footerScrollState),
                    ) {
                        footer.render(
                            dispatch,
                            footerSizeProbe.fillWithBaseParams(footer),
                        )
                    }
                }
            } else {
                footer.render(
                    dispatch,
                    Modifier
                        .align(Alignment.BottomCenter)
                        .then(footerSizeProbe)
                        .fillWithBaseParams(footer),
                )
            }
        }
        renderAlignedElements(screen.overlays, dispatch, Modifier.matchParentSize())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun renderTransparentLayoutScreen(
    screen: Screen.Transparent,
    dispatch: (Message) -> Unit,
) {
    ForceAdjustResizeOnLegacyApi()
    Box(
        contentAlignment = Alignment.TopStart,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
        ) {
            renderAlignedElements(screen.backgrounds, dispatch, Modifier.matchParentSize())
            screen.content.render(dispatch)
            screen.footer?.let { footer ->
                val footerScrollState = rememberScrollState()

                handleScrollCommand(footerScrollState, "footer", dispatch, reverseScrolling = true)
                observeScrollValue(footerScrollState, screen.footerScrollValue, dispatch, reverseScrolling = true)

                CompositionLocalProvider(
                    LocalOverscrollConfiguration provides null,
                ) {
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(footerScrollState, reverseScrolling = true),
                    ) {
                        footer.render(dispatch, Modifier.fillWithBaseParams(footer))
                    }
                }
            }
            renderAlignedElements(screen.overlays, dispatch, Modifier.matchParentSize())
        }
    }
}

@Composable
private fun handleScrollCommand(
    scrollState: ScrollState,
    kind: String,
    dispatch: (Message) -> Unit,
    reverseScrolling: Boolean = false,
) {
    val screenInstance = LocalScreenInstance.current
    val scrollCommand = LocalScrollCommand.current

    LaunchedEffect(scrollCommand) {
        val cmd = scrollCommand ?: return@LaunchedEffect
        if (cmd.instanceId != screenInstance.screenInstanceId) return@LaunchedEffect
        if (cmd.kind != kind) return@LaunchedEffect

        val targetEnd = when (cmd.value) {
            "start" -> reverseScrolling
            "end" -> !reverseScrolling
            else -> return@LaunchedEffect
        }
        if (targetEnd) {
            snapshotFlow { scrollState.maxValue }
                .first { it != Int.MAX_VALUE && it > 0 }
            scrollState.animateScrollTo(scrollState.maxValue)
        } else {
            scrollState.animateScrollTo(0)
        }
        dispatch(Message.ScrollCommandConsumed)
    }
}

@Composable
private fun observeScrollValue(
    scrollState: ScrollState,
    binding: TwoWayBinding?,
    dispatch: (Message) -> Unit,
    reverseScrolling: Boolean = false,
) {
    if (binding == null) return
    val screen = LocalScreenInstance.current

    LaunchedEffect(binding) {
        snapshotFlow { scrollState.normalizedScrollValue(reverseScrolling) }
            .collect { normalized ->
                dispatch(Message.ValueChanged(binding, normalized, screen))
            }
    }
}

private fun ScrollState.normalizedScrollValue(reversed: Boolean): Float {
    val max = maxValue
    if (max <= 0 || max == Int.MAX_VALUE) return 0f
    val raw = value.toFloat() / max.toFloat()
    return if (reversed) 1f - raw else raw
}

@Composable
private fun renderAlignedElements(
    items: List<OverlayItem>,
    dispatch: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
    items.forEach { item ->
        Box(
            contentAlignment = item.align.toComposeAlignment(),
            modifier = modifier,
        ) {
            item.content.render(dispatch)
        }
    }
}

private fun UIElement.declaresScreenMinHeight(): Boolean {
    var node: UIElement = this
    while (true) {
        when (val spec = node.baseProps.heightSpec) {
            is DimSpec.Min -> if (spec.value is DimUnit.ScreenFraction) return true
            is DimSpec.Specified, is DimSpec.Shrink -> return false
            else -> {}
        }
        node = (node as? SingleContainer)?.content ?: return false
    }
}

private fun Modifier.cancelReserveHeight(reservePx: Int): Modifier =
    if (reservePx <= 0) this else this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val reportedHeight = (placeable.height - reservePx).coerceAtLeast(0)
        layout(placeable.width, reportedHeight) {
            placeable.place(0, 0)
        }
    }
