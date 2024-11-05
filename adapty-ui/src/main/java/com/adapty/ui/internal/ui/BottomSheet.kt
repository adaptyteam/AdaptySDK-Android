@file:OptIn(ExperimentalMaterial3Api::class)

package com.adapty.ui.internal.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape

@Composable
internal fun BottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0,0,0,0) },
        content = content,
    )
}

@Composable
internal fun rememberBottomSheetState() = rememberModalBottomSheetState(skipPartiallyExpanded = true)