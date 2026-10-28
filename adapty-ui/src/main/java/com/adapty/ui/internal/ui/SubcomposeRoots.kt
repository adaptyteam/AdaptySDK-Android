package com.adapty.ui.internal.ui

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun IntrinsicSafeBoxWithConstraints(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    val record = remember { NaturalHeightRecord() }
    BoxWithConstraints(
        modifier = modifier.zeroIntrinsicsOrRecordedHeight(record),
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints,
    ) {
        val scope = this
        Box(
            modifier = Modifier.recordNaturalHeight(record),
            contentAlignment = contentAlignment,
            propagateMinConstraints = true,
        ) {
            scope.content()
        }
    }
}

@Composable
internal fun IntrinsicSafeLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.then(ZeroIntrinsicsModifier),
        state = state,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content,
    )
}

@Composable
internal fun IntrinsicSafeHorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = 0,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    userScrollEnabled: Boolean = true,
    snapPosition: SnapPosition = SnapPosition.Start,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    val record = remember { NaturalHeightRecord() }
    val pageHeights = remember { HashMap<Int, Int>() }
    val density = LocalDensity.current
    val paddingPx = with(density) {
        (contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()).roundToPx()
    }
    HorizontalPager(
        state = state,
        modifier = modifier.zeroIntrinsicsOrRecordedHeight(record),
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        verticalAlignment = verticalAlignment,
        userScrollEnabled = userScrollEnabled,
        snapPosition = snapPosition,
    ) { page ->
        val scope = this
        Box(
            modifier = Modifier.recordNaturalHeight(record, paddingPx) { height ->
                pageHeights[page] = height
                record.write(pageHeights.values.max())
            },
            propagateMinConstraints = true,
        ) {
            scope.pageContent(page)
        }
    }
}
