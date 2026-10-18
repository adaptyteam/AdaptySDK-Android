package com.adapty.ui.internal.platform

private val reportedStubs = mutableSetOf<String>()

/** Marks a platform seam that is not implemented on web yet; logs a WARN once per seam. */
internal fun webStub(seam: String) {
    if (reportedStubs.add(seam)) {
        log(WARN) { "[AdaptyUI] web stub: $seam is not implemented on web" }
    }
}
