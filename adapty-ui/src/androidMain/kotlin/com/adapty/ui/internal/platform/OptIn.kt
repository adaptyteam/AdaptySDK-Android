package com.adapty.ui.internal.platform

/**
 * Marks internal AdaptyUI API shared with Adapty's own modules (crossplatform, adapty-ui-video).
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Adapty UI API, please don't rely on it."
)
public annotation class InternalAdaptyUiApi
