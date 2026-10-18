@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adapty.internal.utils.InternalAdaptyApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal object WheelColumnDefaults {
    val TextColor: Color = Color(0xFF999999)
    val SelectedTextColor: Color = Color.Black
    val IndicatorColor: Color = Color(0x1A000000)
    const val FontSize: Float = 16f
    const val IndicatorCornerRadius: Float = 8f
    const val ItemHeightDp: Float = 40f
    const val VisibleItems: Int = 5
}

private const val PickerMaxFlingVelocity = 30000f

@Composable
internal fun WheelColumn(
    labels: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemHeightDp: Float = WheelColumnDefaults.ItemHeightDp,
    visibleItems: Int = WheelColumnDefaults.VisibleItems,
    textColor: Color = WheelColumnDefaults.TextColor,
    selectedTextColor: Color = WheelColumnDefaults.SelectedTextColor,
    backgroundColor: Color? = null,
    textDecoration: TextDecoration? = null,
    indicatorColor: Color = WheelColumnDefaults.IndicatorColor,
    indicatorShape: Shape = RoundedCornerShape(WheelColumnDefaults.IndicatorCornerRadius.dp),
    fontFamily: FontFamily? = null,
    fontSize: TextUnit = WheelColumnDefaults.FontSize.sp,
) {
    if (labels.isEmpty()) return

    val visible = visibleItems.let { if (it % 2 == 0) it + 1 else it }.coerceAtLeast(1)
    val halfVisible = visible / 2

    val safeIndex = selectedIndex.coerceIn(0, labels.lastIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeIndex)
    val coroutineScope = rememberCoroutineScope()

    val totalHeightDp = itemHeightDp * visible
    val itemHeightPx = with(LocalDensity.current) { itemHeightDp.dp.toPx() }

    val centeredIndex by remember(listState, itemHeightPx) {
        derivedStateOf {
            if (itemHeightPx <= 0f) listState.firstVisibleItemIndex
            else {
                val pos = listState.firstVisibleItemIndex +
                    listState.firstVisibleItemScrollOffset / itemHeightPx
                pos.roundToInt()
            }
        }
    }

    val onSelectedIndexChangeState = rememberUpdatedState(onSelectedIndexChange)
    val safeIndexState = rememberUpdatedState(safeIndex)
    val labelsLastIndexState = rememberUpdatedState(labels.lastIndex)

    LaunchedEffect(listState) {
        snapshotFlow { centeredIndex to listState.isScrollInProgress }
            .filter { (_, scrolling) -> !scrolling }
            .map { (index, _) -> index.coerceIn(0, labelsLastIndexState.value) }
            .distinctUntilChanged()
            .collect { index ->
                if (index != safeIndexState.value) {
                    onSelectedIndexChangeState.value(index)
                }
            }
    }

    LaunchedEffect(safeIndex) {
        if (!listState.isScrollInProgress && centeredIndex != safeIndex) {
            listState.animateScrollToItem(safeIndex)
        }
    }

    Box(modifier = modifier.height(totalHeightDp.dp).clipToBounds()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .height(itemHeightDp.dp)
                .fillMaxWidth()
                .background(indicatorColor, indicatorShape),
        )

        LazyColumn(
            state = listState,
            flingBehavior = rememberPickerFlingBehavior(listState),
            userScrollEnabled = enabled,
            contentPadding = PaddingValues(vertical = (itemHeightDp * halfVisible).dp),
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!enabled) Modifier else Modifier.pointerInput(listState) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val wasScrolling = listState.isScrollInProgress
                            val stopJob = if (wasScrolling) {
                                coroutineScope.launch { listState.stopScroll() }
                            } else null
                            val up = waitForUpOrCancellation()
                            if (up != null && stopJob != null) {
                                coroutineScope.launch {
                                    stopJob.join()
                                    val target = centeredIndex
                                        .coerceIn(0, labelsLastIndexState.value)
                                    listState.animateScrollToItem(target)
                                }
                            }
                        }
                    }
                ),
        ) {
            items(labels.size) { index ->
                val centeredPosition = listState.firstVisibleItemIndex +
                    listState.firstVisibleItemScrollOffset / itemHeightPx
                val offsetFromCenter = index - centeredPosition
                val distanceFromCenter = abs(offsetFromCenter)
                val fraction = distanceFromCenter / halfVisible.coerceAtLeast(1)

                val maxAngle = 50f
                val maxAngleRad = Math.toRadians(maxAngle.toDouble()).toFloat()
                val angleX = (-offsetFromCenter / halfVisible.coerceAtLeast(1) * maxAngle)
                    .coerceIn(-80f, 80f)
                val angleRad = Math.toRadians(angleX.toDouble())
                val cylinderScale = cos(angleRad).toFloat()

                val translationAngle = (offsetFromCenter / halfVisible.coerceAtLeast(1) * maxAngle)
                    .coerceIn(-85f, 85f)
                val translationAngleRad = Math.toRadians(translationAngle.toDouble())
                val cylinderY = (halfVisible / maxAngleRad) * sin(translationAngleRad).toFloat() * itemHeightPx
                val linearY = offsetFromCenter * itemHeightPx
                val yCompression = cylinderY - linearY

                val alpha = lerpFloat(1f, 0.35f, fraction).coerceIn(0f, 1f)
                val isSelected = distanceFromCenter < 0.5f

                Box(
                    modifier = Modifier
                        .height(itemHeightDp.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            rotationX = angleX
                            cameraDistance = 8f * density
                            translationY = yCompression
                            scaleX = 1f - (1f - cylinderScale) * 0.15f
                            this.alpha = alpha
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Text(
                        text = labels[index],
                        modifier = if (backgroundColor != null)
                            Modifier.background(backgroundColor) else Modifier,
                        color = if (isSelected) selectedTextColor else textColor,
                        fontSize = fontSize,
                        fontFamily = fontFamily,
                        textAlign = TextAlign.Center,
                        textDecoration = textDecoration,
                    )
                }
            }
        }
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

@Composable
private fun rememberPickerFlingBehavior(
    listState: LazyListState,
): FlingBehavior {
    val snapFling = rememberSnapFlingBehavior(listState)
    return remember(snapFling) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val clamped = initialVelocity.coerceIn(-PickerMaxFlingVelocity, PickerMaxFlingVelocity)
                return with(snapFling) { this@performFling.performFling(clamped) }
            }
        }
    }
}
