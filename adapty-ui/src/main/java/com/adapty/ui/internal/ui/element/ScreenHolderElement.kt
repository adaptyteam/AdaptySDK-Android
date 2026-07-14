@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import com.adapty.ui.internal.utils.getRtlAware
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.LifecycleEvent
import com.adapty.ui.internal.ui.LocalNavigatorConfig
import com.adapty.ui.internal.ui.LocalNavigatorEntry
import com.adapty.ui.internal.ui.LocalScreenBundle
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.attributes.LocalContentAlignment
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.ui.applyAnimations
import com.adapty.ui.internal.ui.renderScreen
import com.adapty.ui.internal.ui.resolveScreenLifecycle
import com.adapty.ui.internal.ui.totalDurationMillis

@InternalAdaptyApi
public class ScreenHolderElement internal constructor(
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val entry = LocalNavigatorEntry.current
        val screenBundle = LocalScreenBundle.current
        val navigatorConfig = LocalNavigatorConfig.current
        val parentAlignment = LocalContentAlignment.current
        val eventDispatcher = com.adapty.ui.internal.ui.event.LocalEventDispatcher.current

        var previousEntry by remember { mutableStateOf<NavigationEntry?>(null) }
        var currentEntry by remember { mutableStateOf(entry) }
        var isTransitioning by remember { mutableStateOf(false) }

        if (entry.screenInstanceId != currentEntry.screenInstanceId) {
            previousEntry = currentEntry
            isTransitioning = shouldPlayTransition(
                previousEntry = currentEntry,
                newEntry = entry,
                navigatorConfig = navigatorConfig,
            )
            currentEntry = entry
        }

        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val transitionKey = currentEntry.transitionId
        val transition = if (transitionKey != null && navigatorConfig != null) {
            navigatorConfig.transitions.getRtlAware(transitionKey, isRtl)
        } else null

        Box(modifier = modifier) {
            if (isTransitioning && transition != null && previousEntry != null) {
                val totalDuration = maxOf(
                    totalDurationMillis(transition.outgoing),
                    totalDurationMillis(transition.incoming),
                )

                val capturedPrevEntry = previousEntry
                LaunchedEffect(currentEntry.screenInstanceId) {
                    capturedPrevEntry?.let { prev ->
                        eventDispatcher.publishLifecycle(
                            com.adapty.ui.internal.ui.event.LifecyclePhase.WILL_DISAPPEAR,
                            prev.screenInstanceId,
                            prev.transitionId,
                            prev.epoch,
                        )
                    }
                    eventDispatcher.publishLifecycle(
                        com.adapty.ui.internal.ui.event.LifecyclePhase.WILL_APPEAR,
                        currentEntry.screenInstanceId,
                        currentEntry.transitionId,
                        currentEntry.epoch,
                    )
                    if (navigatorConfig != null) {
                        capturedPrevEntry?.let { prev ->
                            resolveScreenLifecycle(LifecycleEvent.WILL_DISAPPEAR, prev, screenBundle, navigatorConfig)
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { dispatch(Message.ActionsRequested(it, prev)) }
                        }
                        resolveScreenLifecycle(LifecycleEvent.WILL_APPEAR, currentEntry, screenBundle, navigatorConfig)
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { dispatch(Message.ActionsRequested(it, currentEntry)) }
                    }
                    if (totalDuration > 0) {
                        kotlinx.coroutines.delay(totalDuration)
                    }
                    capturedPrevEntry?.let { prev ->
                        eventDispatcher.publishLifecycle(
                            com.adapty.ui.internal.ui.event.LifecyclePhase.DID_DISAPPEAR,
                            prev.screenInstanceId,
                            prev.transitionId,
                            prev.epoch,
                        )
                    }
                    eventDispatcher.publishLifecycle(
                        com.adapty.ui.internal.ui.event.LifecyclePhase.DID_APPEAR,
                        currentEntry.screenInstanceId,
                        currentEntry.transitionId,
                        currentEntry.epoch,
                    )
                    if (navigatorConfig != null) {
                        capturedPrevEntry?.let { prev ->
                            resolveScreenLifecycle(LifecycleEvent.DID_DISAPPEAR, prev, screenBundle, navigatorConfig)
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { dispatch(Message.ActionsRequested(it, prev)) }
                        }
                        resolveScreenLifecycle(LifecycleEvent.DID_APPEAR, currentEntry, screenBundle, navigatorConfig)
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { dispatch(Message.ActionsRequested(it, currentEntry)) }
                    }
                    isTransitioning = false
                    previousEntry = null
                }

            } else {
                isTransitioning = false

                val capturedPrevEntry = previousEntry
                if (capturedPrevEntry != null) {
                    LaunchedEffect(currentEntry.screenInstanceId) {
                        eventDispatcher.publishLifecycle(
                            com.adapty.ui.internal.ui.event.LifecyclePhase.WILL_DISAPPEAR,
                            capturedPrevEntry.screenInstanceId,
                            capturedPrevEntry.transitionId,
                            capturedPrevEntry.epoch,
                        )
                        eventDispatcher.publishLifecycle(
                            com.adapty.ui.internal.ui.event.LifecyclePhase.WILL_APPEAR,
                            currentEntry.screenInstanceId,
                            currentEntry.transitionId,
                            currentEntry.epoch,
                        )
                        if (navigatorConfig != null) {
                            resolveScreenLifecycle(LifecycleEvent.WILL_DISAPPEAR, capturedPrevEntry, screenBundle, navigatorConfig)
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { dispatch(Message.ActionsRequested(it, capturedPrevEntry)) }
                            resolveScreenLifecycle(LifecycleEvent.WILL_APPEAR, currentEntry, screenBundle, navigatorConfig)
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { dispatch(Message.ActionsRequested(it, currentEntry)) }
                        }
                        eventDispatcher.publishLifecycle(
                            com.adapty.ui.internal.ui.event.LifecyclePhase.DID_DISAPPEAR,
                            capturedPrevEntry.screenInstanceId,
                            capturedPrevEntry.transitionId,
                            capturedPrevEntry.epoch,
                        )
                        if (navigatorConfig != null) {
                            resolveScreenLifecycle(LifecycleEvent.DID_DISAPPEAR, capturedPrevEntry, screenBundle, navigatorConfig)
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { dispatch(Message.ActionsRequested(it, capturedPrevEntry)) }
                        }
                        eventDispatcher.publishLifecycle(
                            com.adapty.ui.internal.ui.event.LifecyclePhase.DID_APPEAR,
                            currentEntry.screenInstanceId,
                            currentEntry.transitionId,
                            currentEntry.epoch,
                        )
                        if (navigatorConfig != null) {
                            resolveScreenLifecycle(LifecycleEvent.DID_APPEAR, currentEntry, screenBundle, navigatorConfig)
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { dispatch(Message.ActionsRequested(it, currentEntry)) }
                        }
                        previousEntry = null
                    }
                }
            }

            val playingTransition = isTransitioning && transition != null && previousEntry != null
            val slots: List<Pair<NavigationEntry, Modifier>> =
                if (playingTransition) {
                    val outgoingZIndex = if (transition!!.isIncomingOnTop) 0f else 1f
                    val incomingZIndex = if (transition.isIncomingOnTop) 1f else 0f
                    listOf(
                        previousEntry!! to Modifier
                            .fillMaxSize()
                            .zIndex(outgoingZIndex)
                            .applyAnimations(transition.outgoing),
                        currentEntry to Modifier
                            .fillMaxSize()
                            .zIndex(incomingZIndex)
                            .applyAnimations(transition.incoming),
                    )
                } else {
                    listOf(currentEntry to Modifier)
                }
            for ((e, slotModifier) in slots) {
                key(e.screenInstanceId, e.epoch) {
                    Box(
                        contentAlignment = if (playingTransition) parentAlignment else Alignment.TopStart,
                        modifier = slotModifier,
                    ) {
                        CompositionLocalProvider(LocalScreenInstance provides e) {
                            renderScreen(e.screenType, screenBundle, dispatch)
                        }
                    }
                }
            }
        }
    }
}

private fun shouldPlayTransition(
    previousEntry: NavigationEntry,
    newEntry: NavigationEntry,
    navigatorConfig: com.adapty.ui.AdaptyUI.FlowConfiguration.NavigatorConfig?,
): Boolean {
    if (navigatorConfig == null) return false
    if (navigatorConfig.transitions.isEmpty()) return false
    if (newEntry.transitionId == null) return false
    if (previousEntry.screenInstanceId == newEntry.screenInstanceId
        && previousEntry.screenType == newEntry.screenType
        && previousEntry.contextPath == newEntry.contextPath
    ) return false
    return true
}
