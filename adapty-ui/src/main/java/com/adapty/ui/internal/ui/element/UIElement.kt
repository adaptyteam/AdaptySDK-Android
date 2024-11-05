package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.utils.EventCallback

@InternalAdaptyApi
@Stable
public interface UIElement {
    public val baseProps: BaseProps

    public fun toComposable(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit

    public fun RowScope.toComposableInRow(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit {
        return toComposable(
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
            baseProps.weight?.let { modifier.weight(it) } ?: modifier,
        )
    }

    public fun ColumnScope.toComposableInColumn(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit {
        return toComposable(
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
            baseProps.weight?.let { modifier.weight(it) } ?: modifier,
        )
    }
}