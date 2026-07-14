@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.mapping.element.Texts
import com.adapty.ui.internal.script.StateAccessor
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.store.ScrollCommand
import com.adapty.ui.internal.store.FocusCommand
import com.adapty.ui.internal.store.TimerSetCommand
import com.adapty.ui.internal.ui.element.ResolveAssets
import com.adapty.ui.internal.ui.element.ResolveState
import com.adapty.ui.internal.ui.element.ResolveText

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

internal val LocalResolveState = staticCompositionLocalOf<ResolveState> {
    @Composable { error("LocalResolveState not provided") }
}

internal val LocalResolveAssets = staticCompositionLocalOf<ResolveAssets> {
    error("LocalResolveAssets not provided")
}

internal val LocalResolveText = staticCompositionLocalOf<ResolveText> {
    @Composable { _, _ -> error("LocalResolveText not provided") }
}

@InternalAdaptyApi
@Composable
public fun resolveAssets(): Assets = LocalResolveAssets.current()

@InternalAdaptyApi
@Composable
public fun resolveState(): StateAccessor = LocalResolveState.current()

@InternalAdaptyApi
public val resolveText: ResolveText @Composable get() = LocalResolveText.current

internal val FAKE_NAVIGATOR_ENTRY = NavigationEntry(
    screenInstanceId = "fake",
    screenType = "fake",
    contextPath = null,
    navigatorId = "fake",
    transitionId = null,
)

internal val LocalScreenInstance = compositionLocalOf<NavigationEntry> {
    error("LocalScreenInstance not provided")
}

internal val LocalNavigatorEntry = compositionLocalOf<NavigationEntry> {
    error("LocalNavigatorEntry not provided")
}

internal val LocalScreenBundle = compositionLocalOf<com.adapty.ui.AdaptyUI.FlowConfiguration.ScreenBundle> {
    error("LocalScreenBundle not provided")
}

internal val LocalNavigatorConfig = compositionLocalOf<com.adapty.ui.AdaptyUI.FlowConfiguration.NavigatorConfig?> { null }

internal val LocalScrollCommand = compositionLocalOf<ScrollCommand?> { null }

internal val LocalTimerCommands = compositionLocalOf<Map<String, TimerSetCommand>> { emptyMap() }

internal val LocalTexts = staticCompositionLocalOf<Texts> {
    error("LocalTexts not provided")
}

internal val LocalFocusCommand = compositionLocalOf<FocusCommand?> { null }

internal val LocalCurrentFocusId = compositionLocalOf<String?> { null }

internal val LocalDispatch = compositionLocalOf<(Message) -> Unit> { {} }

internal val LocalUiEnabled = compositionLocalOf { true }
