@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import com.adapty.ui.internal.ui.LocalFocusCommand
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyCustomElementContent
import com.adapty.ui.AdaptyCustomElementContext
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUiAsset
import com.adapty.ui.CustomElementsRegistry
import com.adapty.ui.internal.store.Effect
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.toAnnotatedString
import com.adapty.ui.internal.text.toSpanned
import com.adapty.ui.internal.ui.IntrinsicSafeAndroidView
import com.adapty.ui.internal.ui.IntrinsicSafeCustomContent
import com.adapty.ui.internal.ui.LocalCustomElementsHost
import com.adapty.ui.internal.ui.LocalKeepPagesComposed
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.LocalShowsDebugPlaceholders
import com.adapty.ui.internal.ui.LocalUiEnabled
import com.adapty.ui.internal.ui.consumePointerInput
import com.adapty.ui.internal.ui.resolveAssets
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.ui.resolveText
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.internal.utils.VIDEO_PREVIEW_ASSET_SUFFIX
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.parseColorInt
import com.adapty.ui.internal.utils.resolve
import com.adapty.ui.toUiAsset
import com.adapty.utils.AdaptyLogLevel.Companion.WARN

@InternalAdaptyApi
public class CustomElement internal constructor(
    internal val customId: String,
    internal val customType: String,
    internal val assets: Map<String, VisualValue>?,
    internal val strings: Map<String, StringId>?,
    internal val bindings: Map<String, TwoWayBinding>?,
    internal val payload: String?,
    internal val fallback: UIElement?,
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val host = LocalCustomElementsHost.current
        val focusedModifier = modifier.customElementFocus(baseProps.focusId, dispatch)
        val content = CustomElementsRegistry.resolve(host?.resolver, customType, customId)
        if (content == null || host == null) {
            renderUnserved(dispatch, focusedModifier)
        } else {
            key(customId) {
                renderContent(content, host.sendAppMessage, dispatch, focusedModifier)
            }
        }
    }

    @Composable
    private fun renderContent(
        content: AdaptyCustomElementContent,
        sendAppMessage: (String, Map<String, Any?>, Effect.SendSDKEvent.AppMessage.Source) -> Unit,
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ) {
        val screen = LocalScreenInstance.current
        val stateAccessor = resolveState()
        val context = remember(stateAccessor, screen) {
            AdaptyCustomElementContext(this, stateAccessor, screen, dispatch, sendAppMessage)
        }
        context.compositionContext = rememberCompositionContext()
        val isEnabled = LocalUiEnabled.current
        context.enabled = isEnabled
        context.assets = resolveUiAssets()
        val strings = resolveStrings()
        context.strings = strings
        val density = LocalDensity.current
        val fontFamilyResolver = LocalFontFamilyResolver.current
        context.spannedStrings = remember(strings, density, fontFamilyResolver) {
            strings.mapValues { (_, string) ->
                string.toSpanned(density) { family -> fontFamilyResolver.resolve(family).value as? Typeface }
            }
        }
        DisposableEffect(context) {
            context.isDisposed = false
            onDispose { context.isDisposed = true }
        }
        val keepPagesComposed = LocalKeepPagesComposed.current
        if (keepPagesComposed != null) SideEffect { keepPagesComposed() }

        val gatedModifier = if (isEnabled) modifier else modifier.consumePointerInput()
        when (content) {
            is AdaptyCustomElementContent.Compose -> IntrinsicSafeCustomContent(gatedModifier, ::warnSizeUnknown) {
                content.content(context)
            }
            is AdaptyCustomElementContent.View -> IntrinsicSafeAndroidView(
                factory = { viewContext -> content.factory(viewContext, context) },
                modifier = gatedModifier,
                update = { view -> content.update(view, context) },
            )
        }
    }

    private fun warnSizeUnknown() {
        log(WARN) {
            "$LOG_PREFIX custom element $customType:$customId content does not report its size (a lazy list, " +
                "BoxWithConstraints), so containers that size by content get 0 for it; give it a height in the " +
                "configuration or a fixed size on the content"
        }
    }

    @Composable
    private fun renderUnserved(dispatch: (Message) -> Unit, modifier: Modifier) {
        LaunchedEffect(Unit) {
            log(WARN) { "$LOG_PREFIX custom element $customType:$customId is not served by a resolver" }
        }
        if (fallback != null) {
            Box(modifier) {
                fallback.render(dispatch)
            }
        } else if (LocalShowsDebugPlaceholders.current) {
            Box(modifier) {
                BasicText(
                    text = "custom $customType:$customId",
                    modifier = Modifier
                        .background(PlaceholderBackground, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
        } else {
            Spacer(modifier = modifier)
        }
    }

    @Composable
    private fun resolveUiAssets(): Map<String, AdaptyUiAsset> {
        val result = mutableMapOf<String, AdaptyUiAsset>()
        assets?.forEach { (assetKey, value) ->
            key(assetKey) {
                value.resolveUiAsset()?.let { result[assetKey] = it }
            }
        }
        return result
    }

    @Composable
    private fun resolveStrings(): Map<String, AnnotatedString> {
        val result = mutableMapOf<String, AnnotatedString>()
        strings?.forEach { (stringKey, stringId) ->
            key(stringKey) {
                val wrapper = resolveText(stringId, null)
                remember(wrapper) { wrapper?.toAnnotatedString() }?.let { result[stringKey] = it }
            }
        }
        return result
    }

    private companion object {
        val PlaceholderBackground = Color(0xFFFF3B30).copy(alpha = 0.7f)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun Modifier.customElementFocus(focusId: String?, dispatch: (Message) -> Unit): Modifier {
    if (focusId == null) return this
    val focusRequester = remember(focusId) { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val hasFocus = remember { mutableStateOf(false) }

    val focusCommand = LocalFocusCommand.current
    if (focusCommand?.focusId == focusId) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val hostView = LocalView.current
        LaunchedEffect(focusCommand) {
            runCatching { focusRequester.requestFocus() }
            withFrameNanos {}
            if (hasFocus.value) {
                hostView.focusedViewEditor()?.let { editor -> hostView.inputMethodManager()?.showSoftInput(editor, 0) }
            } else {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
            dispatch(Message.FocusCommandConsumed)
        }
    }

    val viewEditorFocused = remember { mutableStateOf(false) }
    val focusView = LocalView.current
    LaunchedEffect(hasFocus.value) {
        if (hasFocus.value) {
            withFrameNanos {}
            viewEditorFocused.value = focusView.focusedViewEditor() != null
        } else if (viewEditorFocused.value) {
            viewEditorFocused.value = false
            withFrameNanos {}
            if (focusView.findFocus()?.onCheckIsTextEditor() != true) {
                focusView.inputMethodManager()?.hideSoftInputFromWindow(focusView.windowToken, 0)
            }
        }
    }

    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    LaunchedEffect(hasFocus.value, density) {
        if (!hasFocus.value) return@LaunchedEffect
        snapshotFlow { ime.getBottom(density).takeIf { it == imeTarget.getBottom(density) } }
            .filterNotNull()
            .distinctUntilChanged()
            .collect {
                withFrameNanos {}
                runCatching { bringIntoViewRequester.bringIntoView() }
            }
    }

    return this
        .bringIntoViewRequester(bringIntoViewRequester)
        .focusRequester(focusRequester)
        .onFocusChanged { state ->
            if (state.hasFocus == hasFocus.value) return@onFocusChanged
            hasFocus.value = state.hasFocus
            dispatch(if (state.hasFocus) Message.FocusChanged(focusId) else Message.FocusLost(focusId))
        }
}

private fun View.focusedViewEditor(): View? = findFocus()?.takeIf { it !== this && it.onCheckIsTextEditor() }

private fun View.inputMethodManager(): InputMethodManager? =
    context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

@Composable
private fun VisualValue.resolveUiAsset(): AdaptyUiAsset? {
    val value = source.resolve() ?: return null
    return when (orderedTypes.firstOrNull { it.condition(value) }) {
        VisualValue.Type.ColorLiteral -> remember(value) { AdaptyUiAsset.Color(value.parseColorInt()) }
        VisualValue.Type.AssetId -> {
            val assets = resolveAssets()
            val asset = assets.getAsset<Asset>(value)?.declaredKindOrConfig()
            val preview =
                if (asset is Asset.Video) assets.getAsset<Asset.Image>("$value$VIDEO_PREVIEW_ASSET_SUFFIX")?.main
                else null
            remember(asset, preview) {
                val uiAsset = asset?.toUiAsset()
                if (uiAsset is AdaptyUiAsset.Video && preview != null)
                    AdaptyUiAsset.Video(uiAsset.source, uiAsset.width, uiAsset.height, preview.toUiAsset() as? AdaptyUiAsset.Image.Local)
                else uiAsset
            }
        }
        null -> null
    }
}

@Composable
private fun Asset.Composite<Asset>.declaredKindOrConfig(): Asset {
    val config = fallback ?: return main
    if (main.kind == config.kind) return main
    LaunchedEffect(main, config) {
        log(WARN) {
            "$LOG_PREFIX asset ${config.customId} from resolver is not of the kind declared by configuration, using configuration asset"
        }
    }
    return config
}

private val Asset.kind: Any
    get() = when (this) {
        is Asset.Image, is Asset.RemoteImage -> Asset.Image::class
        else -> this::class
    }
