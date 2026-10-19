@file:OptIn(com.adapty.ui.internal.platform.InternalAdaptyUiApi::class)

package com.adapty.ui.preview

import com.adapty.ui.AdaptyUIWeb
import com.adapty.ui.WebEnvironment
import com.adapty.ui.internal.mapping.element.DeviceKind
import com.adapty.ui.internal.mapping.element.matchesStaticConditionFields
import com.adapty.ui.internal.platform.ADAPTY_SDK_VERSION
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.osVersion
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebPreviewEnvironmentTest {

    @AfterTest
    fun resetEnvironment() {
        AdaptyUIWeb.environment = WebEnvironment()
    }

    @Test
    fun sdkVersionComesFromCoreVersionName() {
        assertTrue(Regex("""\d+\.\d+\.\d+.*""").matches(ADAPTY_SDK_VERSION), ADAPTY_SDK_VERSION)
        assertEquals(ADAPTY_SDK_VERSION, previewDependencies(PlatformContextHandle()).metaInfo.adaptySdkVersion)
    }

    @Test
    fun osVersionComesFromEnvironment() {
        val metaInfo = previewDependencies(PlatformContextHandle()).metaInfo
        assertEquals("", metaInfo.os)
        assertEquals("", osVersion())

        AdaptyUIWeb.environment = WebEnvironment(osVersion = "16")

        assertEquals("16", metaInfo.os)
        assertEquals("16", osVersion())
    }

    @Test
    fun availableConditionUsesEnvironmentOsVersion() {
        fun available(minVersion: String) =
            mapOf("available" to listOf(mapOf("os_name" to "Android", "os_version" to minVersion)))
                .matchesStaticConditionFields(DeviceKind.TAB)

        AdaptyUIWeb.environment = WebEnvironment(osVersion = "16")

        assertTrue(available("15"))
        assertTrue(available("16"))
        assertFalse(available("17"))
    }
}
