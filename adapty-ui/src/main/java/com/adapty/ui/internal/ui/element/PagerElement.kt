@file:OptIn(ExperimentalFoundationApi::class)

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
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
import com.adapty.ui.internal.ui.attributes.plus
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.attributes.toExactDp
import com.adapty.ui.internal.ui.attributes.toPaddingValues
import com.adapty.ui.internal.ui.marginsOrSkip
import com.adapty.ui.internal.utils.EventCallback
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.getForCurrentSystemTheme
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import kotlinx.coroutines.delay

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
    override val baseProps: BaseProps,
) : UIElement, MultiContainer {

    override fun toComposable(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
        ) {
            renderPagerInternal(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                modifier,
            )
        }
    }

    @Composable
    private fun renderPagerInternal(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ) {
        val pages = content
        val pagerState = rememberPagerState(pageCount = { pages.size })
        val interactionSource = pagerState.interactionSource
        val isDragged = interactionSource.collectIsDraggedAsState()
        val wasInterrupted = remember { mutableStateOf(false) }
        val wasFinishedForever = remember { mutableStateOf(false) }

        if (animation != null && pages.size > 1) {
            val shouldAnimate = !isDragged.value && (interactionBehavior != CANCEL_ANIMATION || !wasInterrupted.value) && !wasFinishedForever.value
            LaunchedEffect(shouldAnimate) {
                if (isDragged.value) wasInterrupted.value = true
                if (!shouldAnimate) return@LaunchedEffect

                delay(if (wasInterrupted.value) animation.afterInteractionDelayMillis.coerceAtLeast(500L) else animation.startDelayMillis)
                slideNext(pagerState, pages, animation) {
                    wasFinishedForever.value = true
                }
            }
        }

        when {
            pagerIndicator == null ->
                BoxWithConstraints(contentAlignment = Alignment.Center) {
                    renderHorizontalPager(
                        maxWidth,
                        maxHeight,
                        pagerState,
                        interactionBehavior,
                        resolveAssets,
                        resolveText,
                        resolveState,
                        eventCallback,
                        modifier,
                        pages,
                    )
                }
            pagerIndicator.layout == PagerIndicator.Layout.OVERLAID || pagerIndicator.vAlign == VerticalAlign.CENTER -> {
                BoxWithConstraints(contentAlignment = Alignment.Center) {
                    renderHorizontalPager(
                        maxWidth,
                        maxHeight,
                        pagerState,
                        interactionBehavior,
                        resolveAssets,
                        resolveText,
                        resolveState,
                        eventCallback,
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
                                resolveAssets = resolveAssets,
                                modifier = Modifier.align(pagerIndicatorAlign.toComposeAlignment())
                            )
                        }
                }
            }
            else -> {
                Column {
                    when (pagerIndicator.vAlign) {
                        VerticalAlign.TOP -> {
                            renderHorizontalPagerIndicator(
                                pagerState = pagerState,
                                data = pagerIndicator,
                                resolveAssets = resolveAssets,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            BoxWithConstraints(
                                Modifier
                                    .weight(1f)
                                    .graphicsLayer { clip = true },
                                Alignment.Center,
                            ) {
                                renderHorizontalPager(
                                    maxWidth,
                                    maxHeight,
                                    pagerState,
                                    interactionBehavior,
                                    resolveAssets,
                                    resolveText,
                                    resolveState,
                                    eventCallback,
                                    modifier,
                                    pages,
                                )
                            }
                        }
                        VerticalAlign.BOTTOM -> {
                            BoxWithConstraints(
                                Modifier
                                    .weight(1f)
                                    .graphicsLayer { clip = true },
                                Alignment.Center,
                            ) {
                                renderHorizontalPager(
                                    maxWidth,
                                    maxHeight,
                                    pagerState,
                                    interactionBehavior,
                                    resolveAssets,
                                    resolveText,
                                    resolveState,
                                    eventCallback,
                                    modifier,
                                    pages,
                                )
                            }
                            renderHorizontalPagerIndicator(
                                pagerState = pagerState,
                                data = pagerIndicator,
                                resolveAssets = resolveAssets,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        else -> Unit
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
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
        pages: List<UIElement>
    ) {
        val spacing = spacing?.dp ?: 0.dp

        val maxAvailablePageWidth = maxAvailableWidth - (pagePadding?.let { it.start.toExactDp(
            DimSpec.Axis.X) + it.end.toExactDp(DimSpec.Axis.X) } ?: 0.dp)
        val maxAvailablePageHeight = maxAvailableHeight - (pagePadding?.let { it.top.toExactDp(
            DimSpec.Axis.Y) + it.bottom.toExactDp(DimSpec.Axis.Y) } ?: 0.dp)
        val pageWidth = when(pageWidth) {
            is PageSize.Unit -> pageWidth.value.toExactDp(DimSpec.Axis.X)
            is PageSize.PageFraction -> maxAvailablePageWidth * pageWidth.fraction
        }
        val pageHeight = when(pageHeight) {
            is PageSize.Unit -> pageHeight.value.toExactDp(DimSpec.Axis.Y)
            is PageSize.PageFraction -> {
                if (pageHeight.fraction != 1f) maxAvailablePageHeight * pageHeight.fraction else null
            }
        }
        HorizontalPager(
            pageSize = androidx.compose.foundation.pager.PageSize.Fixed(pageWidth),
            pageSpacing = spacing,
            contentPadding = pagePadding?.toPaddingValues() ?: PaddingValues(0.dp),
            state = pagerState,
            userScrollEnabled = interactionBehavior != NONE,
            modifier = modifier,
        ) { i ->
            if (pageHeight == null)
                pages[i].render(
                    resolveAssets,
                    resolveText,
                    resolveState,
                    eventCallback,
                )
            else
                Box(Modifier.height(pageHeight)) {
                    pages[i].render(
                        resolveAssets,
                        resolveText,
                        resolveState,
                        eventCallback,
                    )
                }
        }
    }

    private suspend fun slideNext(
        pagerState: PagerState,
        pages: List<UIElement>,
        animation: PagerAnimation,
        onFinishedForever: () -> Unit,
    ) {
        val toPage = ((pagerState.currentPage + 1) % pages.size)

        if (toPage == 0) {
            val transition = animation.repeatTransition
            if (transition != null) {
                pagerState.slideBackToStart(0, transition)
                slideNext(pagerState, pages, animation, onFinishedForever)
            } else {
                onFinishedForever()
            }
        } else {
            val transition = animation.pageTransition
            pagerState.slideToPage(toPage, transition)
            slideNext(pagerState, pages, animation, onFinishedForever)
        }
    }

    private suspend fun PagerState.slideToPage(page: Int, transition: Transition.Slide) {
        animateScrollToPage(
            page = page,
            animationSpec = tween(
                transition.durationMillis,
                transition.startDelayMillis,
                easing = transition.easing,
            )
        )
    }

    private suspend fun PagerState.slideBackToStart(targetPage: Int, transition: Transition.Slide) {
        val pageSize = layoutInfo.pageSize
        val pageSpacing = layoutInfo.pageSpacing
        val currentPage = currentPage

        if (currentPage == targetPage) return

        val pageDiff = targetPage - currentPage
        val distance = (pageDiff * (pageSize + pageSpacing)).toFloat()

        animateScrollBy(
            value = distance,
            animationSpec = tween(
                transition.durationMillis,
                transition.startDelayMillis,
                transition.easing,
            )
        )
    }

    @Composable
    private fun renderHorizontalPagerIndicator(
        pagerState: PagerState,
        data: PagerIndicator,
        resolveAssets: () -> Assets,
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
                    fill = (if (i == pagerState.currentPage) data.selectedColor else data.color)?.assetId?.let { assetId ->
                        when (val filling = resolveAssets().getForCurrentSystemTheme(assetId)) {
                            is Asset.Color -> filling.toComposeFill()
                            is Asset.Gradient -> filling.toComposeFill()
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