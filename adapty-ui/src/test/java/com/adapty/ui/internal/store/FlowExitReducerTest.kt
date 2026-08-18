@file:OptIn(com.adapty.internal.utils.InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.internal.utils.Scope
import com.adapty.ui.internal.utils.StringSource
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.FlowMode
import com.adapty.ui.listeners.AdaptyFlowDefaultEventListener
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

internal class FlowExitReducerTest {

    @Test
    fun closeAllRemainsVetoableUntilFlowActuallyExits() {
        val state = state()

        val (newState, effects) = reduce(state, Message.JSCallback.CloseAll)

        assertSame(state, newState)
        assertEquals(1, effects.size)
        assertSame(
            AdaptyUI.Action.Close,
            (effects.single() as Effect.NotifyListener.ActionPerformed).action,
        )
        assertFalse(newState.ui.exitStarted)
    }

    @Test
    fun actualExitFlushesVisibleScreensOnceBeforeClosing() {
        val activeEntry = NavigationEntry("active", "screen", null, "default", epoch = 1L)
        val replacedEntry = NavigationEntry("replaced", "screen", null, "default", epoch = 2L)
        val closingEntry = NavigationEntry("closing", "screen", null, "sheet", epoch = 3L)
        val state = state(
            navigation = NavigationState(
                entries = mapOf("default" to activeEntry),
                closingEntries = mapOf(
                    "default" to ClosingNavigator(replacedEntry, "close"),
                    "sheet" to ClosingNavigator(closingEntry, "close"),
                ),
            ),
            currentFocusId = "email",
        )

        val (exitingState, exitEffects) = reduce(state, Message.FlowExited)

        assertTrue(exitingState.ui.exitStarted)
        assertFalse(exitingState.ui.flowShown)
        assertEquals(null, exitingState.ui.currentFocusId)
        assertEquals(state.ui.focusGeneration + 1, exitingState.ui.focusGeneration)
        assertSame(state.navigation, exitingState.navigation)

        val flush = exitEffects.single() as Effect.FlushBeforeExit
        assertEquals(listOf(closingEntry, activeEntry), flush.focusActions.map { it.screen })
        assertEquals(listOf(closingEntry, activeEntry), flush.willDisappearActions.map { it.screen })
        assertEquals(listOf("focus-sheet", "focus-default"), flush.focusActions.map { it.actions.single().func })
        assertEquals(listOf("will-sheet", "will-default"), flush.willDisappearActions.map { it.actions.single().func })

        val (duplicateState, duplicateEffects) = reduce(exitingState, Message.FlowExited)
        assertSame(exitingState, duplicateState)
        assertTrue(duplicateEffects.isEmpty())

        val (reenteredWhileExiting, reentryEffects) = reduce(exitingState, Message.FlowEntered)
        assertSame(exitingState, reenteredWhileExiting)
        assertTrue(reentryEffects.isEmpty())

        val (closedState, closeEffects) = reduce(exitingState, FlowExitFlushed)
        assertEquals(NavigationState(), closedState.navigation)
        assertFalse(closedState.ui.exitStarted)
        assertTrue(closedState.ui.exitCompleted)
        assertEquals(
            listOf(Effect.ClearActionHandler, Effect.NotifyListener.FlowClosed),
            closeEffects,
        )

        val (afterDuplicateExit, afterDuplicateEffects) = reduce(closedState, Message.FlowExited)
        assertSame(closedState, afterDuplicateExit)
        assertTrue(afterDuplicateEffects.isEmpty())

        val (reenteredAfterExit, completedReentryEffects) = reduce(closedState, Message.FlowEntered)
        assertSame(closedState, reenteredAfterExit)
        assertTrue(completedReentryEffects.isEmpty())
    }

    @Test
    fun platformViewDisposalAndCompositionDisposalStartOneExitFlush() {
        val state = state(currentFocusId = "email")

        val (platformViewExitState, platformViewEffects) = reduce(state, Message.FlowExited)
        val (compositionExitState, compositionEffects) = reduce(platformViewExitState, Message.FlowExited)

        assertEquals(1, platformViewEffects.filterIsInstance<Effect.FlushBeforeExit>().size)
        assertSame(platformViewExitState, compositionExitState)
        assertTrue(compositionEffects.isEmpty())
    }

    @Test
    fun valueAndFocusCallbacksAreIgnoredAfterExitStarts() {
        val state = state(currentFocusId = "email")
        val exitingState = reduce(state, Message.FlowExited).first
        val binding = TwoWayBinding("email", Scope.Global, setter = null)

        val (afterValue, valueEffects) = reduce(
            exitingState,
            Message.ValueChanged(binding, "late", NavigationEntry("active", "screen", null)),
        )
        val (afterFocus, focusEffects) = reduce(exitingState, Message.FocusChanged("email"))

        assertSame(exitingState, afterValue)
        assertTrue(valueEffects.isEmpty())
        assertSame(exitingState, afterFocus)
        assertTrue(focusEffects.isEmpty())
    }

    @Test
    fun customerOnlyInputEventReachesListenerWithoutBackendEffect() {
        val params = mapOf<String, Any?>(
            "name" to "flow_user_input",
            "instanceId" to "screen_registration",
            "isBackendEvent" to false,
            "isCustomerEvent" to true,
            "payload" to """{"version":1,"element_id":"email","element_type":"email_input","value":"reader@example.com"}""",
        )

        val (_, effects) = reduce(
            state(),
            Message.JSCallback.SendAnalyticsEvent("flow_user_input", params),
        )

        assertEquals(
            listOf(Effect.NotifyListener.AnalyticEvent("flow_user_input", params)),
            effects,
        )
        assertTrue(effects.none { it is Effect.LogFlowEvent })
    }

    private fun state(
        navigation: NavigationState = NavigationState(
            entries = mapOf(
                "default" to NavigationEntry("active", "screen", null, "default", epoch = 1L),
            ),
        ),
        currentFocusId: String? = null,
        mode: FlowMode = FlowMode.Preview,
    ): FlowState {
        val focusDefault = Action("focus-default", emptyMap(), Scope.Screen)
        val willDefault = Action("will-default", emptyMap(), Scope.Screen)
        val focusSheet = Action("focus-sheet", emptyMap(), Scope.Screen)
        val willSheet = Action("will-sheet", emptyMap(), Scope.Screen)
        val background = VisualValue(
            StringSource.Value("#000000"),
            setOf(VisualValue.Type.ColorLiteral),
        )
        val viewConfig = AdaptyUI.FlowConfiguration(
            id = "config",
            mode = mode,
            isHard = false,
            isRtl = false,
            locale = Locale.US,
            assets = emptyMap(),
            texts = emptyMap(),
            screens = AdaptyUI.FlowConfiguration.ScreenBundle(emptyMap(), emptyMap()),
            navigators = mapOf(
                "default" to AdaptyUI.FlowConfiguration.NavigatorConfig(
                    background = background,
                    content = SkippedElement,
                    order = 0,
                    onFocusChange = listOf(focusDefault),
                    onWillDisappear = listOf(willDefault),
                ),
                "sheet" to AdaptyUI.FlowConfiguration.NavigatorConfig(
                    background = background,
                    content = SkippedElement,
                    order = 100,
                    onFocusChange = listOf(focusSheet),
                    onWillDisappear = listOf(willSheet),
                ),
            ),
            initialScript = "",
            showPurchaseLoader = false,
            showRestoreLoader = false,
        )
        return FlowState(
            config = ConfigState(
                viewConfig = viewConfig,
                isObserverMode = false,
                placementId = mode.placementId,
                observerModeHandler = null,
                eventListener = AdaptyFlowDefaultEventListener(),
                userInsets = AdaptyFlowInsets.None,
                customAssets = AdaptyCustomAssets.Empty,
                tagResolver = AdaptyUiTagResolver.Default,
                timerResolver = AdaptyUiTimerResolver.Default,
                productLoadingFailureCallback = ProductLoadingFailureCallback { false },
            ),
            products = ProductsState(emptyMap(), LoadingStatus.Idle),
            assets = AssetsState(emptyMap()),
            texts = TextsState(emptyMap()),
            purchase = PurchaseFlowState.Idle,
            restore = RestoreFlowState.Idle,
            navigation = navigation,
            ui = UiState(
                isLoading = false,
                flowShown = true,
                currentFocusId = currentFocusId,
                focusGeneration = 4,
            ),
        )
    }

}
