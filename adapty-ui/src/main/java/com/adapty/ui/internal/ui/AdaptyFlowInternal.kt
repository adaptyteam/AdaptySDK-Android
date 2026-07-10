@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adapty.ui.internal.ui.attributes.Shape
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.Insets
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.ui.element.render
import com.adapty.ui.internal.ui.event.LocalEventDispatcher
import com.adapty.ui.internal.utils.InsetWrapper
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LocalCustomInsets
import com.adapty.ui.internal.utils.LocalSafeAreaInsets
import com.adapty.ui.internal.utils.LocalScreenDimensions
import com.adapty.ui.internal.utils.getRtlAware
import com.adapty.ui.internal.utils.computeSafeAreaInsets
import com.adapty.ui.internal.utils.ScreenDimensions
import com.adapty.ui.internal.utils.getActivityOrNull
import com.adapty.ui.internal.utils.getInsets
import com.adapty.ui.internal.listeners.ContextAwareEventListener
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.rootBarOrCutoutInsets
import com.adapty.ui.internal.utils.wrap
import com.adapty.ui.internal.script.ActionHandler
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.AppearanceAnimation
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.utils.resolveAsset
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN

@InternalAdaptyApi
public data class NavigationEntry(
    val screenInstanceId: String,
    val screenType: String,
    val contextPath: String?,
    val navigatorId: String = "default",
    val transitionId: String? = null,
    val epoch: Long = nextEpoch(),
) {
    internal companion object {
        private val counter = java.util.concurrent.atomic.AtomicLong(0)
        internal fun nextEpoch(): Long = counter.incrementAndGet()
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdaptyFlowInternal(viewModel: FlowViewModel) {
    val userArgs = viewModel.dataState.value ?: return
    val viewConfig = userArgs.viewConfig
    CompositionLocalProvider(
        LocalLayoutDirection provides if (viewConfig.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalCustomInsets provides userArgs.userInsets.wrap(),
    ) {
        val insets = getInsets()
        Box {
            BoxWithConstraints {
                val density = LocalDensity.current
                val configuration = LocalConfiguration.current
                val screenHeightPxFromConfig: Int
                val maxHeightPxFromConstraints: Int
                with(density) {
                    screenHeightPxFromConfig = configuration.screenHeightDp.dp.roundToPx()
                    maxHeightPxFromConstraints = maxHeight.roundToPx()
                }
                var loggedNonSkipping by remember { mutableStateOf(false) }
                val view = LocalView.current
                val layoutDirection = LocalLayoutDirection.current
                if (!insets.isCustom) {
                    val composeInsetsEmpty = insets.getTop(density) == 0 &&
                        insets.getBottom(density) == 0 &&
                        insets.getLeft(density, layoutDirection) == 0 &&
                        insets.getRight(density, layoutDirection) == 0
                    val rootInsets = if (composeInsetsEmpty) view.rootBarOrCutoutInsets() else null
                    val insetsNotDeliveredYet = composeInsetsEmpty &&
                        when (rootInsets) {
                            null -> maxHeightPxFromConstraints - screenHeightPxFromConfig > 10
                            else -> rootInsets != Insets.NONE
                        }
                    if (insetsNotDeliveredYet) {
                        log(VERBOSE) { "$LOG_PREFIX skipping (rootInsets: $rootInsets; $screenHeightPxFromConfig; $maxHeightPxFromConstraints)" }
                        return@BoxWithConstraints
                    } else {
                        if (!loggedNonSkipping) {
                            log(VERBOSE) { "$LOG_PREFIX non-skipping (${insets.getTop(density)}; ${insets.getBottom(density)}; $screenHeightPxFromConfig; $maxHeightPxFromConstraints)" }
                            loggedNonSkipping = true
                        }
                    }
                } else {
                    if (!loggedNonSkipping) {
                        log(VERBOSE) { "$LOG_PREFIX non-skipping (custom insets: ${(insets as? InsetWrapper.Custom)?.insets}" }
                        loggedNonSkipping = true
                    }
                }

                val safeAreaInsets = computeSafeAreaInsets(density, layoutDirection)
                val screenDimensions = ScreenDimensions(maxWidth.value, maxHeight.value)

                val derivedAssets = remember { derivedStateOf { viewModel.state?.assets?.items ?: emptyMap() } }
                val derivedTexts = remember { derivedStateOf { viewModel.state?.texts?.items ?: emptyMap() } }
                val derivedProducts = remember { derivedStateOf { viewModel.state?.products?.items ?: emptyMap() } }
                val derivedScrollCommand = remember { derivedStateOf { viewModel.state?.ui?.scrollCommand } }
                val derivedTimerCommands = remember { derivedStateOf { viewModel.state?.ui?.timerCommands ?: emptyMap() } }
                val derivedFocusCommand = remember { derivedStateOf { viewModel.state?.ui?.focusCommand } }
                val derivedCurrentFocusId = remember { derivedStateOf { viewModel.state?.ui?.currentFocusId } }

                val dispatch: (Message) -> Unit = remember(viewModel) { { msg -> viewModel.dispatch(msg) } }

                val focusCommand = derivedFocusCommand.value
                if (focusCommand != null) {
                    val focusManager = LocalFocusManager.current
                    val keyboardController = LocalSoftwareKeyboardController.current
                    @OptIn(ExperimentalLayoutApi::class)
                    val imeVisible = rememberUpdatedState(WindowInsets.isImeVisible)
                    LaunchedEffect(focusCommand) {
                        if (focusCommand.focusId != null) {
                            repeat(2) { withFrameNanos {} }
                            if (viewModel.state?.ui?.focusCommand != focusCommand) return@LaunchedEffect
                        }
                        keyboardController?.hide()
                        withTimeoutOrNull(500) {
                            snapshotFlow { imeVisible.value }.first { !it }
                        }
                        focusManager.clearFocus()
                        dispatch(Message.FocusCommandConsumed)
                    }
                }

                CompositionLocalProvider(
                    LocalSafeAreaInsets provides safeAreaInsets,
                    LocalScreenDimensions provides screenDimensions,
                    LocalScrollCommand provides derivedScrollCommand.value,
                    LocalTimerCommands provides derivedTimerCommands.value,
                    LocalFocusCommand provides derivedFocusCommand.value,
                    LocalCurrentFocusId provides derivedCurrentFocusId.value,
                    LocalDispatch provides dispatch,
                    LocalResolveAssets provides { derivedAssets.value },
                    LocalTexts provides derivedTexts.value,
                    LocalResolveText provides @Composable { stringId: StringId, textAttrs: Attributes? ->
                        viewModel.resolveTextWith(stringId, textAttrs, derivedTexts.value, derivedProducts.value, derivedAssets.value)
                    },
                    LocalResolveState provides @Composable { viewModel.resolveState() },
                    LocalEventDispatcher provides viewModel.eventDispatcher,
                ) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    val contextAwareListener = remember(viewModel) {
                        ContextAwareEventListener(
                            delegate = userArgs.eventListener,
                            contextProvider = { context },
                        )
                    }
                    viewModel.setContextAwareListener(contextAwareListener)

                    DisposableEffect(viewModel) {
                        viewModel.activityProvider = { context.getActivityOrNull()!! }
                        onDispose {
                            viewModel.activityProvider = null
                            viewModel.setContextAwareListener(null)
                        }
                    }

                    ForceAdjustResize()

                    createActionHandler(viewModel, dispatch)

                    val navigatorConfigs = viewConfig.navigators
                    val navEntries by remember {
                        derivedStateOf { viewModel.state?.navigation?.entries ?: emptyMap() }
                    }
                    val closingEntries by remember {
                        derivedStateOf { viewModel.state?.navigation?.closingEntries ?: emptyMap() }
                    }
                    val visibleNavigators by remember {
                        derivedStateOf {
                            val active = navEntries.mapNotNull { (navId, entry) ->
                                navigatorConfigs[navId]?.let { config ->
                                    VisibleNavigator(navId, config, entry, isClosing = false, closingKey = null)
                                }
                            }
                            val closing = closingEntries.mapNotNull { (navId, closing) ->
                                navigatorConfigs[navId]?.let { config ->
                                    VisibleNavigator(navId, config, closing.entry, isClosing = true, closingKey = closing.appearanceKey)
                                }
                            }
                            (active + closing).distinctBy { it.navId }.sortedBy { it.config.order }
                        }
                    }

                    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                    DisposableEffect(backDispatcher, viewModel) {
                        val callback = object : OnBackPressedCallback(true) {
                            override fun handleOnBackPressed() {
                                if (viewModel.isNotifyingClose) {
                                    isEnabled = false
                                    backDispatcher?.onBackPressed()
                                    isEnabled = true
                                    return
                                }
                                val top = viewModel.state?.navigation?.entries?.entries
                                    ?.mapNotNull { (navId, entry) -> navigatorConfigs[navId]?.let { Triple(navId, it, entry) } }
                                    ?.maxByOrNull { (_, config, _) -> config.order }
                                if (top != null) {
                                    val (navId, navConfig, navEntry) = top
                                    val screenConfig = viewConfig.screens.screens[navEntry.screenType]
                                    val backActions = screenConfig?.onSystemBack ?: navConfig.onSystemBack
                                    if (backActions != null) {
                                        isEnabled = false
                                        backActions.forEach { dispatch(backActionToMessage(it, navId)) }
                                        isEnabled = true
                                        return
                                    }
                                }

                                val handled = viewModel.contextAwareListener
                                    ?.let { it.onBackPressed(it.context) } ?: true
                                if (!handled) {
                                    isEnabled = false
                                    backDispatcher?.onBackPressed()
                                    isEnabled = true
                                }
                            }
                        }
                        backDispatcher?.addCallback(callback)
                        onDispose { callback.remove() }
                    }

                    visibleNavigators.forEach { visible ->
                        key(visible.navId) {
                            NavigatorLayer(
                                visible = visible,
                                dispatch = dispatch,
                                screenBundle = viewConfig.screens,
                            )
                        }
                    }

                    OnScreenLifecycle(
                        key = Unit,
                        onEnter = { viewModel.dispatch(Message.FlowEntered) },
                        onExit = {
                            viewModel.dispatch(Message.FlowExited)
                        },
                    )
                }
            }

            val isLoading by remember {
                derivedStateOf { viewModel.state?.ui?.isLoading ?: false }
            }
            if (isLoading)
                Loading()

            val alertDialogState by remember {
                derivedStateOf { viewModel.state?.ui?.alertDialog }
            }
            alertDialogState?.let { alert ->
                AlertDialogHost(alert) { actionId ->
                    viewModel.dispatch(Message.AlertDialogResolved(alert.callbackId, actionId))
                }
            }
        }
    }
}

@Composable
private fun AlertDialogHost(
    alert: com.adapty.ui.internal.store.AlertDialogState,
    onResolved: (actionId: String?) -> Unit,
) {
    val cancelAction = alert.actions.firstOrNull {
        it.style == Message.JSCallback.ShowAlertDialog.AlertAction.Style.CANCEL
    }
    val nonCancelActions = alert.actions.filter { it !== cancelAction }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onResolved(null) },
        title = alert.title?.takeIf { it.isNotEmpty() }?.let {
            { androidx.compose.material3.Text(it) }
        },
        text = alert.message?.takeIf { it.isNotEmpty() }?.let {
            { androidx.compose.material3.Text(it) }
        },
        confirmButton = {
            if (alert.actions.isEmpty()) {
                androidx.compose.material3.TextButton(onClick = { onResolved(null) }) {
                    androidx.compose.material3.Text("OK")
                }
            } else {
                androidx.compose.foundation.layout.Row {
                    nonCancelActions.forEach { action ->
                        AlertActionButton(action, onResolved)
                    }
                }
            }
        },
        dismissButton = cancelAction?.let { action ->
            { AlertActionButton(action, onResolved) }
        },
    )
}

@Composable
private fun AlertActionButton(
    action: Message.JSCallback.ShowAlertDialog.AlertAction,
    onResolved: (actionId: String?) -> Unit,
) {
    val colors = when (action.style) {
        Message.JSCallback.ShowAlertDialog.AlertAction.Style.DESTRUCTIVE ->
            androidx.compose.material3.ButtonDefaults.textButtonColors(
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error
            )
        else -> androidx.compose.material3.ButtonDefaults.textButtonColors()
    }
    androidx.compose.material3.TextButton(
        onClick = { onResolved(action.actionId) },
        colors = colors,
    ) {
        androidx.compose.material3.Text(action.title.orEmpty())
    }
}

@Composable
internal fun OnScreenLifecycle(
    key: Any?,
    onEnter: () -> Unit,
    onExit: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val hasAppeared = rememberSaveable(key) { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (!hasAppeared.value) {
                        hasAppeared.value = true
                        onEnter()
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            val isChangingConfig = context.getActivityOrNull()?.isChangingConfigurations ?: false
            if (!isChangingConfig) {
                onExit()
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun createActionHandler(
    viewModel: FlowViewModel,
    dispatch: (Message) -> Unit,
) {
    val actionHandler = remember(viewModel) {
        object : ActionHandler {
            override fun onOpenUrl(source: Message.JSCallback.OpenUrl.Source, openIn: String?) =
                dispatch(Message.JSCallback.OpenUrl(source, openIn))

            override fun onUserCustomAction(userCustomId: String) =
                dispatch(Message.JSCallback.CustomAction(userCustomId))

            override fun onPurchaseProduct(productId: String, paywallId: String?, callbackId: String?) =
                dispatch(Message.JSCallback.PurchaseProduct(productId, paywallId, callbackId))

            override fun onWebPurchaseProduct(productId: String, paywallId: String?, openIn: String?, callbackId: String?) =
                dispatch(Message.JSCallback.WebPurchaseProduct(productId, paywallId, openIn, callbackId))

            override fun onRestorePurchases(callbackId: String?) =
                dispatch(Message.JSCallback.RestorePurchases(callbackId))

            override fun onCloseAll() =
                dispatch(Message.JSCallback.CloseAll)

            override fun onSelectProduct(productId: String, paywallId: String?) =
                dispatch(Message.JSCallback.SelectProduct(productId, paywallId))

            override fun onOpenScreen(navEntry: NavigationEntry) =
                dispatch(Message.JSCallback.OpenScreen(navEntry))

            override fun onCloseScreen(navigatorId: String, transitionId: String) =
                dispatch(Message.JSCallback.CloseScreen(navigatorId, transitionId))

            override fun onMoveScroll(instanceId: String, kind: String, value: String) =
                dispatch(Message.JSCallback.MoveScroll(instanceId, kind, value))

            override fun onSetTimer(timerId: String, endAtMs: Long?, durationSeconds: Long?, behavior: String?) =
                dispatch(Message.JSCallback.SetTimer(timerId, endAtMs, durationSeconds, behavior))

            override fun onChangeFocus(focusId: String?) =
                dispatch(Message.JSCallback.ChangeFocus(focusId))

            override fun onSendAnalyticsEvent(name: String, params: Map<String, Any?>) =
                dispatch(Message.JSCallback.SendAnalyticsEvent(name, params))

            override fun onSendEvents(instanceId: String?, eventIds: List<String>) =
                dispatch(Message.JSCallback.SendEvents(instanceId, eventIds))

            override fun onShowAppRate() =
                dispatch(Message.JSCallback.ShowAppRate)

            override fun onShowAlertDialog(
                title: String?,
                message: String?,
                actions: List<Message.JSCallback.ShowAlertDialog.AlertAction>,
                callbackId: String?,
            ) = dispatch(Message.JSCallback.ShowAlertDialog(title, message, actions, callbackId))

            override fun onShowRequestPermission(
                permission: String?,
                customArgs: Map<String, String>?,
                callbackId: String?,
            ) = dispatch(Message.JSCallback.ShowRequestPermission(permission, customArgs, callbackId))

            override fun onJsError(message: String) =
                dispatch(Message.JSError(message))
        }
    }

    viewModel.setActionHandler(actionHandler)
}

internal data class VisibleNavigator(
    val navId: String,
    val config: AdaptyUI.FlowConfiguration.NavigatorConfig,
    val entry: NavigationEntry,
    val isClosing: Boolean,
    val closingKey: String?,
)

private fun backActionToMessage(action: Action, navigatorId: String): Message {
    return when (action.func) {
        "SDK.closeScreen" -> {
            val navId = (action.params["navigatorId"] as? String) ?: navigatorId
            Message.JSCallback.CloseScreen(navId, "on_disappear")
        }
        "SDK.closeAll" -> Message.JSCallback.CloseAll
        else -> {
            log(WARN) { "$LOG_PREFIX Unknown on_system_back func: ${action.func}, falling back to closeAll" }
            Message.JSCallback.CloseAll
        }
    }
}

internal enum class LifecycleEvent { WILL_APPEAR, DID_APPEAR, WILL_DISAPPEAR, DID_DISAPPEAR }

internal fun resolveScreenLifecycle(
    event: LifecycleEvent,
    entry: NavigationEntry,
    screenBundle: AdaptyUI.FlowConfiguration.ScreenBundle,
    navigatorConfig: AdaptyUI.FlowConfiguration.NavigatorConfig,
): List<Action>? {
    val screen = screenBundle.screens[entry.screenType]
    val perScreen = when (event) {
        LifecycleEvent.WILL_APPEAR -> screen?.onWillAppear
        LifecycleEvent.DID_APPEAR -> screen?.onDidAppear
        LifecycleEvent.WILL_DISAPPEAR -> screen?.onWillDisappear
        LifecycleEvent.DID_DISAPPEAR -> screen?.onDidDisappear
    }
    if (perScreen != null) return perScreen
    return when (event) {
        LifecycleEvent.WILL_APPEAR -> navigatorConfig.onWillAppear
        LifecycleEvent.DID_APPEAR -> navigatorConfig.onDidAppear
        LifecycleEvent.WILL_DISAPPEAR -> navigatorConfig.onWillDisappear
        LifecycleEvent.DID_DISAPPEAR -> navigatorConfig.onDidDisappear
    }
}

@Composable
private fun NavigatorLayer(
    visible: VisibleNavigator,
    dispatch: (Message) -> Unit,
    screenBundle: AdaptyUI.FlowConfiguration.ScreenBundle,
) {
    val config = visible.config
    val entry = visible.entry
    val navId = visible.navId
    val eventDispatcher = LocalEventDispatcher.current

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val appearKey = remember(navId) { entry.transitionId ?: "on_appear" }
    val appearanceAnim: AppearanceAnimation? = if (visible.isClosing) {
        visible.closingKey?.let { config.appearance.getRtlAware(it, isRtl) }
    } else {
        config.appearance.getRtlAware(appearKey, isRtl)
    }

    var hasAppeared by remember(navId) { mutableStateOf(false) }

    val bgComposite = config.background.resolveAsset<Asset.Filling.Local>()
    val staticBgColor = bgComposite?.castOrNull<Asset.Color>()?.toComposeFill()?.color
        ?: if (bgComposite == null) androidx.compose.ui.graphics.Color.Black
           else androidx.compose.ui.graphics.Color.Transparent
    val bgShape = remember(config.background) {
        Shape(
            fill = config.background,
            type = Shape.Type.Rectangle(null),
            border = null,
            shadow = null,
            innerShadow = null,
        )
    }
    val mediaBackgroundModifier: Modifier = when (bgComposite?.main) {
        is Asset.Gradient, is Asset.Image -> Modifier.backgroundOrSkip(bgShape)
        else -> Modifier
    }

    val bgColorState = if (!hasAppeared && !visible.isClosing && appearanceAnim?.background != null) {
        animateBackgroundColor(appearanceAnim.background, staticBgColor)
    } else if (visible.isClosing && appearanceAnim?.background != null) {
        animateBackgroundColor(appearanceAnim.background, staticBgColor)
    } else {
        remember(staticBgColor) { androidx.compose.runtime.mutableStateOf(staticBgColor) }
    }

    val contentAnimModifier = if (!hasAppeared && !visible.isClosing && appearanceAnim != null) {
        Modifier.applyAnimations(appearanceAnim.content)
    } else if (visible.isClosing && appearanceAnim != null) {
        Modifier.applyAnimations(appearanceAnim.content)
    } else {
        Modifier
    }

    if (!visible.isClosing && !hasAppeared) {
        val totalDuration = if (appearanceAnim != null) {
            totalDurationMillis(appearanceAnim.background, appearanceAnim.content)
        } else 0L
        val onWillAppear = resolveScreenLifecycle(LifecycleEvent.WILL_APPEAR, entry, screenBundle, config)
        val onDidAppear = resolveScreenLifecycle(LifecycleEvent.DID_APPEAR, entry, screenBundle, config)
        LaunchedEffect(navId) {
            eventDispatcher.publishLifecycle(
                com.adapty.ui.internal.ui.event.LifecyclePhase.WILL_APPEAR,
                entry.screenInstanceId,
                entry.transitionId,
                entry.epoch,
            )
            if (!onWillAppear.isNullOrEmpty()) {
                dispatch(Message.ActionsRequested(onWillAppear, entry))
            }
            if (totalDuration > 0) {
                kotlinx.coroutines.delay(totalDuration)
            }
            hasAppeared = true
            dispatch(Message.NavigatorAppeared(navId))
            eventDispatcher.publishLifecycle(
                com.adapty.ui.internal.ui.event.LifecyclePhase.DID_APPEAR,
                entry.screenInstanceId,
                entry.transitionId,
                entry.epoch,
            )
            if (!onDidAppear.isNullOrEmpty()) {
                dispatch(Message.ActionsRequested(onDidAppear, entry))
            }
        }
    }

    if (visible.isClosing) {
        val totalDuration = if (appearanceAnim != null) {
            totalDurationMillis(appearanceAnim.background, appearanceAnim.content)
        } else 0L
        val onWillDisappear = resolveScreenLifecycle(LifecycleEvent.WILL_DISAPPEAR, entry, screenBundle, config)
        val onDidDisappear = resolveScreenLifecycle(LifecycleEvent.DID_DISAPPEAR, entry, screenBundle, config)
        LaunchedEffect(navId, visible.closingKey) {
            eventDispatcher.publishLifecycle(
                com.adapty.ui.internal.ui.event.LifecyclePhase.WILL_DISAPPEAR,
                entry.screenInstanceId,
                entry.transitionId,
                entry.epoch,
            )
            if (!onWillDisappear.isNullOrEmpty()) {
                dispatch(Message.ActionsRequested(onWillDisappear, entry))
            }
            if (totalDuration > 0) {
                kotlinx.coroutines.delay(totalDuration)
            }
            dispatch(Message.NavigatorDismissed(navId))
            eventDispatcher.publishLifecycle(
                com.adapty.ui.internal.ui.event.LifecyclePhase.DID_DISAPPEAR,
                entry.screenInstanceId,
                entry.transitionId,
                entry.epoch,
            )
            if (!onDidDisappear.isNullOrEmpty()) {
                dispatch(Message.ActionsRequested(onDidDisappear, entry))
            }
        }
    }

    val bgActions = screenBundle.screens[entry.screenType]?.onOutsideTap ?: config.onOutsideTap

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(bgColorState.value)
            .then(mediaBackgroundModifier)
            .then(
                if (bgActions.isNotEmpty())
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { dispatch(Message.ActionsRequested(bgActions, entry)) }
                else Modifier
            ),
    ) {
        CompositionLocalProvider(
            LocalNavigatorEntry provides entry,
            LocalScreenBundle provides screenBundle,
            LocalNavigatorConfig provides config,
            LocalScreenInstance provides FAKE_NAVIGATOR_ENTRY,
        ) {
            Box(modifier = contentAnimModifier) {
                config.content.render(dispatch)
            }
            config.overlays.forEach { overlay ->
                Box(
                    contentAlignment = overlay.align.toComposeAlignment(),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    overlay.content.render(dispatch)
                }
            }
        }
    }
}
