package com.adapty.ui.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.adapty.ui.internal.text.MissingTagKind

internal class DebugPlaceholders(
    val missingTag: (tag: String, kind: MissingTagKind) -> String?,
)

@Composable
internal fun ProvideDebugPlaceholders(debugPlaceholders: DebugPlaceholders?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalShowsDebugPlaceholders provides (debugPlaceholders != null), content = content)
}
