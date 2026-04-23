package dev.nemeyes.nextvideo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.roundToInt

/**
 * Simple draggable fast-scroll thumb for a LazyColumn.
 *
 * Not an "accurate" scrollbar; it maps drag position to item index for quick navigation.
 */
@Composable
fun FastScrollBar(
    state: LazyListState,
    totalItems: Int,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color(0x660082C9),
    thumbWidthDp: Int = 6,
    thumbHeightDp: Int = 44,
) {
    if (totalItems <= 0) return

    val density = LocalDensity.current
    val thumbHeightPx = with(density) { thumbHeightDp.dp.toPx() }

    val firstVisibleIndex by remember(state) { derivedStateOf { state.firstVisibleItemIndex } }
    val maxIndex = (totalItems - 1).coerceAtLeast(0)
    val progress =
        remember(firstVisibleIndex, maxIndex) {
            if (maxIndex == 0) 0f else (firstVisibleIndex.toFloat() / maxIndex.toFloat()).coerceIn(0f, 1f)
        }

    var containerHeightPx = remember { 0 }

    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .width(thumbWidthDp.dp)
                .onSizeChanged { containerHeightPx = it.height }
                .pointerInput(totalItems, containerHeightPx) {
                    detectVerticalDragGestures(
                        onDragStart = { start: Offset ->
                            if (containerHeightPx <= 0) return@detectVerticalDragGestures
                            val p = (start.y / containerHeightPx.toFloat()).coerceIn(0f, 1f)
                            val idx = (p * maxIndex).roundToInt().coerceIn(0, maxIndex)
                            state.requestScrollToItem(idx)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            if (containerHeightPx <= 0) return@detectVerticalDragGestures
                            val p = (change.position.y / containerHeightPx.toFloat()).coerceIn(0f, 1f)
                            val idx = (p * maxIndex).roundToInt().coerceIn(0, maxIndex)
                            state.requestScrollToItem(idx)
                        },
                    )
                },
        contentAlignment = Alignment.TopCenter,
    ) {
        val yPx =
            ((containerHeightPx.toFloat() - thumbHeightPx).coerceAtLeast(0f) * progress)
                .roundToInt()

        Box(
            modifier =
                Modifier
                    .offset { IntOffset(0, yPx) }
                    .width(thumbWidthDp.dp)
                    .height(thumbHeightDp.dp)
                    .background(thumbColor, RoundedCornerShape(999.dp))
        )
    }
}

