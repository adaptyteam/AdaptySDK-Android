package com.adapty.ui.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adapty.ui.internal.utils.LOADING_BG_COLOR
import com.adapty.ui.internal.utils.LOADING_SIZE
import com.adapty.ui.internal.utils.getProgressCustomColorOrNull

@Composable
internal fun Loading(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = false, onClick = {})
            .background(Color(LOADING_BG_COLOR))
    ) {
        val context = LocalContext.current

        val circularColor = remember {
            context.getProgressCustomColorOrNull()?.let { Color(it) }
        }

        CircularProgressIndicator(
            modifier = Modifier.size(LOADING_SIZE.dp),
            color = circularColor ?: ProgressIndicatorDefaults.circularColor,
        )
    }
}