package com.adapty.ui.internal.script

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class JSRpcBarrierInstrumentedTest {

    @Test
    fun barrierWaitsForEarlierAnalyticsRpc() = runBlocking {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()
        val handler = Proxy.newProxyInstance(
            ActionHandler::class.java.classLoader,
            arrayOf(ActionHandler::class.java),
        ) { _, method, args ->
            if (method.name == "onSendAnalyticsEvent") {
                @Suppress("UNCHECKED_CAST")
                events.add((args?.get(0) as String) to (args[1] as Map<String, Any?>))
            }
            null
        } as ActionHandler
        val gson = Gson()
        val stateMachine = JSStateMachine(
            InstrumentationRegistry.getInstrumentation().targetContext,
            JSActionBridge(gson),
            gson,
        )
        stateMachine.setActionHandler(handler)

        try {
            stateMachine.executeScript(
                """
                SDK.sendAnalyticsEvent({
                  name: 'flow_user_input',
                  instanceId: 'screen_registration',
                  isBackendEvent: false,
                  isCustomerEvent: true,
                  payload: '{"version":1,"element_id":"email","element_type":"email_input","value":"reader@example.com"}'
                })
                """.trimIndent(),
            )
            assertTrue(stateMachine.awaitRpcDrain())

            assertEquals(1, events.size)
            val (name, params) = events.single()
            assertEquals("flow_user_input", name)
            assertEquals("screen_registration", params["instanceId"])
            assertEquals(false, params["isBackendEvent"])
            assertEquals(true, params["isCustomerEvent"])
            assertEquals(
                """{"version":1,"element_id":"email","element_type":"email_input","value":"reader@example.com"}""",
                params["payload"],
            )
        } finally {
            stateMachine.close()
        }
    }

    @Test
    fun webViewDrainsBacklogBeforeBarrier() = runBlocking {
        val eventNames = mutableListOf<String>()
        val handler = Proxy.newProxyInstance(
            ActionHandler::class.java.classLoader,
            arrayOf(ActionHandler::class.java),
        ) { _, method, args ->
            if (method.name == "onSendAnalyticsEvent") {
                eventNames.add(args?.get(0) as String)
            }
            null
        } as ActionHandler
        val gson = Gson()
        val bridge = JSActionBridge(gson).apply { actionHandler = handler }
        val barrierReached = CompletableDeferred<Unit>()
        bridge.onRpcBarrier = { id ->
            if (id == "backlog_drained") barrierReached.complete(Unit)
        }
        val engine = JSEngineWebView(
            InstrumentationRegistry.getInstrumentation().targetContext,
            bridge,
            gson,
        )

        try {
            engine.initialize()
            engine.execute(
                """
                for (var i = 0; i < 64; i++) {
                  postToHost({ method: 'SDK.sendAnalyticsEvent', params: { name: 'event_' + i } });
                }
                postToHost({
                  method: '${JSActionBridge.RPC_BARRIER_METHOD}',
                  params: { id: 'backlog_drained' }
                });
                """.trimIndent(),
            )

            withTimeout(2_000L) { barrierReached.await() }
            assertEquals((0 until 64).map { "event_$it" }, eventNames)
        } finally {
            engine.close()
        }
    }

    @Test
    fun lateRpcAfterExitCannotReachTheNextHandler() = runBlocking {
        val events = mutableListOf<String>()
        val oldEventDelivered = CompletableDeferred<Unit>()
        val newEventDelivered = CompletableDeferred<Unit>()
        val bridge = JSActionBridge(Gson())

        bridge.actionHandler = analyticsHandler { name ->
            events.add("old:$name")
            oldEventDelivered.complete(Unit)
        }
        bridge.handleAnalyticsRpc("before-exit")
        withTimeout(1_000L) { oldEventDelivered.await() }

        bridge.actionHandler = null
        bridge.handleAnalyticsRpc("late-old")
        bridge.actionHandler = analyticsHandler { name ->
            events.add("new:$name")
            newEventDelivered.complete(Unit)
        }
        bridge.handleAnalyticsRpc("late-old-after-handler")
        bridge.reset()
        bridge.handleAnalyticsRpc("new-flow")
        withTimeout(1_000L) { newEventDelivered.await() }

        assertEquals(listOf("old:before-exit", "new:new-flow"), events)
    }

    private fun JSActionBridge.handleAnalyticsRpc(name: String) {
        handleRpc(
            """{"method":"SDK.sendAnalyticsEvent","params":{"name":"$name"}}""",
        ) { _, _, _ -> error("Analytics event must not resolve a JS promise") }
    }

    private fun analyticsHandler(onEvent: (String) -> Unit): ActionHandler = Proxy.newProxyInstance(
        ActionHandler::class.java.classLoader,
        arrayOf(ActionHandler::class.java),
    ) { _, method, args ->
        if (method.name == "onSendAnalyticsEvent") onEvent(args?.get(0) as String)
        null
    } as ActionHandler
}
