@file:OptIn(ExperimentalFoundationApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.ComposeFill
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.EdgeEntities
import com.adapty.ui.internal.ui.attributes.HorizontalAlign
import com.adapty.ui.internal.ui.attributes.InteractionBehavior
import com.adapty.ui.internal.ui.attributes.InteractionBehavior.CANCEL_ANIMATION
import com.adapty.ui.internal.ui.attributes.InteractionBehavior.NONE
import com.adapty.ui.internal.ui.attributes.PageSize
import com.adapty.ui.internal.ui.attributes.PagerAnimation
import com.adapty.ui.internal.ui.attributes.PagerIndicator
import com.adapty.ui.internal.ui.attributes.Transition
import com.adapty.ui.internal.ui.attributes.VerticalAlign
import com.adapty.ui.internal.ui.attributes.easing
import com.adapty.ui.internal.ui.attributes.hasAnyNegative
import com.adapty.ui.internal.ui.attributes.horizontalSumOrDefault
import com.adapty.ui.internal.ui.attributes.plus
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.attributes.toExactDp
import com.adapty.ui.internal.ui.attributes.toPaddingValues
import com.adapty.ui.internal.ui.attributes.toPositivePaddingValues
import com.adapty.ui.internal.ui.attributes.verticalSumOrDefault
import com.adapty.ui.internal.ui.marginsOrSkip
import com.adapty.ui.internal.ui.resolveAssets
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.script.toJsInt
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.ZeroIntrinsicsModifier
import com.adapty.ui.internal.ui.negativePaddingInset
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.internal.utils.areAnimationsDisabled
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.resolveAsset
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@InternalAdaptyApi
public class PagerElement internal constructor(
    internal val pageWidth: PageSize,
    internal val pageHeight: PageSize,
    internal val pagePadding: EdgeEntities?,
    internal val spacing: Float?,
    override var content: List<UIElement>,
    internal val pagerIndicator: PagerIndicator?,
    internal val animation: PagerAnimation?,
    internal val interactionBehavior: InteractionBehavior,
    internal val pageIndex: TwoWayBinding?,
    override val baseProps: BaseProps,
) : UIElement, MultiContainer {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
        ) {
            renderPagerInternal(
                dispatch,
                modifier,
            )
        }
    }

    @Composable
    private fun renderPagerInternal(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ) {
        val pages = content
        val pagerState = rememberPagerState(pageCount = { pages.size })
        val interactionSource = pagerState.interactionSource
        val isDragged = interactionSource.collectIsDraggedAsState()
        val wasInterrupted = remember { mutableStateOf(false) }
        val wasFinishedForever = remember { mutableStateOf(false) }
        val context = LocalContext.current
        val animationsDisabled = remember(context) {
            context.areAnimationsDisabled()
        }

        if (pageIndex != null) {
            val state = resolveState()
            val screen = LocalScreenInstance.current

            LaunchedEffect(pagerState) {
                snapshotFlow {
                    pagerState.currentPage to pagerState.isScrollInProgress
                }
                    .filter { (_, scrolling) -> !scrolling }
                    .map { (page, _) -> page }
                    .distinctUntilChanged()
                    .collect { page ->
                        dispatch(Message.ValueChanged(pageIndex, page, screen))
                    }
            }

            val bindingPage = state[pageIndex].toJsInt()
                ?.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            LaunchedEffect(bindingPage) {
                if (bindingPage != null && !pagerState.isScrollInProgress && pagerState.currentPage != bindingPage) {
                    pagerState.animateScrollToPage(bindingPage)
                }
            }
        }

        if (animation != null && pages.size > 1) {
            val shouldAnimate = !isDragged.value && (interactionBehavior != CANCEL_ANIMATION || !wasInterrupted.value) && !wasFinishedForever.value
            LaunchedEffect(shouldAnimate) {
                if (isDragged.value) wasInterrupted.value = true
                if (!shouldAnimate) return@LaunchedEffect

                delay(if (wasInterrupted.value) animation.afterInteractionDelayMillis.coerceAtLeast(500L) else animation.startDelayMillis)
                slideNext(pagerState, pages, animation, animationsDisabled) {
                    wasFinishedForever.value = true
                }
            }
        }

        val pagerClipModifier = (if (pagePadding?.hasAnyNegative == true) {
            Modifier.graphicsLayer { clip = true }
        } else {
            Modifier
        }).then(ZeroIntrinsicsModifier)
        when {
            pagerIndicator == null ->
                BoxWithConstraints(pagerClipModifier, Alignment.Center) {
                    renderHorizontalPager(
                        maxWidth,
                        maxHeight,
                        pagerState,
                        interactionBehavior,
                        dispatch,
                        modifier,
                        pages,
                    )
                }
            pagerIndicator.layout == PagerIndicator.Layout.OVERLAID || pagerIndicator.vAlign == VerticalAlign.CENTER -> {
                BoxWithConstraints(pagerClipModifier, Alignment.Center) {
                    renderHorizontalPager(
                        maxWidth,
                        maxHeight,
                        pagerState,
                        interactionBehavior,
                        dispatch,
                        modifier,
                        pages,
                    )
                    runCatching { pagerIndicator.vAlign + HorizontalAlign.CENTER }
                        .getOrElse { e ->
                            log(ERROR) { "$LOG_PREFIX Couldn't resolve pager indicator alignment: ${e.localizedMessage}" }
                            null
                        }
                        ?.let { pagerIndicatorAlign ->
                            renderHorizontalPagerIndicator(
                                pagerState = pagerState,
                                data = pagerIndicator,
                                modifier = Modifier.align(pagerIndicatorAlign.toComposeAlignment())
                            )
                        }
                }
            }
            else -> {
                BoxWithConstraints(ZeroIntrinsicsModifier) {
                    val hasBoundedHeight = constraints.hasBoundedHeight
                    Column(modifier) {
                        val viewportModifier = (if (hasBoundedHeight) Modifier.weight(1f) else Modifier)
                            .graphicsLayer { clip = true }
                            .then(ZeroIntrinsicsModifier)
                        when (pagerIndicator.vAlign) {
                            VerticalAlign.TOP -> {
                                renderHorizontalPagerIndicator(
                                    pagerState = pagerState,
                                    data = pagerIndicator,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                BoxWithConstraints(viewportModifier, Alignment.Center) {
                                    renderHorizontalPager(
                                        maxWidth,
                                        maxHeight,
                                        pagerState,
                                        interactionBehavior,
                                        dispatch,
                                        Modifier,
                                        pages,
                                    )
                                }
                            }
                            VerticalAlign.BOTTOM -> {
                                BoxWithConstraints(viewportModifier, Alignment.Center) {
                                    renderHorizontalPager(
                                        maxWidth,
                                        maxHeight,
                                        pagerState,
                                        interactionBehavior,
                                        dispatch,
                                        Modifier,
                                        pages,
                                    )
                                }
                                renderHorizontalPagerIndicator(
                                    pagerState = pagerState,
                                    data = pagerIndicator,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun renderHorizontalPager(
        maxAvailableWidth: Dp,
        maxAvailableHeight: Dp,
        pagerState: PagerState,
        interactionBehavior: InteractionBehavior,
        dispatch: (Message) -> Unit,
        modifier: Modifier,
        pages: List<UIElement>
    ) {
        val spacing = spacing?.dp ?: 0.dp

        val horizontalPagePadding = pagePadding.horizontalSumOrDefault
        val verticalPagePadding = pagePadding.verticalSumOrDefault
        val pageWidth = (when (val pw = pageWidth) {
            is PageSize.Unit -> pw.value.toExactDp(DimSpec.Axis.X)
            is PageSize.PageFraction -> maxAvailableWidth * pw.fraction
        } - horizontalPagePadding).coerceAtLeast(1.dp)
        val pageHeight = when (val ph = pageHeight) {
            is PageSize.Unit -> (ph.value.toExactDp(DimSpec.Axis.Y) - verticalPagePadding).coerceAtLeast(0.dp)
            is PageSize.PageFraction ->
                if (ph.fraction != 1f && maxAvailableHeight.isFinite) (maxAvailableHeight * ph.fraction - verticalPagePadding).coerceAtLeast(0.dp) else null
        }
        val pagerContentPadding: PaddingValues
        val pagerModifier: Modifier
        if (pagePadding != null && pagePadding.hasAnyNegative) {
            val startNegDp = pagePadding.start.toExactDp(DimSpec.Axis.X).coerceAtMost(0.dp)
            val endNegDp = pagePadding.end.toExactDp(DimSpec.Axis.X).coerceAtMost(0.dp)
            val startPosDp = pagePadding.start.toExactDp(DimSpec.Axis.X).coerceAtLeast(0.dp)
            val endPosDp = pagePadding.end.toExactDp(DimSpec.Axis.X).coerceAtLeast(0.dp)
            val topPosDp = pagePadding.top.toExactDp(DimSpec.Axis.Y).coerceAtLeast(0.dp)
            val bottomPosDp = pagePadding.bottom.toExactDp(DimSpec.Axis.Y).coerceAtLeast(0.dp)
            val effectiveViewportWidth = maxAvailableWidth - startNegDp - endNegDp
            val trailingRoom = (effectiveViewportWidth - pageWidth - startPosDp).coerceAtLeast(0.dp)
            pagerContentPadding = PaddingValues(
                start = startPosDp,
                top = topPosDp,
                end = maxOf(endPosDp, trailingRoom),
                bottom = bottomPosDp,
            )
            pagerModifier = modifier.negativePaddingInset(pagePadding)
        } else {
            val startPadDp = pagePadding?.start?.toExactDp(DimSpec.Axis.X) ?: 0.dp
            val endPadDp = pagePadding?.end?.toExactDp(DimSpec.Axis.X) ?: 0.dp
            val topPadDp = pagePadding?.top?.toExactDp(DimSpec.Axis.Y) ?: 0.dp
            val bottomPadDp = pagePadding?.bottom?.toExactDp(DimSpec.Axis.Y) ?: 0.dp
            val trailingRoom = (maxAvailableWidth - pageWidth - startPadDp).coerceAtLeast(0.dp)
            pagerContentPadding = PaddingValues(
                start = startPadDp,
                top = topPadDp,
                end = maxOf(endPadDp, trailingRoom),
                bottom = bottomPadDp,
            )
            pagerModifier = modifier
        }
        HorizontalPager(
            pageSize = androidx.compose.foundation.pager.PageSize.Fixed(pageWidth),
            pageSpacing = spacing,
            contentPadding = pagerContentPadding,
            beyondViewportPageCount = if (!maxAvailableHeight.isFinite) pages.size else 0,
            verticalAlignment = if (pageHeight != null) Alignment.Top else Alignment.CenterVertically,
            state = pagerState,
            userScrollEnabled = interactionBehavior != NONE,
            modifier = if (pageHeight != null) pagerModifier.fillMaxHeight() else pagerModifier,
        ) { i ->
            Box(
                if (pageHeight != null) Modifier.width(pageWidth).height(pageHeight)
                else Modifier.width(pageWidth),
                contentAlignment = Alignment.Center,
            ) {
                pages[i].render(dispatch)
            }
        }
    }

    private suspend fun slideNext(
        pagerState: PagerState,
        pages: List<UIElement>,
        animation: PagerAnimation,
        animationsDisabled: Boolean,
        onFinishedForever: () -> Unit,
    ) {
        val toPage = ((pagerState.currentPage + 1) % pages.size)

        if (toPage == 0) {
            val transition = animation.repeatTransition
            if (transition != null) {
                pagerState.slideBackToStart(0, transition, animation.pageTransition, animationsDisabled)
                slideNext(pagerState, pages, animation, animationsDisabled, onFinishedForever)
            } else {
                onFinishedForever()
            }
        } else {
            val transition = animation.pageTransition
            pagerState.slideToPage(toPage, transition, animationsDisabled)
            slideNext(pagerState, pages, animation, animationsDisabled, onFinishedForever)
        }
    }

    private suspend fun PagerState.slideToPage(
        page: Int,
        transition: Transition,
        animationsDisabled: Boolean,
    ) {
        if (animationsDisabled) {
            delay(transition.startDelayMillis.roundToLong().coerceAtLeast(500L))
            scrollToPage(page)
            return
        }
        animateScrollToPage(
            page = page,
            animationSpec = tween(
                transition.durationMillis.roundToInt(),
                transition.startDelayMillis.roundToInt(),
                easing = transition.easing,
            )
        )
    }

    private suspend fun PagerState.slideBackToStart(
        targetPage: Int,
        repeatTransition: Transition,
        pageTransition: Transition,
        animationsDisabled: Boolean,
    ) {
        val pageSize = layoutInfo.pageSize
        val pageSpacing = layoutInfo.pageSpacing
        val currentPage = currentPage

        if (currentPage == targetPage) return

        val timerIntervalMillis = pageTransition.durationMillis + pageTransition.startDelayMillis
        val paddingDelayMillis = (timerIntervalMillis - repeatTransition.durationMillis).coerceAtLeast(0f)

        if (animationsDisabled) {
            delay(timerIntervalMillis.roundToLong().coerceAtLeast(500L))
            scrollToPage(targetPage)
            return
        }

        val pageDiff = targetPage - currentPage
        val distance = (pageDiff * (pageSize + pageSpacing)).toFloat()

        animateScrollBy(
            value = distance,
            animationSpec = tween(
                durationMillis = repeatTransition.durationMillis.roundToInt(),
                delayMillis = paddingDelayMillis.roundToInt(),
                easing = repeatTransition.easing,
            )
        )
    }

    @Composable
    private fun renderHorizontalPagerIndicator(
        pagerState: PagerState,
        data: PagerIndicator,
        modifier: Modifier = Modifier,
    ) {
        val spacing = data.spacing.dp
        Row(
            Modifier
                .marginsOrSkip(data.padding)
                .then(modifier)
        ) {
            repeat(pagerState.pageCount) { i ->
                RoundDot(
                    fill = (if (i == pagerState.currentPage) data.selectedColor else data.color)?.let { value ->
                        val filling = value.resolveAsset<Asset.Filling.Local>()
                        when (filling?.main) {
                            is Asset.Color -> filling.cast<Asset.Color>().toComposeFill()
                            is Asset.Gradient -> filling.cast<Asset.Gradient>().toComposeFill()
                            else -> null
                        }
                    },
                    modifier = Modifier.size(data.dotSize.dp),
                )
                if (i < pagerState.pageCount - 1 && spacing > 0.dp) {
                    Spacer(modifier = Modifier.width(spacing))
                }
            }
        }
    }

    @Composable
    private fun RoundDot(fill: ComposeFill?, modifier: Modifier) {
        Canvas(modifier = modifier) {
            when (fill) {
                is ComposeFill.Color -> drawCircle(fill.color)
                is ComposeFill.Gradient -> drawCircle(fill.shader)
                else -> drawCircle(Color.Unspecified)
            }
        }
    }
}