@file:OptIn(com.adapty.internal.utils.InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import androidx.compose.runtime.Composable
import com.adapty.ui.internal.script.ActionHandler
import com.adapty.ui.internal.script.StateAccessor
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.internal.ui.event.EventDispatcher
import com.adapty.ui.internal.ui.event.LifecyclePhase
import com.adapty.ui.internal.utils.Scope
import com.adapty.ui.internal.utils.TwoWayBinding
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

internal class JsEffectHandlerTest {

    @Test
    fun failedRpcBarrierRetriesBeforeCompletingExit() = runBlocking {
        val stateHandler = RecordingStateHandler(barrierResults = listOf(false, true))
        stateHandler.releaseBarrier.complete(Unit)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val handler = JsEffectHandler(scope, stateHandler, EventDispatcher())
        val messages = mutableListOf<Message>()

        try {
            handler.handle(
                Effect.FlushBeforeExit(emptyList(), emptyList()),
                messages::add,
            )
            withTimeout(1_000L) {
                while (FlowExitFlushed !in messages) yield()
            }

            assertEquals(listOf(FlowExitFlushed), messages)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun permanentRpcBarrierFailureStillCompletesExit() = runBlocking {
        val stateHandler = RecordingStateHandler(barrierResults = listOf(false))
        stateHandler.releaseBarrier.complete(Unit)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val handler = JsEffectHandler(scope, stateHandler, EventDispatcher())
        val messages = mutableListOf<Message>()

        try {
            handler.handle(
                Effect.FlushBeforeExit(emptyList(), emptyList()),
                messages::add,
            )
            withTimeout(1_000L) {
                while (FlowExitFlushed !in messages) yield()
            }

            assertEquals(2, stateHandler.calls.count { it == "barrier" })
            assertEquals(listOf(FlowExitFlushed), messages)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun hangingPreExitJavaScriptCannotBlockFlowClosed() = runBlocking {
        listOf(
            RecordingStateHandler(hangOnSetValue = true) to emptyList(),
            RecordingStateHandler(hangOnAction = "focus") to listOf(
                Effect.ExecuteJSActions(
                    listOf(Action("focus", emptyMap(), Scope.Screen)),
                    NavigationEntry("active", "screen", null),
                ),
            ),
        ).forEach { (stateHandler, focusActions) ->
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val handler = JsEffectHandler(
                scope,
                stateHandler,
                EventDispatcher(),
                exitFlushTimeoutMillis = 10L,
            )
            val messages = mutableListOf<Message>()

            try {
                if (focusActions.isEmpty()) {
                    listOf("hanging", "queued").forEach { value ->
                        handler.handle(
                            Effect.SetJSValue(
                                TwoWayBinding("email", Scope.Global, setter = null),
                                value,
                                NavigationEntry("active", "screen", null),
                            ),
                            messages::add,
                        )
                    }
                }
                handler.handle(
                    Effect.FlushBeforeExit(focusActions, emptyList()),
                    messages::add,
                )

                withTimeout(1_000L) {
                    while (FlowExitFlushed !in messages) yield()
                }
                assertEquals(listOf(FlowExitFlushed), messages)
                if (focusActions.isEmpty()) {
                    withTimeout(1_000L) {
                        while (stateHandler.cancelledWriteCount < 1) yield()
                    }
                    assertEquals(1, stateHandler.cancelledWriteCount)
                }
            } finally {
                scope.cancel()
            }
        }
    }

    @Test
    fun exitWaitsForValueThenDeduplicatesLifecycleAndDrainsRpc() = runBlocking {
        val stateHandler = RecordingStateHandler()
        val eventDispatcher = EventDispatcher()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val handler = JsEffectHandler(scope, stateHandler, eventDispatcher)
        val active = NavigationEntry("active", "screen", null, epoch = 11L)
        val alreadyClosing = NavigationEntry("closing", "screen", null, epoch = 12L)
        val noActions = NavigationEntry("no-actions", "screen", null, epoch = 13L)
        val messages = mutableListOf<Message>()
        eventDispatcher.publishLifecycle(
            LifecyclePhase.WILL_DISAPPEAR,
            alreadyClosing.screenInstanceId,
            alreadyClosing.transitionId,
            alreadyClosing.epoch,
        )

        try {
            handler.handle(
                Effect.SetJSValue(
                    TwoWayBinding("email", Scope.Global, setter = null),
                    "first",
                    active,
                ),
                messages::add,
            )
            handler.handle(
                Effect.SetJSValue(
                    TwoWayBinding("email", Scope.Global, setter = null),
                    "dirty",
                    active,
                ),
                messages::add,
            )
            handler.handle(
                Effect.FlushBeforeExit(
                    focusActions = listOf(
                        Effect.ExecuteJSActions(
                            listOf(Action("focus", emptyMap(), Scope.Screen)),
                            active,
                        ),
                    ),
                    willDisappearActions = listOf(
                        Effect.ExecuteJSActions(
                            listOf(Action("will-active", emptyMap(), Scope.Screen)),
                            active,
                        ),
                        Effect.ExecuteJSActions(
                            listOf(Action("will-duplicate", emptyMap(), Scope.Screen)),
                            alreadyClosing,
                        ),
                        Effect.ExecuteJSActions(emptyList(), noActions),
                    ),
                ),
                messages::add,
            )

            assertEquals(listOf("write-first-start"), stateHandler.calls)
            stateHandler.releaseFirstWrite.complete(Unit)
            assertEquals(
                listOf("write-first-start", "write-first-end", "write-dirty-start"),
                stateHandler.calls,
            )
            stateHandler.releaseDirtyWrite.complete(Unit)
            withTimeout(1_000L) {
                while ("barrier" !in stateHandler.calls) yield()
            }

            assertEquals(
                listOf(
                    "write-first-start",
                    "write-first-end",
                    "write-dirty-start",
                    "write-dirty-end",
                    "focus",
                    "will-active",
                    "barrier",
                ),
                stateHandler.calls,
            )
            assertEquals(emptyList<Message>(), messages)
            stateHandler.releaseBarrier.complete(Unit)
            withTimeout(1_000L) {
                while (FlowExitFlushed !in messages) yield()
            }
            assertEquals(1, eventDispatcher.fireCount("active", LifecyclePhase.WILL_DISAPPEAR.key))
            assertEquals(1, eventDispatcher.fireCount("closing", LifecyclePhase.WILL_DISAPPEAR.key))
            assertEquals(1, eventDispatcher.fireCount("no-actions", LifecyclePhase.WILL_DISAPPEAR.key))
            assertEquals(listOf(FlowExitFlushed), messages)

            messages.clear()
            stateHandler.calls.clear()
            val nextPresentation = NavigationEntry("next", "screen", null, epoch = 14L)
            handler.handle(
                Effect.FlushBeforeExit(
                    focusActions = emptyList(),
                    willDisappearActions = listOf(
                        Effect.ExecuteJSActions(
                            listOf(Action("will-next", emptyMap(), Scope.Screen)),
                            nextPresentation,
                        ),
                    ),
                ),
                messages::add,
            )
            withTimeout(1_000L) {
                while (FlowExitFlushed !in messages) yield()
            }
            assertEquals(listOf("will-next", "barrier"), stateHandler.calls)
            assertEquals(listOf(FlowExitFlushed), messages)
        } finally {
            scope.cancel()
        }
    }

    private class RecordingStateHandler(
        private val barrierResults: List<Boolean> = listOf(true),
        private val hangOnSetValue: Boolean = false,
        private val hangOnAction: String? = null,
    ) : StateHandler {
        val calls = mutableListOf<String>()
        val releaseFirstWrite = CompletableDeferred<Unit>()
        val releaseDirtyWrite = CompletableDeferred<Unit>()
        val releaseBarrier = CompletableDeferred<Unit>()
        var cancelledWriteCount = 0
        private var barrierIndex = 0

        override var stateOwner: Any? = null
        override var onStateRefreshed: (() -> Unit)? = null

        override fun beginFlowExit(owner: Any) = Unit
        override fun deferUntilFlowExitCompleted(action: () -> Unit): Boolean = false
        override fun completeFlowExit(owner: Any) = Unit

        @Composable
        override fun observeState(): StateAccessor = object : StateAccessor {
            override fun get(key: String): Any? = null
        }

        override suspend fun reset() = Unit
        override suspend fun loadScript(script: String) = Unit
        override suspend fun collectStateSnapshot(): Map<String, Any?>? = null
        override suspend fun applyStateSnapshot(snapshot: Map<String, Any?>) = Unit
        override suspend fun setActionsSuppressed(suppressed: Boolean) = Unit
        override suspend fun refreshState() = Unit

        override suspend fun setValue(binding: TwoWayBinding, value: Any?, screen: NavigationEntry) {
            calls.add("write-$value-start")
            if (hangOnSetValue) {
                try {
                    awaitCancellation()
                } finally {
                    cancelledWriteCount++
                }
            }
            when (value) {
                "first" -> releaseFirstWrite.await()
                "dirty" -> releaseDirtyWrite.await()
            }
            calls.add("write-$value-end")
        }

        override suspend fun executeAction(
            func: String,
            params: Map<String, Any?>,
            scope: Scope,
            screen: NavigationEntry,
        ) {
            calls.add(func)
            if (func == hangOnAction) awaitCancellation()
        }

        override suspend fun awaitRpcDrain(): Boolean {
            calls.add("barrier")
            releaseBarrier.await()
            return barrierResults.getOrElse(barrierIndex++) { barrierResults.last() }
        }

        override suspend fun invokeTimerCallback(timerId: String) = Unit
        override suspend fun sendSDKEvent(eventJson: String) = Unit
        override suspend fun invokePurchaseCallback(callbackId: String, response: Map<String, Any?>) = Unit
        override suspend fun invokeRestoreCallback(callbackId: String, response: Map<String, Any?>) = Unit
        override suspend fun invokeAlertCallback(callbackId: String, response: Map<String, Any?>) = Unit
        override suspend fun invokePermissionCallback(callbackId: String, response: Map<String, Any?>) = Unit
        override suspend fun injectSDKGlobals(sdkEnvJson: String, sdkProductsJson: String) = Unit
        override suspend fun updateSDKProducts(sdkProductsJson: String) = Unit
        override fun setActionHandler(handler: ActionHandler?) = Unit
    }
}
