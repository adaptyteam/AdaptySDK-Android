@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.text.WebFonts

/** Web host configuration: the fonts and the screen environment Android gets from the system. */
public object AdaptyUIWeb {

    /**
     * Registers the fonts Android takes from /system/fonts.
     * [system] is the variable system font (Android `Roboto-Regular.ttf`), used for every font asset with
     * `wght`/`ital` variations. [symbolFallbacks] and [emoji] are glyph fallbacks, consulted in that order.
     * Call before the first flow is composed; unparsable fonts are logged and skipped.
     */
    public fun registerFonts(system: ByteArray, symbolFallbacks: List<ByteArray> = emptyList(), emoji: ByteArray? = null) {
        WebFonts.register(system, symbolFallbacks, emoji)
    }

    /** Device environment: `screenSmallestWidthDp`, `isLandscape`, `screenHeightDp`, safe-area insets and OS version. Observable by Compose. */
    public var environment: WebEnvironment by mutableStateOf(WebEnvironment())

    internal fun resetFontsForTest() {
        WebFonts.reset()
    }
}

/**
 * @param safeInsets safe-area insets in px (status/navigation bars on Android).
 * @param smallestScreenWidthDp Android `Configuration.smallestScreenWidthDp`.
 * @param isLandscape Android orientation.
 * @param screenHeightDp Android `Configuration.screenHeightDp` (the screen, not the browser viewport).
 * @param osVersion Android `Build.VERSION.RELEASE` (SDK environment `osVersion`, OS version conditions); empty = unknown.
 */
public data class WebEnvironment(
    val safeInsets: AdaptyFlowInsets = AdaptyFlowInsets.None,
    val smallestScreenWidthDp: Int = 800,
    val isLandscape: Boolean = true,
    val screenHeightDp: Int = 800,
    val osVersion: String = "",
)
