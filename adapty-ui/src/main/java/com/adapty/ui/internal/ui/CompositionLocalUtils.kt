package com.adapty.ui.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal

@Composable
internal fun <T> ClearCompositionLocalProvider(
    local: ProvidableCompositionLocal<T?>,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(local provides null) {
        content()
    }
}

@Composable
internal fun ClearCompositionLocalProvider(
    vararg locals: ProvidableCompositionLocal<*>,
    content: @Composable () -> Unit,
) {
    val providedValues = locals.map { (it as ProvidableCompositionLocal<Any?>) provides null }.toTypedArray()
    CompositionLocalProvider(*providedValues) {
        content()
    }
}
