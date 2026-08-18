@file:OptIn(com.adapty.internal.utils.InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.adapty.internal.utils.PriceFormatter
import com.adapty.models.AdaptyFlow
import com.adapty.models.AdaptyPlacement
import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.listeners.ContextAwareEventListener
import com.adapty.ui.internal.script.ActionHandler
import com.adapty.ui.internal.script.JSActionBridge
import com.adapty.ui.internal.script.JSEngineAndroidx
import com.adapty.ui.internal.script.JSStateHandler
import com.adapty.ui.internal.script.JSStateMachine
import com.adapty.ui.internal.store.Effect
import com.adapty.ui.internal.store.EffectHandler
import com.adapty.ui.internal.store.JsEffectHandler
import com.adapty.ui.internal.store.ListenerEffectHandler
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.text.PriceConverter
import com.adapty.ui.internal.text.TagResolver
import com.adapty.ui.internal.text.TextResolver
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.utils.FlowMode
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.internal.utils.Scope
import com.adapty.ui.internal.utils.StringSource
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.initializeFlowStateAsync
import com.adapty.ui.listeners.AdaptyFlowDefaultEventListener
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import com.adapty.utils.ImmutableList
import com.google.gson.Gson
import java.lang.reflect.Proxy
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FlowExitIntegrationInstrumentedTest {

    @Test
    fun vetoedCloseThenActualExitDeliversInputOnceBeforeFlowClosed() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val gson = Gson()
        val stateMachine = JSStateMachine(context, JSActionBridge(gson), gson)
        val stateHandler = JSStateHandler(stateMachine, gson)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val focusReady = CompletableDeferred<Unit>()
        val flowClosed = CompletableDeferred<Unit>()
        val trace = mutableListOf<String>()
        val analytics = mutableListOf<Pair<String, Map<String, Any?>>>()
        val backendEvents = mutableListOf<Effect.LogFlowEvent>()
        val listener = recordingListener(trace, analytics, flowClosed)
        val configuration = flowConfiguration()
        val viewModel = FlowViewModel(
            flowKey = "flow-exit-integration",
            isObserverMode = false,
            textResolver = textResolver(),
            stateHandler = stateHandler,
        )
        val secondViewModel = FlowViewModel(
            flowKey = "flow-exit-integration-second",
            isObserverMode = false,
            textResolver = textResolver(),
            stateHandler = stateHandler,
        )

        try {
            viewModel.effectHandlers = listOf(
                JsEffectHandler(scope, stateHandler, viewModel.eventDispatcher),
                ListenerEffectHandler { viewModel.contextAwareListener },
                EffectHandler { effect, _ ->
                    if (effect is Effect.LogFlowEvent) backendEvents.add(effect)
                },
            )
            secondViewModel.effectHandlers = listOf(
                JsEffectHandler(scope, stateHandler, secondViewModel.eventDispatcher),
                ListenerEffectHandler { secondViewModel.contextAwareListener },
                EffectHandler { effect, _ ->
                    if (effect is Effect.LogFlowEvent) backendEvents.add(effect)
                },
            )
            viewModel.setContextAwareListener(ContextAwareEventListener(listener) { context })
            viewModel.setActionHandler(actionHandler(viewModel, focusReady))
            stateHandler.loadScript(INPUT_SCRIPT)
            assertTrue(androidxEngineIsActive(stateMachine))

            withContext(Dispatchers.Main) {
                viewModel.setNewData(userArgs(configuration, listener))
                viewModel.dispatch(Message.JSCallback.OpenScreen(SCREEN))
                viewModel.dispatch(Message.FlushPendingNavigation)
                viewModel.dispatch(Message.FlowEntered)
                viewModel.dispatch(Message.JSCallback.ChangeFocus("email"))
            }
            withTimeout(5_000L) { focusReady.await() }

            withContext(Dispatchers.Main) {
                viewModel.dispatch(
                    Message.ValueChanged(
                        TwoWayBinding("answer", Scope.Global, setter = "on_input_change"),
                        "reader@example.com",
                        SCREEN,
                    ),
                )
                viewModel.dispatch(Message.JSCallback.CloseAll)
            }

            assertEquals(listOf("close"), trace)
            assertTrue(analytics.isEmpty())
            assertTrue(backendEvents.isEmpty())

            val secondViewModelStarted = CompletableDeferred<Unit>()
            withContext(Dispatchers.Main) {
                viewModel.dispatch(Message.FlowExited)
                assertTrue(
                    secondViewModel.deferUntilExitCompleted {
                        stateHandler.initializeFlowStateAsync(configuration, "{}", "{}") {
                            trace.add("second-start")
                            secondViewModelStarted.complete(Unit)
                        }
                    },
                )
                assertFalse(secondViewModelStarted.isCompleted)
            }
            withTimeout(5_000L) { flowClosed.await() }
            withTimeout(1_000L) { secondViewModelStarted.await() }

            assertEquals(listOf("close", "analytic", "closed", "second-start"), trace)
            assertEquals(1, analytics.size)
            val (name, params) = analytics.single()
            assertEquals("flow_user_input", name)
            assertEquals("screen_registration", params["instanceId"])
            assertEquals(false, params["isBackendEvent"])
            assertEquals(true, params["isCustomerEvent"])
            assertEquals(
                """{"version":1,"element_id":"email","element_type":"email_input","value":"reader@example.com"}""",
                params["payload"],
            )
            assertTrue(backendEvents.isEmpty())

            withContext(Dispatchers.Main) {
                viewModel.dispatch(Message.FlowExited)
            }
            assertEquals(1, analytics.size)
            assertEquals(1, trace.count { it == "closed" })

            val secondFocusReady = CompletableDeferred<Unit>()
            val secondFlowClosed = CompletableDeferred<Unit>()
            val secondListener = recordingListener(trace, analytics, secondFlowClosed)
            withContext(Dispatchers.Main) {
                secondViewModel.setContextAwareListener(ContextAwareEventListener(secondListener) { context })
                secondViewModel.setActionHandler(actionHandler(secondViewModel, secondFocusReady))
                secondViewModel.setNewData(userArgs(configuration, secondListener))
                secondViewModel.dispatch(Message.JSCallback.OpenScreen(SCREEN))
                secondViewModel.dispatch(Message.FlushPendingNavigation)
                secondViewModel.dispatch(Message.FlowEntered)
                secondViewModel.dispatch(Message.JSCallback.ChangeFocus("email"))
            }
            withTimeout(5_000L) { secondFocusReady.await() }

            withContext(Dispatchers.Main) {
                secondViewModel.dispatch(
                    Message.ValueChanged(
                        TwoWayBinding("answer", Scope.Global, setter = "on_input_change"),
                        "second@example.com",
                        SCREEN,
                    ),
                )
                secondViewModel.dispatch(Message.FlowExited)
            }
            withTimeout(5_000L) { secondFlowClosed.await() }

            assertEquals(2, analytics.size)
            assertEquals(2, trace.count { it == "closed" })
            assertEquals(
                """{"version":1,"element_id":"email","element_type":"email_input","value":"second@example.com"}""",
                analytics.last().second["payload"],
            )
            assertTrue(backendEvents.isEmpty())
        } finally {
            scope.cancel()
            stateMachine.close()
        }
    }

    private fun actionHandler(
        viewModel: FlowViewModel,
        focusReady: CompletableDeferred<Unit>,
    ): ActionHandler = Proxy.newProxyInstance(
        ActionHandler::class.java.classLoader,
        arrayOf(ActionHandler::class.java),
    ) { _, method, args ->
        when (method.name) {
            "onUserCustomAction" -> {
                if (args?.get(0) == "focus_ready") focusReady.complete(Unit)
            }
            "onSendAnalyticsEvent" -> {
                @Suppress("UNCHECKED_CAST")
                val params = args?.get(1) as Map<String, Any?>
                viewModel.dispatch(
                    Message.JSCallback.SendAnalyticsEvent(args[0] as String, params),
                )
            }
        }
        null
    } as ActionHandler

    private fun recordingListener(
        trace: MutableList<String>,
        analytics: MutableList<Pair<String, Map<String, Any?>>>,
        flowClosed: CompletableDeferred<Unit>,
    ) = object : AdaptyFlowDefaultEventListener() {
        override fun onActionPerformed(action: AdaptyUI.Action, context: Context) {
            trace.add("close")
        }

        override fun onAnalyticEvent(name: String, params: Map<String, Any?>, context: Context) {
            analytics.add(name to params)
            trace.add("analytic")
        }

        override fun onFlowClosed() {
            trace.add("closed")
            flowClosed.complete(Unit)
        }
    }

    private fun userArgs(
        viewConfig: AdaptyUI.FlowConfiguration,
        listener: AdaptyFlowDefaultEventListener,
    ) = UserArgs.create(
        viewConfig = viewConfig,
        eventListener = listener,
        userInsets = AdaptyFlowInsets.None,
        customAssets = AdaptyCustomAssets.Empty,
        tagResolver = AdaptyUiTagResolver.Default,
        timerResolver = AdaptyUiTimerResolver.Default,
        observerModeHandler = null,
        products = emptyList(),
        productLoadingFailureCallback = ProductLoadingFailureCallback { false },
    )

    private fun flowConfiguration(): AdaptyUI.FlowConfiguration {
        val background = VisualValue(
            StringSource.Value("#000000"),
            setOf(VisualValue.Type.ColorLiteral),
        )
        return AdaptyUI.FlowConfiguration(
            id = "config",
            mode = FlowMode.Live(liveFlow()),
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
                    onFocusChange = listOf(
                        Action("on_focus_change_handler", emptyMap(), Scope.Global),
                    ),
                    onWillDisappear = emptyList(),
                ),
            ),
            initialScript = INPUT_SCRIPT,
            showPurchaseLoader = false,
            showRestoreLoader = false,
        )
    }

    private fun textResolver(): TextResolver = TextResolver(
        TagResolver(
            PriceFormatter(Locale.US),
            PriceConverter(),
            AdaptyUiTagResolver.Default,
            Locale.US,
        ),
    )

    private fun androidxEngineIsActive(stateMachine: JSStateMachine): Boolean {
        val field = JSStateMachine::class.java.getDeclaredField("jsEngine")
        field.isAccessible = true
        return field.get(stateMachine) is JSEngineAndroidx
    }

    private fun liveFlow(): AdaptyFlow {
        val placementConstructor = AdaptyPlacement::class.java.declaredConstructors.single()
        placementConstructor.isAccessible = true
        val placement = placementConstructor.newInstance(
            "placement",
            "A/B",
            "Audience",
            1,
            false,
            "audience-version",
        ) as AdaptyPlacement

        val flowConstructor = AdaptyFlow::class.java.declaredConstructors.single()
        flowConstructor.isAccessible = true
        return flowConstructor.newInstance(
            "flow",
            "variation",
            "Flow",
            ImmutableList(emptyList<Any>()),
            placement,
            ImmutableList(emptyList<Any>()),
            "config",
            emptyMap<String, Any>(),
            0L,
        ) as AdaptyFlow
    }

    private companion object {
        val SCREEN = NavigationEntry(
            "screen_registration",
            "screen",
            null,
            "default",
            epoch = 1L,
        )

        const val INPUT_SCRIPT = """
            var input_dirty = false;
            var input_value = '';
            var focus_initialized = false;

            function on_input_change(params) {
              input_value = params.value;
              input_dirty = true;
            }

            function on_focus_change_handler(params) {
              if (!focus_initialized) {
                focus_initialized = true;
                SDK.userCustomAction({ userCustomId: 'focus_ready' });
                return;
              }
              if (!input_dirty) return;
              input_dirty = false;
              SDK.sendAnalyticsEvent({
                name: 'flow_user_input',
                instanceId: 'screen_registration',
                isBackendEvent: false,
                isCustomerEvent: true,
                payload: JSON.stringify({
                  version: 1,
                  element_id: 'email',
                  element_type: 'email_input',
                  value: input_value
                })
              });
            }
        """
    }
}
