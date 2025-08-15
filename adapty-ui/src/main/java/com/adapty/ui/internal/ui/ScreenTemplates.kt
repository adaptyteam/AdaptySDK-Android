@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Screen
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.ScreenBundle
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.attributes.toExactDp
import com.adapty.ui.internal.ui.element.ResolveAssets
import com.adapty.ui.internal.ui.element.ResolveState
import com.adapty.ui.internal.ui.element.ResolveText
import com.adapty.ui.internal.ui.element.render
import com.adapty.ui.internal.utils.EventCallback

@Composable
internal fun renderDefaultScreen(
    screenBundle: ScreenBundle,
    resolveAssets: ResolveAssets,
    resolveText: ResolveText,
    resolveState: ResolveState,
    eventCallback: EventCallback,
) {
    when (val defaultScreen = screenBundle.defaultScreen) {
        is Screen.Default.Basic -> renderBasicTemplate(
            defaultScreen,
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
        is Screen.Default.Flat -> renderFlatTemplate(
            defaultScreen,
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
        is Screen.Default.Transparent -> renderTransparentTemplate(
            defaultScreen,
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun renderBasicTemplate(
    defaultScreen: Screen.Default.Basic,
    resolveAssets: ResolveAssets,
    resolveText: ResolveText,
    resolveState: ResolveState,
    eventCallback: EventCallback,
) {
    val measuredContentHeightPxState = remember { mutableIntStateOf(0) }
    val measuredFooterHeightPxState = remember { mutableIntStateOf(0) }
    val adjustedContentHeightState = remember(
        measuredContentHeightPxState.intValue,
        measuredFooterHeightPxState.intValue,
    ) { mutableStateOf<Dp>(Dp.Unspecified) }

    val coverHeight = (defaultScreen.cover.baseProps.heightSpec as? DimSpec.Specified)
        ?.value
        ?.toExactDp(DimSpec.Axis.Y)
        ?: 0.dp

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .clickable(enabled = false, onClick = {})
            .backgroundOrSkip(Shape.plain(defaultScreen.background), resolveAssets),
    ) {
        defaultScreen.cover.render(
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
        val boxMaxHeightPx = constraints.maxHeight
        val contentWrapper = defaultScreen.contentWrapper
        val contentOffsetY = contentWrapper.offset?.y?.toExactDp(DimSpec.Axis.Y) ?: 0.dp
        val contentTopPadding = remember(coverHeight) {
            (coverHeight + contentOffsetY).coerceAtLeast(0.dp)
        }
        contentWrapper.offset?.consumed = true
        val density = LocalDensity.current

        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
        ) {
            Box(
                contentAlignment = contentWrapper.contentAlign.toComposeAlignment(),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .run {
                        val adjustedContentHeight = adjustedContentHeightState.value
                        if (adjustedContentHeight != Dp.Unspecified)
                            return@run height(adjustedContentHeight)
                        val measuredFooterHeightPx = measuredFooterHeightPxState.intValue
                        if (measuredFooterHeightPx == 0) return@run this
                        val measuredContentHeightPx = measuredContentHeightPxState.intValue
                        if (measuredContentHeightPx == 0) return@run this
                        with(density) {
                            val contentHeight = calculateAdjustedContentHeightPx(
                                contentTopPadding.toPx() + measuredContentHeightPx.toFloat(),
                                measuredFooterHeightPx.toFloat(),
                                boxMaxHeightPx.toFloat(),
                            ).toDp()
                            height(contentHeight)
                                .also { adjustedContentHeightState.value = contentHeight }
                        }
                    }
                    .padding(
                        PaddingValues(top = contentTopPadding)
                    )
                    .offsetOrSkip(contentWrapper.offset)
                    .backgroundOrSkip(contentWrapper.background, resolveAssets)
            ) {
                contentWrapper.content.render(
                    resolveAssets,
                    resolveText,
                    resolveState,
                    eventCallback,
                    Modifier
                        .onSizeChanged { size ->
                            if (size.height <= 0 || measuredContentHeightPxState.intValue == size.height)
                                return@onSizeChanged
                            measuredContentHeightPxState.intValue = size.height
                        }
                        .fillWithBaseParams(contentWrapper.content, resolveAssets),
                )
            }
        }

        defaultScreen.footer?.let { footer ->
            footer.render(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        if (size.height <= 0 || measuredFooterHeightPxState.intValue == size.height)
                            return@onSizeChanged
                        measuredFooterHeightPxState.intValue = size.height
                    }
                    .fillWithBaseParams(footer, resolveAssets),
            )
        }
        defaultScreen.overlay?.render(
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun renderFlatTemplate(
    defaultScreen: Screen.Default.Flat,
    resolveAssets: ResolveAssets,
    resolveText: ResolveText,
    resolveState: ResolveState,
    eventCallback: EventCallback,
) {
    val measuredContentHeightPxState = remember { mutableIntStateOf(0) }
    val measuredFooterHeightPxState = remember { mutableIntStateOf(0) }
    val adjustedContentHeightState = remember(
        measuredContentHeightPxState.intValue,
        measuredFooterHeightPxState.intValue,
    ) { mutableStateOf<Dp>(Dp.Unspecified) }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .clickable(enabled = false, onClick = {})
            .backgroundOrSkip(Shape.plain(defaultScreen.background), resolveAssets),
    ) {
        defaultScreen.cover?.render(
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
        val boxMaxHeightPx = constraints.maxHeight
        val density = LocalDensity.current
        val contentWrapper = defaultScreen.contentWrapper

        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
        ) {
            Box(
                contentAlignment = contentWrapper.contentAlign.toComposeAlignment(),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .run {
                        val adjustedContentHeight = adjustedContentHeightState.value
                        if (adjustedContentHeight != Dp.Unspecified)
                            return@run height(adjustedContentHeight)
                        val measuredFooterHeightPx = measuredFooterHeightPxState.intValue
                        if (measuredFooterHeightPx == 0) return@run this
                        val measuredContentHeightPx = measuredContentHeightPxState.intValue
                        if (measuredContentHeightPx == 0) return@run this
                        with(density) {
                            val contentHeight = calculateAdjustedContentHeightPx(
                                measuredContentHeightPx.toFloat(),
                                measuredFooterHeightPx.toFloat(),
                                boxMaxHeightPx.toFloat(),
                            ).toDp()
                            height(contentHeight)
                                .also { adjustedContentHeightState.value = contentHeight }
                        }
                    }
                    .backgroundOrSkip(contentWrapper.background, resolveAssets)
            ) {
                contentWrapper.content.render(
                    resolveAssets,
                    resolveText,
                    resolveState,
                    eventCallback,
                    Modifier
                        .onSizeChanged { size ->
                            if (size.height <= 0 || measuredContentHeightPxState.intValue == size.height)
                                return@onSizeChanged
                            measuredContentHeightPxState.intValue = size.height
                        }
                        .fillWithBaseParams(contentWrapper.content, resolveAssets),
                )
            }
        }

        defaultScreen.footer?.let { footer ->
            footer.render(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        if (size.height <= 0 || measuredFooterHeightPxState.intValue == size.height)
                            return@onSizeChanged
                        measuredFooterHeightPxState.intValue = size.height
                    }
                    .fillWithBaseParams(footer, resolveAssets),
            )
        }
        defaultScreen.overlay?.render(
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun renderTransparentTemplate(
    defaultScreen: Screen.Default.Transparent,
    resolveAssets: ResolveAssets,
    resolveText: ResolveText,
    resolveState: ResolveState,
    eventCallback: EventCallback,
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .clickable(enabled = false, onClick = {})
            .backgroundOrSkip(Shape.plain(defaultScreen.background), resolveAssets),
    ) {
        defaultScreen.content.render(
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
        defaultScreen.footer?.let { footer ->
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null,
            ) {
                footer.render(
                    resolveAssets,
                    resolveText,
                    resolveState,
                    eventCallback,
                    Modifier
                        .verticalScroll(rememberScrollState(), reverseScrolling = true)
                        .height(IntrinsicSize.Max)
                        .fillWithBaseParams(footer, resolveAssets)
                )
            }
        }
        defaultScreen.overlay?.render(
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
    }
}

private fun calculateAdjustedContentHeightPx(
    initialContentHeightPx: Float,
    footerHeightPx: Float,
    containerHeightPx: Float,
): Float {
    val footerOverlapPx = footerHeightPx - (containerHeightPx - initialContentHeightPx).coerceAtLeast(0f)
    return initialContentHeightPx.coerceAtLeast(containerHeightPx) + footerOverlapPx
}
