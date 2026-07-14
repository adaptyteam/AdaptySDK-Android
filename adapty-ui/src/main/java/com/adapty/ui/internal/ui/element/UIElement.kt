@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Message

@InternalAdaptyApi
@Stable
public interface UIElement {
    public val baseProps: BaseProps
    public val layoutRelevantProps: BaseProps get() = baseProps

    @Composable
    public fun layoutRelevantPropsResolved(): BaseProps = layoutRelevantProps

    public fun anyLayoutVariant(predicate: (UIElement) -> Boolean): Boolean = predicate(this)

    public fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit

    public fun RowScope.toComposableInRow(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit {
        return toComposable(
            dispatch,
            baseProps.weight?.let { modifier.weight(it) } ?: modifier,
        )
    }

    public fun ColumnScope.toComposableInColumn(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit {
        return toComposable(
            dispatch,
            baseProps.weight?.let { modifier.weight(it) } ?: modifier,
        )
    }
}