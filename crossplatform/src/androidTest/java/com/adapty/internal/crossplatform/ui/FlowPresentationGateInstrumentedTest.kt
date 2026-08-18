package com.adapty.internal.crossplatform.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.adapty.internal.crossplatform.SerializationHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FlowPresentationGateInstrumentedTest {
    private lateinit var flowUiManager: FlowUiManager

    @Before
    fun setUp() {
        flowUiManager = FlowUiManager(SerializationHelper { error("unused") })
        Dependencies.map[FlowUiManager::class.java] = mapOf(
            null to DIObject({ flowUiManager }),
        )
    }

    @After
    fun tearDown() {
        Dependencies.map.remove(FlowUiManager::class.java)
    }

    @Test
    fun missingFlowDataCompletesDismissalWithoutWaitingForFlowClosed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var dismissCallbackCount = 0
        flowUiManager.isShown = true
        flowUiManager.addPendingDismissCallback("missing") {
            dismissCallbackCount++
        }
        val intent = Intent(context, AdaptyUiActivity::class.java)
            .putExtra(AdaptyUiActivity.VIEW_ID, "missing")

        ActivityScenario.launch<AdaptyUiActivity>(intent).use {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }

        assertFalse(flowUiManager.isShown)
        assertEquals(1, dismissCallbackCount)
    }
}
