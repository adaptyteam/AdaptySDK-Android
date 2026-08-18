package com.adapty.ui.internal.script

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

internal class JSActionBridgeTest {

    @Test
    fun rpcBarrierBypassesTheActionHandlerQueue() {
        val bridge = JSActionBridge(Gson())
        var receivedId: String? = null
        bridge.onRpcBarrier = { receivedId = it }

        bridge.handleRpc(
            """{"method":"${JSActionBridge.RPC_BARRIER_METHOD}","params":{"id":"exit-1"}}""",
        ) { _, _, _ -> error("Barrier must not resolve a JS promise") }

        assertEquals("exit-1", receivedId)
    }

}
