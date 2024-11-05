package com.adapty.ui.internal.ui

import android.util.TypedValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adapty.ui.R
import com.adapty.ui.internal.utils.LOADING_BG_COLOR
import com.adapty.ui.internal.utils.LOADING_SIZE

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
            val typedValue = TypedValue()
            if (context.theme.resolveAttribute(R.attr.adapty_progressIndicatorColor, typedValue, true)) {
                kotlin.runCatching { Color(typedValue.data) }.getOrNull()
            } else {
                null
            }
        }

        CircularProgressIndicator(
            modifier = Modifier.width(LOADING_SIZE.dp),
            color = circularColor ?: ProgressIndicatorDefaults.circularColor,
        )
    }
}