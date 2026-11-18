package com.adapty.ui.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

internal data class UnspecifiedAxes(val horizontal: Boolean, val vertical: Boolean) {

    fun declaring(horizontal: Boolean?, vertical: Boolean?): UnspecifiedAxes =
        UnspecifiedAxes(horizontal ?: this.horizontal, vertical ?: this.vertical)

    companion object {
        val None = UnspecifiedAxes(horizontal = false, vertical = false)
    }
}

internal val LocalUnspecifiedAxes = compositionLocalOf { UnspecifiedAxes.None }

@Composable
internal fun DeclaringLength(
    unspecifiedHorizontal: Boolean?,
    unspecifiedVertical: Boolean?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalUnspecifiedAxes provides
            LocalUnspecifiedAxes.current.declaring(unspecifiedHorizontal, unspecifiedVertical),
        content = content,
    )
}
