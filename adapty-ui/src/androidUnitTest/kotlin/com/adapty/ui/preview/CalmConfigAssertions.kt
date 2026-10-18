@file:OptIn(com.adapty.internal.utils.InternalAdaptyApi::class)

package com.adapty.ui.preview

import com.adapty.ui.AdaptyUI
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Moves to commonTest once the preview layer is common (Task 6d)
internal val calmScreenIds = setOf(
    "screen_welcome",
    "screen_reviews",
    "screen_goals",
    "screen_paywall",
    "screen_survey",
)

internal fun assertCalmConfig(config: AdaptyUI.FlowConfiguration) {
    assertEquals(calmScreenIds, config.screens.screens.keys)
    assertEquals(setOf("default"), config.navigators.keys)
    assertEquals("en", config.localizationId)
    assertTrue(config.texts.isNotEmpty(), "localized texts are mapped")
    val script = config.initialScript
    assertTrue("goToReviews" in script, "bundle script is embedded")
    assertTrue(
        "SDK.openScreen({ instanceId: \"screen_welcome\", type: \"screen_welcome\", navigatorId: \"default\"" in script,
        "initial screen is opened on the default navigator",
    )
}
